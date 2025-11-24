package main

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"image"
	"image/jpeg"
	"os"
	"os/exec"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/disintegration/imaging"
	"github.com/rs/zerolog/log"
)

type Thumbnail struct {
	ID      sha256Hash
	Content []byte
}

type MediaItemMetadata struct {
	ID           sha256Hash
	Path         string
	CreationTime time.Time
}

type MediaItem struct {
	Metadata MediaItemMetadata
	Content  []byte
}

type MediaStore interface {
	GetThumbnailsBeforeTimestamp(date time.Time, count uint) ([]Thumbnail, error)
	GetOptimizedMediaItem(id sha256Hash) (MediaItem, error)
	GetFullMediaItem(id sha256Hash) (MediaItem, error)
	GetCreationTimeOfMediaItem(id sha256Hash) (time.Time, error)

	GetMediaItemsCountForTest() int
}

type mediaStoreImpl struct {
	items      map[sha256Hash]MediaItemMetadata
	thumbnails map[sha256Hash]Thumbnail
}

func (ms *mediaStoreImpl) GetCreationTimeOfMediaItem(id sha256Hash) (time.Time, error) {
	item, ok := ms.items[id]
	if !ok {
		return time.Time{}, fmt.Errorf("media item with given id not found")
	}
	return item.CreationTime, nil
}

// Returns at most 'count' thumbnails created before (or at) the given timestamp
func (ms *mediaStoreImpl) GetThumbnailsBeforeTimestamp(date time.Time, count uint) ([]Thumbnail, error) {
	type idDatePair struct {
		id   sha256Hash
		date time.Time
	}

	// put date, sha256Hash into a list for dates >= given date
	idsByDate := make([]idDatePair, 0, len(ms.items))
	for id, item := range ms.items {
		// filter out items after the given date
		if item.CreationTime.After(date) {
			continue
		}
		idsByDate = append(idsByDate, idDatePair{
			id:   id,
			date: item.CreationTime,
		})
	}

	// sort by date descending
	sort.Slice(idsByDate, func(i, j int) bool {
		return idsByDate[i].date.After(idsByDate[j].date)
	})

	// return the first 'count' items
	ids := make([]sha256Hash, 0, count)
	for i := 0; i < int(count) && i < len(idsByDate); i++ {
		ids = append(ids, idsByDate[i].id)
	}
	return ms.getThumbnailsByIDs(ids)
}

func (ms *mediaStoreImpl) getThumbnailsByIDs(ids []sha256Hash) ([]Thumbnail, error) {
	thumbnails := make([]Thumbnail, 0)

	for _, id := range ids {
		thumbnail, ok := ms.thumbnails[id]
		if !ok {
			log.Error().Str("ID", id.String()).Msg("Thumbnail with ID not found")
			continue
		}
		thumbnails = append(thumbnails, thumbnail)
	}
	return thumbnails, nil
}

func (ms *mediaStoreImpl) GetFullMediaItem(id sha256Hash) (MediaItem, error) {
	item, ok := ms.items[id]
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

	itemFull := MediaItem{
		Metadata: item,
		Content:  content,
	}
	return itemFull, nil
}

