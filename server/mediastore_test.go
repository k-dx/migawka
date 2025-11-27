package main

import (
	"image"
	_ "image/jpeg"
	"os"
	"os/exec"
	"testing"
	"time"
)

func copyDir(t *testing.T, src string, dst string) {
	cmd := exec.Command("cp", "-r", src, dst)
	out, err := cmd.CombinedOutput()
	if err != nil {
		// handle error; out contains combined stdout/stderr
		t.Fatalf("Failed to copy directory from %s to %s: %v, output: %s", src, dst, err, string(out))
	}
}

func TestMediaStore_loadMediaItems_shouldCreateThumbnailDirectory(t *testing.T) {
	copyDir(t, "./tests/test1", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	_, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}
	thumbnailsPath := "./test/.thumbnails"
	if _, err := os.Stat(thumbnailsPath); os.IsNotExist(err) {
		t.Fatalf("Thumbnail directory was not created at %s", thumbnailsPath)
	}
}

func TestMediaStore_loadMediaItems_shouldLoadMediaItems(t *testing.T) {
	copyDir(t, "./tests/test2", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	mediaStore, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}

	expectedMediaItemsCount := 2
	got := mediaStore.GetMediaItemsCountForTest()
	if got != expectedMediaItemsCount {
		t.Fatalf("Expected %d media items, got %d", expectedMediaItemsCount, got)
	}
}

func TestMediaStore_loadMediaItems_shouldGenerateThumbnails(t *testing.T) {
	copyDir(t, "./tests/test2", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	mediaStore, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}

	expectedThumbnailsCount := 2
	timestamp := time.Date(2025, time.November, 23, 17, 2, 52, 0, time.UTC)
	thumbnails, err := mediaStore.GetThumbnailsBeforeTimestamp(timestamp, 10)
	if err != nil {
		t.Fatalf("Failed to get thumbnails: %v", err)
	}
	got := len(thumbnails)
	if got != expectedThumbnailsCount {
		t.Fatalf("Expected %d thumbnails, got %d", expectedThumbnailsCount, got)
	}

	// check if thumbnail files exist
	thumbnailsPath := "./test/.thumbnails"
	for _, thumbnail := range thumbnails {
		thumbnailFilePath := thumbnailsPath + "/" + thumbnail.ID.String() + ".jpg"
		if _, err := os.Stat(thumbnailFilePath); os.IsNotExist(err) {
			t.Fatalf("Thumbnail file does not exist at %s", thumbnailFilePath)
		}
	}

	expectedDimension := 256
	for _, thumbnail := range thumbnails {
		thumbnailFilePath := thumbnailsPath + "/" + thumbnail.ID.String() + ".jpg"

		file, err := os.Open(thumbnailFilePath)
		if err != nil {
			t.Fatalf("Failed to open thumbnail file %s: %v", thumbnailFilePath, err)
		}
		defer file.Close()

		img, _, err := image.DecodeConfig(file)
		if err != nil {
			t.Fatalf("Failed to decode image config for %s: %v", thumbnailFilePath, err)
		}

		if img.Width > expectedDimension && img.Height > expectedDimension {
			t.Fatalf("Expected at least one dimension of thumbnail to be at most %d, got %dx%d for %s",
				expectedDimension, img.Width, img.Height, thumbnailFilePath)
		}
	}
}

func TestMediaStore_loadMediaItems_shouldHandleExistingThumbnails(t *testing.T) {
	copyDir(t, "./tests/test3", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	mediaStore, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}

	expectedThumbnailsCount := 2
	timestamp := time.Date(2025, time.November, 23, 17, 2, 52, 0, time.UTC)
	thumbnails, err := mediaStore.GetThumbnailsBeforeTimestamp(timestamp, 10)
	if err != nil {
		t.Fatalf("Failed to get thumbnails: %v", err)
	}
	got := len(thumbnails)
	if got != expectedThumbnailsCount {
		t.Fatalf("Expected %d thumbnails, got %d", expectedThumbnailsCount, got)
	}
}

func TestMediaStore_GetThumbnailsBeforeTimestamp(t *testing.T) {
	copyDir(t, "./tests/test2", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	mediaStore, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}

	timestamp := time.Date(2025, time.November, 23, 17, 2, 52, 0, time.UTC)
	thumbnails, err := mediaStore.GetThumbnailsBeforeTimestamp(timestamp, 10)
	if err != nil {
		t.Fatalf("Failed to get thumbnails: %v", err)
	}

	got := len(thumbnails)
	expectedThumbnailsCount := 2
	if got != expectedThumbnailsCount {
		t.Fatalf("Expected %d thumbnails, got %d", expectedThumbnailsCount, got)
	}

	t1CreationTime, err := mediaStore.GetCreationTimeOfMediaItem(thumbnails[0].ID)
	if err != nil {
		t.Fatalf("Failed to get creation time of media item: %v", err)
	}
	t2CreationTime, err := mediaStore.GetCreationTimeOfMediaItem(thumbnails[1].ID)
	if err != nil {
		t.Fatalf("Failed to get creation time of media item: %v", err)
	}

	if t1CreationTime.Before(t2CreationTime) {
		t.Fatalf("Thumbnails are not sorted by creation time descending")
	}
}

func TestMediaStore_GetMediaItem(t *testing.T) {
	copyDir(t, "./tests/test2", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	mediaStore, err := NewMediaStore("./test", Sha256Hasher{})
	if err != nil {
		t.Fatalf("Failed to create media store: %v", err)
	}

	// get one of the media item IDs
	var testId sha256Hash
	err = testId.FromString("0bce366acd5c95aaf3d6c97b0b79645dec870870624ada0f74af1c871d7bef8b") // lake.jpg
	if err != nil {
		t.Fatalf("Failed to create sha256Hash from string: %v", err)
	}
	mediaItem, err := mediaStore.GetFullMediaItem(&testId)
	if err != nil {
		t.Fatalf("Failed to get media item: %v", err)
	}

	expectedFirst128Bytes := [128]int{255, 216, 255, 224, 0, 16, 74, 70, 73, 70, 0, 1, 1, 1, 0, 180, 0, 180, 0, 0, 255, 225, 125, 156, 69, 120, 105, 102, 0, 0, 73, 73, 42, 0, 8, 0, 0, 0, 13, 0, 15, 1, 2, 0, 10, 0, 0, 0, 170, 0, 0, 0, 16, 1, 2, 0, 9, 0, 0, 0, 182, 0, 0, 0, 18, 1, 3, 0, 1, 0, 0, 0, 1, 0, 0, 0, 26, 1, 5, 0, 1, 0, 0, 0, 196, 0, 0, 0, 27, 1, 5, 0, 1, 0, 0, 0, 204, 0, 0, 0, 40, 1, 3, 0, 1, 0, 0, 0, 2, 0, 0, 0, 49, 1, 2, 0, 10, 0, 0, 0, 212, 0, 0, 0, 50, 1, 2, 0}

	if len(mediaItem.Content) < 128 {
		t.Fatalf("Media item content is too short: %d bytes", len(mediaItem.Content))
	}

	for i := 0; i < 128; i++ {
		if int(mediaItem.Content[i]) != expectedFirst128Bytes[i] {
			t.Fatalf("Byte mismatch at position %d: expected %d, got %d", i, expectedFirst128Bytes[i], mediaItem.Content[i])
		}
	}
}
