package main

import (
	"bytes"
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"image"
	"image/draw"
	"image/jpeg"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/nfnt/resize"
	"github.com/rs/zerolog/log"
)

const sha256HashSize = 32

type sha256Hash [sha256HashSize]byte

type Thumbnail struct {
	ID      sha256Hash
	Content []byte
}

type MediaItem struct {
	ID           sha256Hash
	Path         string
	CreationTime time.Time
	Content      []byte
}

type MediaStore interface {
	GetThumbnailsFromDate(date time.Time, count uint) ([]Thumbnail, error)
	GetMediaItem(id sha256Hash) (MediaItem, error)

	GetMediaItemsCountForTest() int
}

type mediaStoreImpl struct {
	items      map[sha256Hash]MediaItem
	thumbnails map[sha256Hash]Thumbnail
}

// Returns at most 'count' thumbnails created after (or at) the given date
func (ms *mediaStoreImpl) GetThumbnailsFromDate(date time.Time, count uint) ([]Thumbnail, error) {
	type idDatePair struct {
		id   sha256Hash
		date time.Time
	}

	// put date, sha256Hash into a list for dates >= given date
	idsByDate := make([]idDatePair, 0, len(ms.items))
	for id, item := range ms.items {
		if item.CreationTime.Before(date) {
			continue
		}
		idsByDate = append(idsByDate, idDatePair{
			id:   id,
			date: item.CreationTime,
		})
	}

	// sort by date
	sort.Slice(idsByDate, func(i, j int) bool {
		return idsByDate[i].date.Before(idsByDate[j].date)
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

func (ms *mediaStoreImpl) GetMediaItem(id sha256Hash) (MediaItem, error) {
	item, ok := ms.items[id]
	if !ok {
		return MediaItem{}, fmt.Errorf("media item with given id not found")
	}
	return item, nil
}

func NewMediaStore(path string) (MediaStore, error) {
	log.Debug().Msg("Creating new MediaStore")
	ms := &mediaStoreImpl{
		items:      make(map[sha256Hash]MediaItem),
		thumbnails: make(map[sha256Hash]Thumbnail),
	}
	err := ms.loadMediaItems(path, filepath.Join(path, ".thumbnails"))
	if err != nil {
		return nil, err
	}
	return ms, nil
}

func (h *sha256Hash) FromString(s string) error {
	data, err := hex.DecodeString(s)
	if err != nil {
		return err
	}
	if len(data) != sha256HashSize {
		return fmt.Errorf("invalid hash length: got %d, want %d", len(data), sha256HashSize)
	}
	copy(h[:], data)
	return nil
}

func (h *sha256Hash) String() string {
	return hex.EncodeToString(h[:])
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

	// walk through thumbnail directory and load thumbnails
	err := filepath.Walk(thumbnailPath, func(filePath string, info os.FileInfo, err error) error {
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

	// walk through the directory and load media items
	err = filepath.Walk(mediaPath, func(filePath string, info os.FileInfo, err error) error {
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

		// Store in map
		ms.items[hash] = MediaItem{
			ID:           hash,
			Path:         filePath,
			CreationTime: info.ModTime(), // TODO: use EXIF for images if available
			Content:      content,
		}

		return nil
	})
	if err != nil {
		return fmt.Errorf("failed to load media items: %w", err)
	}
	log.Debug().Int("count", len(ms.items)).Msg("Loaded media items")

	// generate thumbnails for media items without thumbnails
	for id, item := range ms.items {
		if _, ok := ms.thumbnails[id]; !ok {
			// generate thumbnail
			thumbnailContent, err := generateThumbnail(item.Content)
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

func generateThumbnail(data []byte) ([]byte, error) {
	return ResizeTo256(data)
}

// ResizeTo256 takes raw image bytes and returns JPEG-encoded bytes of a 256x256
// image. It center-crops non-square images, then resizes with good quality.
// Supported input formats: JPEG, PNG, GIF.
//
// Returns JPEG bytes and any error.
func ResizeTo256(in []byte) ([]byte, error) {
	img, format, err := image.Decode(bytes.NewReader(in))
	if err != nil {
		return nil, err
	}

	// Determine square crop rectangle centered
	b := img.Bounds()
	w := b.Dx()
	h := b.Dy()

	var src image.Image
	if w == h {
		src = img
	} else if w > h {
		// landscape: crop sides
		x0 := (w - h) / 2
		rect := image.Rect(x0, 0, x0+h, h)
		c := image.NewRGBA(image.Rect(0, 0, rect.Dx(), rect.Dy()))
		draw.Draw(c, c.Bounds(), img, rect.Min, draw.Src)
		src = c
	} else {
		// portrait: crop top/bottom
		y0 := (h - w) / 2
		rect := image.Rect(0, y0, w, y0+w)
		c := image.NewRGBA(image.Rect(0, 0, rect.Dx(), rect.Dy()))
		draw.Draw(c, c.Bounds(), img, rect.Min, draw.Src)
		src = c
	}

	// Resize to 256x256 using github.com/nfnt/resize (Lanczos3)
	resized := resize.Resize(256, 256, src, resize.Lanczos3)

	// Encode to JPEG
	var buf bytes.Buffer
	opts := &jpeg.Options{Quality: 90}
	if err := jpeg.Encode(&buf, resized, opts); err != nil {
		return nil, err
	}

	// If caller wants original format instead of JPEG, you can branch on `format`.
	_ = format // keep for future use
	return buf.Bytes(), nil
}

// In mediastore_test.go or mediastore.go
func (ms *mediaStoreImpl) GetMediaItemsCountForTest() int {
	return len(ms.items)
}
