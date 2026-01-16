package main

import (
	"database/sql"
	"errors"
	"fmt"

	_ "github.com/mattn/go-sqlite3"
)

type SqliteDBRepository struct {
	db *sql.DB
}

func NewDBRepository(dbPath string) (DBRepository, error) {
	db, err := initOrOpenDB(dbPath)
	if err != nil {
		return nil, err
	}
	return &SqliteDBRepository{db: db}, nil
}

func initOrOpenDB(dbPath string) (*sql.DB, error) {
	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		return nil, fmt.Errorf("failed to open database: %w", err)
	}

	const query = `
	CREATE TABLE IF NOT EXISTS files (
		id    INTEGER PRIMARY KEY AUTOINCREMENT,
		filepath            TEXT NOT NULL UNIQUE,
		hash                TEXT NOT NULL UNIQUE,
		modified_at         DATETIME,
		media_creation_time DATETIME
	);`

	_, err = db.Exec(query)
	if err != nil {
		return nil, fmt.Errorf("failed to create table: %w", err)
	}

	return db, nil
}

func (r *SqliteDBRepository) GetFileByPath(path string) (*FileRecord, error) {
	var f FileRecord

	query := `SELECT id, filepath, hash, modified_at, media_creation_time FROM files WHERE filepath = ?`

	// QueryRow is used when you expect at most one result
	err := r.db.
		QueryRow(query, path).
		Scan(&f.ID, &f.Filepath, &f.Hash, &f.ModifiedAt, &f.MediaCreationTime)

	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, nil // No record found
		}
		return nil, err
	}

	return &f, nil
}

func (r *SqliteDBRepository) UpsertFileRecord(fileRecord FileRecord) error {
	query := `
	INSERT OR REPLACE INTO files (filepath, hash, modified_at, media_creation_time)
	VALUES (?, ?, ?, ?);
	`

	_, err := r.db.Exec(
		query,
		fileRecord.Filepath,
		fileRecord.Hash,
		fileRecord.ModifiedAt,
		fileRecord.MediaCreationTime,
	)
	if err != nil {
		return fmt.Errorf("failed to upsert file record: %w", err)
	}

	return nil
}

// TODO: use this!!!
func (r *SqliteDBRepository) Close() error {
	return r.db.Close()
}
