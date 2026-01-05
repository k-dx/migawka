package main

// TODO - TESTS:
// * should load existing thumbnails
// * should generate thumbnail if not present

import (
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"sync/atomic"

	"github.com/h2non/bimg"
	"github.com/rs/zerolog/log"
)

type Thumbnail struct {
	ID      Hash
	Content []byte
}

type IdWithPath struct {
	ID   Key
	Path string
}

type ThumbnailProvider interface {
	GetThumbnailByID(request IdWithPath) (Thumbnail, error)
	GetThumbnailsByIDs(requests []IdWithPath) ([]Thumbnail, error)

	// GenerateMissingThumbnails generates thumbnails for media items (requests)
	// that do not have thumbnails yet. It's only called before the server
	// starts serving requests, so does not have to care about concurrency.
	GenerateMissingThumbnails(requests []IdWithPath) error
	GetThumbnailDirectory() string
}

type lockEntry struct {
	mtx  sync.Mutex
	refs int32 // use atomics
}

type Locks struct {
	m sync.Map // map[string]*lockEntry
}

func (l *Locks) acquire(key string) *lockEntry {
	actual, _ := l.m.LoadOrStore(key, &lockEntry{refs: 1})
	entry := actual.(*lockEntry)

	// if Load returned existing, increment refs
	if entry != actual {
		atomic.AddInt32(&entry.refs, 1)
	}
	return entry
}

// release decreases ref count and deletes the map entry when zero.
func (l *Locks) release(key string, entry *lockEntry) {
	if atomic.AddInt32(&entry.refs, -1) == 0 {
		// attempt to remove only if the stored value is this entry
		l.m.CompareAndDelete(key, entry)
	}
}

type thumbnailProviderImpl struct {
	Hasher Hasher

	thumbnails         map[Key]Thumbnail
	thumbnailsLock     sync.RWMutex
	thumbnailsFileLock Locks

	// directory to store thumbnails in, absolute path
	thumbnaildir string
}

func NewThumbnailProvider(
	thumbnaildir string,
	hasher Hasher,
) ThumbnailProvider {
	tp := &thumbnailProviderImpl{
		Hasher:       hasher,
		thumbnails:   make(map[Key]Thumbnail),
		thumbnaildir: thumbnaildir,
	}

	tp.loadExistingThumbnails()

	return tp
}

