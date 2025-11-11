package main

import (
	"encoding/hex"
	"fmt"
)

const sha256HashSize = 32

type sha256Hash [sha256HashSize]byte

func NewSha256FromString(s string) (*sha256Hash, error) {
	var h sha256Hash
	err := h.FromString(s)
	if err != nil {
		return nil, err
	}
	return &h, nil
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
