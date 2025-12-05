package main

import (
	"os"
	"path/filepath"
)

type Directory struct {
	Name string
}

func GetDirsInDir(mediadir string, path string) ([]Directory, error) {
	absPath := filepath.Join(mediadir, path)
	entries, err := os.ReadDir(absPath)
	if err != nil {
		return nil, err
	}

	var dirs []Directory
	for _, entry := range entries {
		if entry.IsDir() {
			dirs = append(dirs, Directory{Name: entry.Name()})
		}
	}
	return dirs, nil
}
