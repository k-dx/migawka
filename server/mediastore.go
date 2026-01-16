package main

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/h2non/bimg"
	"github.com/rs/zerolog/log"
)

type TimelineEntry struct {
	ID        Hash
	Timestamp time.Time
}

type MediaItemMetadata struct {
	ID           Hash
	Path         string
	CreationTime time.Time
}

type ExifTag int

const (
	DateTime ExifTag = iota
	Make
	Model
	Orientation
	FocalLength
	ExposureTime
	FNumber
	ISO
	Flash
	WhiteBalance
	exifTagCount
	// MAKE SURE TO UPDATE GetStatusMap() WHEN ADDING NEW TAGS
)

type MediaItemFullMetadata struct {
	Metadata   MediaItemMetadata
	ExifValues map[ExifTag]string
}

type MediaItem struct {
	Metadata MediaItemMetadata
	Content  []byte
}

type MediaStore interface {
	GetHasher() Hasher

	// Returns at most* 'count' thumbnails created before the given timestamp,
	// sorted by creation time descending (newest first).
	// *will return more if there are multiple items with the same timestamp as
	// the last one
	GetThumbnailsBeforeTimestamp(date time.Time, count uint) ([]Thumbnail, error)
	GetOptimizedMediaItem(id Hash) (MediaItem, error)
	GetFullMediaItem(id Hash) (MediaItem, error)
	GetCreationTimeOfMediaItem(id Hash) (time.Time, error)
	GetThumbnailByID(id Hash) (Thumbnail, error)
	GetTimelineEntries() ([]TimelineEntry, error)

	// Path does not start with a slash, is relative to mediadir. The ordering
	// of the results is arbitrary. Returns thumbnails of media items in the
	// given path (without subdirectories) and corresponding filenames. Ignores
	// thumbnaildir.
	GetTimelineEntriesByPath(path string) ([]TimelineEntry, []string, error)
	GenerateMissingThumbnails()
	GetFullMetadata(id Hash) (MediaItemFullMetadata, error)

	GetMediaDirectory() string
	GetThumbnailDirectory() string

	Close() error

	GetMediaItemsCountForTest() int
}

type Key string

func (k *Key) String() string {
	return string(*k)
}

type Hasher interface {
	HashFromString(s string) (Hash, error)
	CalculateHash(data []byte) Hash
	HashToKey(h Hash) Key
	HashFromKey(k Key) Hash
}

type mediaStoreImpl struct {
	Hasher            Hasher
	thumbnailProvider ThumbnailProvider

	// only read after initialization, so no lock needed
	items    map[Key]MediaItemMetadata
	mediadir string
	db       DBRepository
}

func NewMediaStore(path string, dbRepo DBRepository, hasher Hasher) (MediaStore, error) {
	log.Debug().Msg("Creating new MediaStore")
	thumbnaildir := filepath.Join(path, ".thumbnails")
	ms := &mediaStoreImpl{
		Hasher:            hasher,
		thumbnailProvider: NewThumbnailProvider(thumbnaildir, hasher),
		items:             make(map[Key]MediaItemMetadata),
		mediadir:          path,
		db:                dbRepo,
	}
	err := ms.loadMediaItems(path, thumbnaildir)
	if err != nil {
		return nil, err
	}
	return ms, nil
}

func (ms *mediaStoreImpl) Close() error {
	err := ms.db.Close()
	if err != nil {
		return fmt.Errorf("failed to close media store: %w", err)
	}
	return nil
}