func NewMediaItemMetadata(id sha256Hash, path string, creationTime time.Time) MediaItemMetadata {
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

func (ms *mediaStoreImpl) GetOptimizedMediaItem(id sha256Hash) (MediaItem, error) {
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

func NewMediaStore(path string) (MediaStore, error) {
	log.Debug().Msg("Creating new MediaStore")
	ms := &mediaStoreImpl{
		items:      make(map[sha256Hash]MediaItemMetadata),
		thumbnails: make(map[sha256Hash]Thumbnail),
	}
	err := ms.loadMediaItems(path, filepath.Join(path, ".thumbnails"))
	if err != nil {
		return nil, err
	}
	return ms, nil
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
	// TODO: change both to follow symlinks

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
		// TODO: consider using a library for better file type detection
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

		// Read file content
		content, err := os.ReadFile(filePath)
		if err != nil {
			return fmt.Errorf("failed to read file %s: %w", filePath, err)
		}

		// Calculate SHA256 hash
		hash := sha256.Sum256(content)

		log.Debug().
			Str("file", filePath).
			Str("hash", hex.EncodeToString(hash[:])).
			Msg("Loaded media item")

		creatationDatetime, err := getExifDate(filePath)
		if err != nil {
			log.Info().
				Str("file", filePath).
				Err(err).
				Msg("Failed to get EXIF date, using file modification time instead")
			creatationDatetime = info.ModTime()
		}

		// Store in map
		ms.items[hash] = NewMediaItemMetadata(
			hash,
			filePath,
			creatationDatetime,
		)

		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to load media items: %w", err)
	}
	log.Debug().Int("count", len(ms.items)).Msg("Loaded media items")

	// walk through thumbnail directory and load thumbnails
	err = filepath.Walk(thumbnailPath, func(filePath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if info.IsDir() {
			return nil
		}
		content, err := os.ReadFile(filePath)
		if err != nil {
			return fmt.Errorf("failed to read file %s: %w", filePath, err)
		}

		filenameWithoutExt := strings.TrimSuffix(info.Name(), filepath.Ext(info.Name()))
		var hash sha256Hash
		err = hash.FromString(filenameWithoutExt)
		if err != nil {
			log.Error().Str("file", filePath).Err(err).Msg("ignoring bad thumbnail filename")
		} else if _, mediaItemPresent := ms.items[hash]; !mediaItemPresent {
			log.Warn().Str("file", filePath).Msg("thumbnail does not match any media item. ignoring")
		} else {
			ms.thumbnails[hash] = Thumbnail{
				ID:      hash,
				Content: content,
			}
		}

		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to load thumbnails: %w", err)
	}
	log.Debug().Int("count", len(ms.thumbnails)).Msg("Loaded thumbnails from existing files")

	// generate thumbnails for media items without thumbnails
	for id, item := range ms.items {
		if _, ok := ms.thumbnails[id]; !ok {
			// generate thumbnail
			content, err := os.ReadFile(item.Path)
			if err != nil {
				log.Error().
					Str("file", item.Path).
					Err(err).
					Msg("failed to read media item for thumbnail generation")
				continue
			}

			thumbnailContent, err := generateThumbnail(content)
			if err != nil {
				log.Error().
					Str("ID", id.String()).
					Err(err).
					Msg("failed to generate thumbnail")
				continue
			}
			// store thumbnail in map
			ms.thumbnails[id] = Thumbnail{
				ID:      id,
				Content: thumbnailContent,
			}
			// save thumbnail to disk
			thumbnailFilePath := filepath.Join(thumbnailPath, id.String()+".jpg")
			err = os.WriteFile(thumbnailFilePath, thumbnailContent, os.ModePerm)
			if err != nil {
				log.Error().
					Str("file", thumbnailFilePath).
					Err(err).
					Msg("failed to write thumbnail")
			}
			log.Debug().Str("ID", id.String()).
				Str("file", thumbnailFilePath).
				Msg("Generated and saved new thumbnail")
		}
	}

	return nil
}

// TODO: add tests for this
func getExifDate(path string) (time.Time, error) {
	cmd := exec.Command("exiftool", "-DateTimeOriginal", "-s3", path)

	output, err := cmd.Output()
	if err != nil {
		return time.Time{}, fmt.Errorf("failed to get EXIF date: %w", err)
	}

	dateStr := strings.TrimSpace(string(output))
	if dateStr == "" {
		return time.Time{}, fmt.Errorf("no EXIF date found")
	}

	const exifDateFormat = "2006:01:02 15:04:05"
	t, err := time.Parse(exifDateFormat, dateStr)
	if err != nil {
		return time.Time{}, fmt.Errorf("failed to parse EXIF date: %w", err)
	}

	return t, nil
}

func generateThumbnail(data []byte) ([]byte, error) {
	return ResizeToThumbnail(data)
}

func optimizeJpg(in []byte) ([]byte, error) {
	img, _, err := image.Decode(bytes.NewReader(in))
	if err != nil {
		return nil, err
	}

	// TODO: copy EXIF metadata from original image

	var buf bytes.Buffer
	opts := &jpeg.Options{Quality: 50}
	if err := jpeg.Encode(&buf, img, opts); err != nil {
		return nil, err
	}

	return buf.Bytes(), nil
}

// ResizeToThumbnail takes raw image bytes and returns JPEG-encoded bytes of an
// image resized to longer side to 256 pixels, preserving aspect ratio. It
// respects image orientation.
func ResizeToThumbnail(in []byte) ([]byte, error) {
	img, err := imaging.Decode(bytes.NewReader(in), imaging.AutoOrientation(true))
	if err != nil {
		return nil, err
	}

	// Determine square crop rectangle centered
	b := img.Bounds()
	w := b.Dx()
	h := b.Dy()

	TARGET_LONGER_SIDE := 256

	target_w := 0
	target_h := 0

	if w >= h {
		if w > TARGET_LONGER_SIDE {
			target_w = TARGET_LONGER_SIDE
			target_h = 0 // will preserve aspect ratio
		}
	} else {
		if h > TARGET_LONGER_SIDE {
			target_w = 0 // will preserve aspect ratio
			target_h = TARGET_LONGER_SIDE
		}
	}

	resized := imaging.Resize(img, target_w, target_h, imaging.Lanczos)

	// encode to JPEG
	var outBuf bytes.Buffer
	opts := &jpeg.Options{Quality: 50}
	err = imaging.Encode(&outBuf, resized, imaging.JPEG, imaging.JPEGQuality(opts.Quality))
	if err != nil {
		return nil, err
	}

	return outBuf.Bytes(), nil
}

// In mediastore_test.go or mediastore.go
func (ms *mediaStoreImpl) GetMediaItemsCountForTest() int {
	return len(ms.items)
}
