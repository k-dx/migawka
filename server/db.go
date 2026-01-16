package main

import "time"

type FileRecord struct {
	ID                int
	Filepath          string
	Hash              string
	ModifiedAt        time.Time // File modification time
	MediaCreationTime time.Time // e.g. EXIF creation time
}

type DBRepository interface {
	GetFileByPath(path string) (*FileRecord, error)
	UpsertFileRecord(fileRecord FileRecord) error
	Close() error
}