func (ms *mediaStoreImpl) GetFullMetadata(id Hash) (MediaItemFullMetadata, error) {

	item, ok := ms.items[ms.Hasher.HashToKey(id)]
	if !ok {
		return MediaItemFullMetadata{}, fmt.Errorf("media item with given id not found")
	}

	path := item.Path
	log.Debug().Str("ID", id.String()).
		Str("path", path).
		Msg("Returning full media item metadata")

	content, err := os.ReadFile(path)
	if err != nil {
		return MediaItemFullMetadata{}, fmt.Errorf("failed to read media item from disk: %w", err)
	}

	fullPath := item.Path
	pathRootedAtMediadir, err := filepath.Rel(ms.mediadir, fullPath)
	if err != nil || strings.HasPrefix(pathRootedAtMediadir, "/") {
		return MediaItemFullMetadata{}, fmt.Errorf("failed to get path relative to mediadir: %w", err)
	}

	metadata := NewMediaItemMetadata(
		item.ID,
		pathRootedAtMediadir,
		item.CreationTime,
	)

	exifValues, err := getExifMetadata(content)
	if err != nil {
		log.Debug().Str("ID", id.String()).
			Err(err).
			Msg("Failed to get EXIF metadata")
	}

	// for tag, value := range exifValues {
	// 	metadataValue := value
	// 	metadataValue = strings.TrimSpace(metadataValue)
	// 	if metadataValue != "" {
	// 		metadataValue = strings.ReplaceAll(metadataValue, "\x00", "")
	// 		if metadataValue != "" {
	// 			exifValues[tag] = metadataValue
	// 		}
	// 	}
	// }

	itemFull := MediaItemFullMetadata{
		Metadata:   metadata,
		ExifValues: exifValues,
	}
	return itemFull, nil
}

func (ms *mediaStoreImpl) GetMediaDirectory() string {
	return ms.mediadir
}

func (ms *mediaStoreImpl) GetThumbnailDirectory() string {
	return ms.thumbnailProvider.GetThumbnailDirectory()
}

func (ms *mediaStoreImpl) GetHasher() Hasher {
	return ms.Hasher
}

func (ms *mediaStoreImpl) GetCreationTimeOfMediaItem(id Hash) (time.Time, error) {
	item, ok := ms.items[ms.Hasher.HashToKey(id)]
	if !ok {
		return time.Time{}, fmt.Errorf("media item with given id not found")
	}
	return item.CreationTime, nil
}

func (ms *mediaStoreImpl) GetThumbnailsBeforeTimestamp(date time.Time, count uint) ([]Thumbnail, error) {
	// for safety, cap count to number of items. If we have more items in memory
	// than uint can represent, something is very wrong anyway.
	if count > uint(len(ms.items)) {
		count = uint(len(ms.items))
	}

	type idDatePair struct {
		id   Key
		path string
		date time.Time
	}

	// put date, Hash into a list for dates >= given date
	idsByDate := make([]idDatePair, 0, len(ms.items))
	for id, item := range ms.items {
		// filter out items after the given date
		if item.CreationTime.Before(date) {
			idsByDate = append(idsByDate, idDatePair{
				id:   id,
				path: item.Path,
				date: item.CreationTime,
			})
		}
	}

	// sort by date descending
	sort.Slice(idsByDate, func(i, j int) bool {
		return idsByDate[i].date.After(idsByDate[j].date)
	})
	// take first 'count' ids, plus any with the same date as the last one
	ids := make([]IdWithPath, 0)
	i := 0
	for ; i < int(count) && i < len(idsByDate); i++ {
		ids = append(ids, IdWithPath{ID: idsByDate[i].id, Path: idsByDate[i].path})
	}
	for ; i > 0 && i < len(idsByDate) && idsByDate[i].date.Equal(idsByDate[i-1].date); i++ {
		ids = append(ids, IdWithPath{ID: idsByDate[i].id, Path: idsByDate[i].path})
	}

	return ms.thumbnailProvider.GetThumbnailsByIDs(ids)
}

