package main

import (
	"os"
	"testing"
	"time"
)

func TestDB_1(t *testing.T) {
	defer func() {
		os.Remove("./test.db")
	}()
	db, err := NewDBRepository("./test.db")
	if err != nil {
		t.Fatalf("Failed to create DB repository: %v", err)
	}
	defer db.Close()

	modTime, err := time.Parse(time.RFC3339, "2024-05-01T12:00:00Z")
	if err != nil {
		t.Fatalf("Failed to parse time: %v", err)
	}
	mediaCreationTime, err := time.Parse(time.RFC3339, "2024-05-01T10:00:00Z")
	if err != nil {
		t.Fatalf("Failed to parse time: %v", err)
	}

	err = db.UpsertFileRecord(FileRecord{
		Filepath:          "/path/to/a.jpg",
		Hash:              "abc123",
		ModifiedAt:        modTime,
		MediaCreationTime: mediaCreationTime,
	})
	if err != nil {
		t.Fatalf("Failed to upsert file record: %v", err)
	}
	err = db.UpsertFileRecord(FileRecord{
		Filepath:          "/path/to/b.jpg",
		Hash:              "def456",
		ModifiedAt:        modTime,
		MediaCreationTime: mediaCreationTime,
	})
	if err != nil {
		t.Fatalf("Failed to upsert file record: %v", err)
	}

	// rename a.jpg to b.jpg
	err = db.UpsertFileRecord(FileRecord{
		Filepath:          "/path/to/b.jpg",
		Hash:              "abc123",
		ModifiedAt:        modTime,
		MediaCreationTime: mediaCreationTime,
	})
	if err != nil {
		t.Fatalf("Failed to upsert file record: %v", err)
	}

}
