package main

import (
	"os"
	"path/filepath"
	"sort"
	"testing"
)

func TestGetDirsInDir(t *testing.T) {
	copyDir(t, "./tests/test4", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	relativeThumbnailDir := "dir/.thumbnails"
	absThumbnailDir := filepath.Join("./test", relativeThumbnailDir)
	dirs, err := GetDirsInDir("./test", absThumbnailDir, "")
	if err != nil {
		t.Fatalf("GetDirsInDir failed: %v", err)
	}

	// expected directories, sorted by name
	expectedDirs := []Directory{
		{Name: "dir"},
		{Name: "dir1"},
		{Name: "dir2"},
	}

	// sort expectedDirs and dirs for comparison
	sort.Slice(dirs, func(i, j int) bool {
		return dirs[i].Name < dirs[j].Name
	})

	if len(dirs) != len(expectedDirs) {
		t.Fatalf("Expected %d directories, got %d", len(expectedDirs), len(dirs))
	}

	for i, dir := range dirs {
		if dir.Name != expectedDirs[i].Name {
			t.Fatalf("Expected directory name '%s', got '%s'", expectedDirs[i].Name, dir.Name)
		}
	}
}

func TestGetDirsInDir2(t *testing.T) {
	copyDir(t, "./tests/test4", "./test")
	t.Cleanup(func() {
		os.RemoveAll("./test")
	})

	relativeThumbnailDir := "dir/.thumbnails"
	absThumbnailDir := filepath.Join("./test", relativeThumbnailDir)
	dirs, err := GetDirsInDir("./test", absThumbnailDir, "dir")
	if err != nil {
		t.Fatalf("GetDirsInDir failed: %v", err)
	}

	// expected directories, sorted by name
	expectedDirs := []Directory{
		{Name: "sub"},
	}

	// sort expectedDirs and dirs for comparison
	sort.Slice(dirs, func(i, j int) bool {
		return dirs[i].Name < dirs[j].Name
	})

	if len(dirs) != len(expectedDirs) {
		t.Fatalf("Expected %d directories, got %d", len(expectedDirs), len(dirs))
	}

	for i, dir := range dirs {
		if dir.Name != expectedDirs[i].Name {
			t.Fatalf("Expected directory name '%s', got '%s'", expectedDirs[i].Name, dir.Name)
		}
	}
}