func (ms *mediaStoreImpl) GetFullMediaItem(id Hash) (MediaItem, error) {
	item, ok := ms.items[ms.Hasher.HashToKey(id)]
	if !ok {
		return MediaItem{}, fmt.Errorf("media item with given id not found")
	}

	path := item.Path
	log.Debug().Str("ID", id.String()).
		Str("path", path).
		Msg("Returning full media item")

	content, err := os.ReadFile(path)
	if err != nil {
		return MediaItem{}, fmt.Errorf("failed to read media item from disk: %w", err)
	}

	fullPath := item.Path
	pathRootedAtMediadir, err := filepath.Rel(ms.mediadir, fullPath)
	if err != nil || strings.HasPrefix(pathRootedAtMediadir, "/") {
		return MediaItem{}, fmt.Errorf("failed to get path relative to mediadir: %w", err)
	}

	metadata := NewMediaItemMetadata(
		item.ID,
		pathRootedAtMediadir,
		item.CreationTime,
	)

	itemFull := MediaItem{
		Metadata: metadata,
		Content:  content,
	}
	return itemFull, nil
}

func NewMediaItemMetadata(id Hash, path string, creationTime time.Time) MediaItemMetadata {
	return MediaItemMetadata{
		ID:           id,
		Path:         path,
		CreationTime: creationTime,
	}
}

func NewMediaItem(metadata MediaItemMetadata, content []byte) MediaItem {
	return MediaItem{
		Metadata: metadata,
		Content:  content,
	}
}

func (ms *mediaStoreImpl) GetOptimizedMediaItem(id Hash) (MediaItem, error) {
	// TODO: generate optimized version on first request and cache it
	// or generate all optimized versions on startup
	mediaItem, err := ms.GetFullMediaItem(id)
	if err != nil {
		return MediaItem{}, err
	}

	optimized, err := optimizeJpg(mediaItem.Content)
	if err != nil {
		return MediaItem{}, fmt.Errorf("failed to optimize media item: %w", err)
	}

	log.Debug().Str("ID", id.String()).
		Int("originalSize", len(mediaItem.Content)).
		Int("optimizedSize", len(optimized)).
		Msg("Returning optimized media item")

	metadata := NewMediaItemMetadata(
		mediaItem.Metadata.ID,
		mediaItem.Metadata.Path,
		mediaItem.Metadata.CreationTime,
	)

	return NewMediaItem(
		metadata,
		optimized,
	), nil
}

func supportedMimeType(fileContent []byte) bool {
	mimeType := bimg.DetermineImageType(fileContent)

	supportedMimeTypes := []bimg.ImageType{
		bimg.JPEG,
	}

	for _, mt := range supportedMimeTypes {
		if mimeType == mt {
			return true
		}
	}
	return false
}

