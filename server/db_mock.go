package main

type MockDBRepository struct{}

func NewMockDBRepository() DBRepository {
	return &MockDBRepository{}
}

func (r *MockDBRepository) GetFileByPath(path string) (*FileRecord, error) {
	return nil, nil
}

func (r *MockDBRepository) UpsertFileRecord(fileRecord FileRecord) error {
	return nil
}

func (r *MockDBRepository) Close() error {
	return nil
}
