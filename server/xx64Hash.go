package main

import (
	"encoding/binary"
	"fmt"
	"strconv"

	"github.com/cespare/xxhash/v2"
)

const xx64HashSize = 8

type xx64Hash uint64

func (h *xx64Hash) Bytes() []byte {
	result := make([]byte, xx64HashSize)
	binary.LittleEndian.PutUint64(result, uint64(*h))
	return result
}

func (h *xx64Hash) FromBytes(b []byte) error {
	if len(b) != xx64HashSize {
		return fmt.Errorf("invalid hash length: got %d, want %d", len(b), xx64HashSize)
	}
	*h = xx64Hash(binary.LittleEndian.Uint64(b))
	return nil
}

func (h *xx64Hash) FromString(s string) error {
	val, err := strconv.ParseUint(s, 16, 64)
	if err != nil {
		fmt.Println("Error parsing hex string:", err)
		return err
	}
	*h = xx64Hash(val)
	return nil
}

func (h *xx64Hash) String() string {
	return strconv.FormatUint(uint64(*h), 16)
}

func (h *xx64Hash) Calculate(data []byte) Hash {
	*h = xx64Hash(xxhash.Sum64(data))
	return h
}