func (ms *mediaStoreImpl) loadMediaItems(mediaPath string, thumbnailPath string) error {
	log.Debug().Str("mediaPath", mediaPath).
		Str("thumbnailPath", thumbnailPath).
		Msg("Loading media items")
	// create the thumbnail directory if it doesn't exist
	if _, err := os.Stat(thumbnailPath); err != nil {
		if errors.Is(err, os.ErrNotExist) {
			err := os.MkdirAll(thumbnailPath, os.ModePerm)
			if err != nil {
				return fmt.Errorf("failed to create thumbnail directory %s: %w", thumbnailPath, err)
			}
		} else {
			return fmt.Errorf("failed to stat thumbnail directory %s: %w", thumbnailPath, err)
		}
		log.Info().Str("thumbnailPath", thumbnailPath).Msg("Created thumbnail directory")
	}

	// TODO: change both to WalkDir for better performance with many files
	// TODO: symlinks support (consider security implications)

	// walk through the directory and load media items
	err := filepath.Walk(mediaPath, func(filePath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		if info.IsDir() {
			// Skip all files in the thumbnail directory
			absFilePath, err1 := filepath.Abs(filePath)
			absThumbnailPath, err2 := filepath.Abs(thumbnailPath)
			if err1 == nil && err2 == nil && absFilePath == absThumbnailPath {
				return filepath.SkipDir
			}

			// Skip directory itself
			return nil
		}

		// Check if file is a media file (basic check by extension)
		ext := strings.ToLower(filepath.Ext(filePath))

		// TODO: support more file types
		// TODO: save type in MediaItem
		// validExts := []string{".jpg", ".jpeg", ".png", ".gif", ".mp4", ".mov", ".avi", ".mkv"}
		validExts := []string{".jpg", ".jpeg"}
		isMedia := false
		for _, validExt := range validExts {
			if ext == validExt {
				isMedia = true
				break
			}
		}
		if !isMedia {
			return nil
		}

		// TODO: if we already have the file in the database and its
		// modification time is unchanged, skip it
		fileRecord, err := ms.db.GetFileByPath(filePath)
		if err != nil {
			return fmt.Errorf("failed to get file from database: %w", err)
		}

		var hash Hash
		var creationDatetime time.Time
		if fileRecord != nil && fileRecord.ModifiedAt.Equal(info.ModTime()) {
			// Read from database

			// The file has not changed since we saved info about it in the
			// database, so we can just load info from there
			log.Debug().
				Str("file", filePath).
				Str("hash", fileRecord.Hash).
				Msg("Loaded media item from database")

			hash, err = ms.Hasher.HashFromString(fileRecord.Hash)
			if err != nil {
				return fmt.Errorf("failed to parse hash from database for file %s: %w", filePath, err)
			}
			creationDatetime = fileRecord.MediaCreationTime
		} else {
			// Read file content
			content, err := os.ReadFile(filePath)
			if err != nil {
				return fmt.Errorf("failed to read file %s: %w", filePath, err)
			}

			if !supportedMimeType(content) {
				return nil
			}

			hash = ms.Hasher.CalculateHash(content)

			log.Debug().
				Str("file", filePath).
				Str("hash", hash.String()).
				Msg("Loaded media item")

			creationDatetime, err = getExifCreationDate(content)
			if err != nil {
				log.Debug().
					Str("file", filePath).
					Err(err).
					Msg("Failed to get EXIF date, using file modification time instead")
				creationDatetime = info.ModTime()
			}

			// Save to database
			err = ms.db.UpsertFileRecord(FileRecord{
				Filepath:          filePath,
				Hash:              hash.String(),
				ModifiedAt:        info.ModTime(),
				MediaCreationTime: creationDatetime,
			})
		}

		// Store in map
		ms.items[ms.Hasher.HashToKey(hash)] = NewMediaItemMetadata(
			hash,
			filePath,
			creationDatetime,
		)

		if len(ms.items)%500 == 0 {
			log.Info().Int("count", len(ms.items)).Msg("Loaded media items so far")
		}

		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to load media items: %w", err)
	}
	log.Debug().Int("count", len(ms.items)).Msg("Loaded media items")

	return nil
}

// TODO: add tests for this
func getExifCreationDate(img []byte) (time.Time, error) {
	metadata, err := bimg.Metadata(img)
	if err != nil {
		return time.Time{}, fmt.Errorf("failed to get EXIF metadata: %w", err)
	}

	dateStr := metadata.EXIF.DateTimeOriginal

	timeLayout := "2006:01:02 15:04:05"
	t, err := time.Parse(timeLayout, dateStr)
	if err != nil {
		return time.Time{}, fmt.Errorf("failed to parse EXIF date: %w", err)
	}

	return t, nil
}

func getExifMetadata(img []byte) (map[ExifTag]string, error) {
	metadata, err := bimg.Metadata(img)
	if err != nil {
		return map[ExifTag]string{}, fmt.Errorf("failed to get EXIF metadata: %w", err)
	}

	rawTags := make(map[ExifTag]string)

	rawTags[DateTime] = metadata.EXIF.DateTimeOriginal
	rawTags[Make] = metadata.EXIF.Make
	rawTags[Model] = metadata.EXIF.Model
	rawTags[Orientation] = fmt.Sprintf("%d", metadata.EXIF.Orientation)
	rawTags[FocalLength] = metadata.EXIF.FocalLength
	rawTags[ExposureTime] = metadata.EXIF.ExposureTime
	rawTags[FNumber] = metadata.EXIF.FNumber
	rawTags[ISO] = fmt.Sprintf("%d", metadata.EXIF.ISOSpeedRatings)
	rawTags[Flash] = fmt.Sprintf("%d", metadata.EXIF.Flash)
	rawTags[WhiteBalance] = fmt.Sprintf("%d", metadata.EXIF.WhiteBalance)

	cleanedTags := make(map[ExifTag]string)
	for tag, value := range rawTags {
		metadataValue := value
		metadataValue = strings.TrimSpace(metadataValue)
		metadataValue = strings.ReplaceAll(metadataValue, "\x00", "")
		if metadataValue != "" {
			cleanedTags[tag] = metadataValue
		}
	}

	return cleanedTags, nil
}

func optimizeJpg(in []byte) ([]byte, error) {
	img := bimg.NewImage(in)

	size, err := img.Size()
	if err != nil {
		return nil, fmt.Errorf("failed to get image size: %w", err)
	}

	TARGET_LONGER_SIDE := 1024

	target_w := 0
	target_h := 0

	if size.Width >= size.Height {
		if size.Width > TARGET_LONGER_SIDE {
			target_w = TARGET_LONGER_SIDE
			target_h = 0 // will preserve aspect ratio
		}
	} else {
		if size.Height > TARGET_LONGER_SIDE {
			target_w = 0 // will preserve aspect ratio
			target_h = TARGET_LONGER_SIDE
		}
	}

	options := bimg.Options{
		Width:         target_w,
		Height:        target_h,
		Quality:       80,
		Interlace:     true,
		StripMetadata: false, // keep EXIF metadata

		// bimg respects EXIF Orientation tag by default,
		// but we are not stripping metadata, so disable it
		NoAutoRotate: true,
	}

	newImage, err := img.Process(options)
	if err != nil {
		return nil, fmt.Errorf("failed to process image: %w", err)
	}

	return newImage, nil
}

func (ms *mediaStoreImpl) GetTimelineEntriesByPath(pathRelativeToMediadir string) ([]TimelineEntry, []string, error) {
	results := make([]TimelineEntry, 0)
	filenames := make([]string, 0)

	absPath := filepath.Join(ms.mediadir, pathRelativeToMediadir)
	for key, item := range ms.items {
		dirOfItem := filepath.Dir(item.Path)

		if dirOfItem == absPath {
			results = append(results, TimelineEntry{
				ID:        ms.Hasher.HashFromKey(key),
				Timestamp: item.CreationTime,
			})
			filename := filepath.Base(item.Path)
			filenames = append(filenames, filename)
		}
	}

	return results, filenames, nil
}

func (ms *mediaStoreImpl) GenerateMissingThumbnails() {
	idsWithPath := make([]IdWithPath, 0, len(ms.items))
	for id, item := range ms.items {
		idsWithPath = append(idsWithPath, IdWithPath{ID: id, Path: item.Path})
	}
	log.Info().Msg("Generating missing thumbnails")
	err := ms.thumbnailProvider.GenerateMissingThumbnails(idsWithPath)
	if err != nil {
		log.Error().Err(err).Msg("Error generating missing thumbnails")
	}
}

func (ms *mediaStoreImpl) GetThumbnailByID(id Hash) (Thumbnail, error) {
	item, ok := ms.items[ms.Hasher.HashToKey(id)]
	if !ok {
		return Thumbnail{}, fmt.Errorf("media item with given id not found")
	}

	request := IdWithPath{ID: ms.Hasher.HashToKey(id), Path: item.Path}
	return ms.thumbnailProvider.GetThumbnailByID(request)
}

func (ms *mediaStoreImpl) GetTimelineEntries() ([]TimelineEntry, error) {
	entries := make([]TimelineEntry, 0, len(ms.items))

	for id, item := range ms.items {
		entries = append(entries, TimelineEntry{
			ID:        ms.Hasher.HashFromKey(id),
			Timestamp: item.CreationTime,
		})
	}

	return entries, nil
}

// In mediastore_test.go or mediastore.go
func (ms *mediaStoreImpl) GetMediaItemsCountForTest() int {
	return len(ms.items)
}
