package main

import (
	"crypto/sha256"
	"encoding/hex"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"
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
	GetThumbnailsFromDate(date time.Time, count int) ([]Thumbnail, error)
	GetMediaItem(id sha256Hash) (MediaItem, error)
}

type mediaStoreImpl struct {
	items      map[sha256Hash]MediaItem
	thumbnails map[sha256Hash]Thumbnail
}

// Returns at most 'count' thumbnails created after (or at) the given date
func (ms *mediaStoreImpl) GetThumbnailsFromDate(date time.Time, count int) ([]Thumbnail, error) {
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
	for i := 0; i < count && i < len(idsByDate); i++ {
		ids = append(ids, idsByDate[i].id)
	}
	return ms.getThumbnailsByIDs(ids)
}

func (ms *mediaStoreImpl) getThumbnailsByIDs(ids []sha256Hash) ([]Thumbnail, error) {
	thumbnails := make([]Thumbnail, 0)

	for _, id := range ids {
		thumbnail, ok := ms.thumbnails[id]
		if !ok {
			// TODO: make proper logging
			fmt.Fprintf(os.Stderr, "Thumbnail with ID %x not found\n", id)
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
	ms := &mediaStoreImpl{
		items: make(map[sha256Hash]MediaItem),
	}
	err := ms.loadMediaItems(path, filepath.Join(path, ".thumbnails"))
	if err != nil {
		return nil, err
	}
	return ms, nil
}

func (h sha256Hash) FromString(s string) error {
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

func (ms *mediaStoreImpl) loadMediaItems(mediaPath string, thumbnailPath string) error {
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
	}

	// TODO: change both to WalkDir for better performance with many files
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

		filename := info.Name()
		var hash sha256Hash
		err = hash.FromString(filename)
		if err != nil {
			fmt.Fprintf(os.Stderr, "ignoring bad thumbnail filename %s, %v\n", filePath, err) // TODO: logging
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

	// walk through the directory and load media items
	return filepath.Walk(mediaPath, func(filePath string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Skip directories
		if info.IsDir() {
			return nil
		}

		// Check if file is a media file (basic check by extension)
		ext := strings.ToLower(filepath.Ext(filePath))
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

		// Store in map
		ms.items[hash] = MediaItem{
			ID:           hash,
			Path:         filePath,
			CreationTime: info.ModTime(), // TODO: use EXIF for images if available
			Content:      content,
		}

		return nil
	})
}