func (tp *thumbnailProviderImpl) loadExistingThumbnails() error {
	// TODO: replace with WalkDir

	// walk through thumbnail directory and load thumbnails
	tp.thumbnailsLock.Lock()
	defer tp.thumbnailsLock.Unlock()
	err := filepath.Walk(tp.thumbnaildir, func(filePath string, info os.FileInfo, err error) error {
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

		hash, err := tp.Hasher.HashFromString(filenameWithoutExt)
		if err != nil {
			log.Error().Str("file", filePath).Err(err).Msg("ignoring bad thumbnail filename")
			// } else if _, mediaItemPresent := ms.items[tp.Hasher.HashToKey(hash)]; !mediaItemPresent {
			// 	log.Warn().Str("file", filePath).Msg("thumbnail does not match any media item. ignoring")
		} else {
			tp.thumbnails[tp.Hasher.HashToKey(hash)] = Thumbnail{
				ID:      hash,
				Content: content,
			}
		}

		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to load thumbnails: %w", err)
	}
	log.Debug().Int("count", len(tp.thumbnails)).
		Msg("Loaded thumbnails from existing files")

	return nil
}

func (tp *thumbnailProviderImpl) generateAndSaveThumbnail(request IdWithPath) (Thumbnail, error) {
	// generate thumbnail
	content, err := os.ReadFile(request.Path)
	if err != nil {
		log.Error().
			Str("file", request.Path).
			Err(err).
			Msg("failed to read media item for thumbnail generation")
		return Thumbnail{}, err
	}

	thumbnailContent, err := generateThumbnail(content)
	if err != nil {
		log.Error().
			Str("ID", request.ID.String()).
			Str("mediaItem", request.Path).
			Err(err).
			Msg("failed to generate thumbnail")
		return Thumbnail{}, err
	}

	thumbnail := Thumbnail{
		ID:      tp.Hasher.HashFromKey(request.ID),
		Content: thumbnailContent,
	}

	// store thumbnail in map
	tp.thumbnailsLock.Lock()
	tp.thumbnails[request.ID] = thumbnail
	tp.thumbnailsLock.Unlock()

	// save thumbnail to disk
	thumbnailFilePath := filepath.Join(tp.thumbnaildir, request.ID.String()+".jpg")
	err = os.WriteFile(thumbnailFilePath, thumbnailContent, os.ModePerm)
	if err != nil {
		log.Error().
			Str("file", thumbnailFilePath).
			Err(err).
			Msg("failed to write thumbnail")
	}

	log.Debug().Str("ID", request.ID.String()).
		Str("thumbnail", thumbnailFilePath).
		Str("mediaItem", request.Path).
		Msg("Generated and saved new thumbnail")

	return thumbnail, nil
}

func (tp *thumbnailProviderImpl) GenerateMissingThumbnails(requests []IdWithPath) error {

	// calculate which thumbnails are missing for progress logging
	missingCount := 0
	tp.thumbnailsLock.RLock()
	for _, request := range requests {
		if _, ok := tp.thumbnails[request.ID]; !ok {
			missingCount++
		}
	}
	tp.thumbnailsLock.RUnlock()

	logProgress := func(done, total int) {
		msg := fmt.Sprintf("Generating missing thumbnails, progress: %d / %d",
			done, total)
		log.Info().Msg(msg)
	}

	// generate thumbnails for media items without thumbnails
	generatedCount := 0
	logProgress(0, missingCount)
	for _, request := range requests {
		tp.thumbnailsLock.RLock()
		_, ok := tp.thumbnails[request.ID]
		tp.thumbnailsLock.RUnlock()
		if !ok {
			_, err := tp.generateAndSaveThumbnail(request)
			if err == nil {
				generatedCount++
				if generatedCount%100 == 0 {
					logProgress(generatedCount, missingCount)
				}
			}
		}
	}

	log.Info().Int("number of generated thumbnails", generatedCount).
		Int("missing thumbnails before generation", missingCount).
		Msg("Finished generating missing thumbnails")

	log.Debug().Int("count", len(tp.thumbnails)).
		Msg("Total thumbnails in memory after generation")

	return nil
}

func (tp *thumbnailProviderImpl) GetThumbnailByID(request IdWithPath) (Thumbnail, error) {
	fileLock := tp.thumbnailsFileLock.acquire(request.ID.String())
	fileLock.mtx.Lock()
	defer func() {
		fileLock.mtx.Unlock()
		tp.thumbnailsFileLock.release(request.ID.String(), fileLock)
	}()

	tp.thumbnailsLock.RLock()
	thumbnail, ok := tp.thumbnails[request.ID]
	tp.thumbnailsLock.RUnlock()
	if ok {
		return thumbnail, nil
	}

	// if not in map, fallback to file
	absThumbnailPath := filepath.Join(tp.thumbnaildir, request.ID.String()+".jpg")
	_, err := os.Stat(absThumbnailPath)

	// case: error stating file
	if err != nil && !errors.Is(err, os.ErrNotExist) {

		log.Debug().Err(err).Str("path", absThumbnailPath).
			Msg("Error stating thumbnail file")
		return Thumbnail{}, err
	}

	// case: file does not exist
	if err != nil && errors.Is(err, os.ErrNotExist) {
		thumbnail, err := tp.generateAndSaveThumbnail(request)
		if err != nil {
			return Thumbnail{}, err
		}
		return thumbnail, nil
	}

	// case: file exists, load it
	thumbnailContent, err := os.ReadFile(absThumbnailPath)
	if err != nil {
		log.Error().
			Str("file", absThumbnailPath).
			Err(err).
			Msg("failed to read thumbnail file")
		return Thumbnail{}, err
	}

	// store thumbnail in map
	thumbnail = Thumbnail{
		ID:      tp.Hasher.HashFromKey(request.ID),
		Content: thumbnailContent,
	}

	tp.thumbnailsLock.Lock()
	tp.thumbnails[request.ID] = thumbnail
	tp.thumbnailsLock.Unlock()

	return thumbnail, nil
}

func (tp *thumbnailProviderImpl) GetThumbnailsByIDs(requests []IdWithPath) ([]Thumbnail, error) {
	results := make([]Thumbnail, 0, len(requests))
	for _, request := range requests {
		thumbnail, err := tp.GetThumbnailByID(request)
		if err != nil {
			log.Error().
				Str("ID", request.ID.String()).
				Err(err).
				Msg("failed to get thumbnail by ID")
		}
		results = append(results, thumbnail)
	}

	return results, nil
}

func (tp *thumbnailProviderImpl) GetThumbnailDirectory() string {
	return tp.thumbnaildir
}

func generateThumbnail(data []byte) ([]byte, error) {
	return ResizeToThumbnail(data)
}

// ResizeToThumbnail takes raw image bytes and returns JPEG-encoded bytes of an
// image resized to longer side to 256 pixels, preserving aspect ratio. It
// respects image orientation.
func ResizeToThumbnail(in []byte) ([]byte, error) {
	img := bimg.NewImage(in)

	size, err := img.Size()
	if err != nil {
		return nil, fmt.Errorf("failed to get image size: %w", err)
	}

	TARGET_LONGER_SIDE := 256

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
		Width:     target_w,
		Height:    target_h,
		Quality:   50,
		Interlace: true,
	}

	newImage, err := img.Process(options)
	if err != nil {
		return nil, fmt.Errorf("failed to process image: %w", err)
	}
	return newImage, nil
}
