package main

import (
	"os"
	"path/filepath"
)

type Directory struct {
	Name string
}

// GetDirsInDir returns a list of directories in the given path relative to
// mediadir. It's non-recursive (only immediate subdirectories in path are
// returned). Ignores the thumbnail directory, given by thmbnaildir relative
// to mediadir.
func GetDirsInDir(absMediaDir, absThumbnailDir, path string) ([]Directory, error) {
	absPath := filepath.Join(absMediaDir, path)
	entries, err := os.ReadDir(absPath)
	if err != nil {
		return nil, err
	}

	var dirs []Directory
	for _, entry := range entries {
		absEntryPath := filepath.Join(absPath, entry.Name())
		// skip thumbnail directory
		if entry.IsDir() && absEntryPath != absThumbnailDir {
			dirs = append(dirs, Directory{Name: entry.Name()})
		}
	}
	return dirs, nil
}
