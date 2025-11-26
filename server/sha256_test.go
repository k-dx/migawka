package main

import (
	"testing"
)

func TestSha256Hash_FromString(t *testing.T) {
	var h sha256Hash
	err := h.FromString("0bce366acd5c95aaf3d6c97b0b79645dec870870624ada0f74af1c871d7bef8b")
	if err != nil {
		t.Fatalf("Failed to parse sha256 hash from string: %v", err)
	}
	expectedBytes := []byte{
		0x0b, 0xce, 0x36, 0x6a, 0xcd, 0x5c, 0x95, 0xaa,
		0xf3, 0xd6, 0xc9, 0x7b, 0x0b, 0x79, 0x64, 0x5d,
		0xec, 0x87, 0x08, 0x70, 0x62, 0x4a, 0xda, 0x0f,
		0x74, 0xaf, 0x1c, 0x87, 0x1d, 0x7b, 0xef, 0x8b,
	}

	for i := range expectedBytes {
		if h.Bytes()[i] != expectedBytes[i] {
			t.Fatalf("Parsed sha256 hash byte at index %d does not match expected byte", i)
		}
	}
}

func TestSha256Hash_Calculate(t *testing.T) {
	var h sha256Hash
	h.Calculate([]byte("test data"))
	expectedString := "916f0027a575074ce72a331777c3478d6513f786a591bd892da1a577bf2335f9"

	if h.String() != expectedString {
		t.Fatalf("Sha256 hash string does not match expected string:\ngot  %s\nwant %s", h.String(), expectedString)
	}
}

func TestSha256Hash_FromStringToString(t *testing.T) {
	expectedString := "0bce366acd5c95aaf3d6c97b0b79645dec870870624ada0f74af1c871d7bef8b"
	var h sha256Hash
	err := h.FromString(expectedString)
	if err != nil {
		t.Fatalf("Failed to parse sha256 hash from string: %v", err)
	}

	actualString := h.String()
	if actualString != expectedString {
		t.Fatalf("Sha256 hash string does not match expected string after FromString and String calls")
	}
}

func TestSha256Hash_FromBytes(t *testing.T) {
	expectedBytes := []byte{
		0x0b, 0xce, 0x36, 0x6a, 0xcd, 0x5c, 0x95, 0xaa,
		0xf3, 0xd6, 0xc9, 0x7b, 0x0b, 0x79, 0x64, 0x5d,
		0xec, 0x87, 0x08, 0x70, 0x62, 0x4a, 0xda, 0x0f,
		0x74, 0xaf, 0x1c, 0x87, 0x1d, 0x7b, 0xef, 0x8b,
	}

	var h sha256Hash
	err := h.FromBytes(expectedBytes)
	if err != nil {
		t.Fatalf("Failed to parse sha256 hash from bytes: %v", err)
	}

	actualBytes := h.Bytes()
	for i := range expectedBytes {
		if actualBytes[i] != expectedBytes[i] {
			t.Fatalf("Parsed sha256 hash byte at index %d does not match expected byte", i)
		}
	}
}
