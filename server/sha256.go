package main

import (
	"crypto/sha256"
	"encoding/hex"
	"fmt"
	"strings"
)

const sha256HashSize = 32

type sha256Hash [sha256HashSize]byte

func (h *sha256Hash) Bytes() []byte {
	return h[:]
}

func (h *sha256Hash) FromBytes(b []byte) error {
	if len(b) != sha256HashSize {
		return fmt.Errorf("invalid hash length: got %d, want %d", len(b), sha256HashSize)
	}
	copy(h[:], b)
	return nil
}

func (h *sha256Hash) FromString(s string) error {
	if len(s) > 2*sha256HashSize {
		return fmt.Errorf("invalid hash string length: got %d, want at most %d", len(s), 2*sha256HashSize)
	}
	s = strings.Repeat("0", sha256HashSize*2-len(s)) + s

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

func (h *sha256Hash) Calculate(data []byte) Hash {
	*h = sha256.Sum256(data)
	return h
}
