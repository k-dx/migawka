package main

import (
	"crypto/sha1"
	"encoding/hex"
	"fmt"
)

const sha1HashSize = 20

type sha1Hash [sha1HashSize]byte

func (h *sha1Hash) Bytes() []byte {
	return h[:]
}

func (h *sha1Hash) FromBytes(b []byte) error {
	if len(b) != sha1HashSize {
		return fmt.Errorf("invalid hash length: got %d, want %d", len(b), sha1HashSize)
	}
	copy(h[:], b)
	return nil
}

func (h *sha1Hash) FromString(s string) error {
	data, err := hex.DecodeString(s)
	if err != nil {
		return err
	}
	if len(data) != sha1HashSize {
		return fmt.Errorf("invalid hash length: got %d, want %d", len(data), sha1HashSize)
	}
	copy(h[:], data)
	return nil
}

func (h *sha1Hash) String() string {
	return hex.EncodeToString(h[:])
}

func (h *sha1Hash) Calculate(data []byte) Hash {
	*h = sha1.Sum(data)
	return h
}
