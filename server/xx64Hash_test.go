package main

import (
	"testing"
)

func TestXx64Hash_calculatesCorrectValue(t *testing.T) {
	var h xx64Hash
	h.Calculate([]byte("The quick brown fox jumps over the lazy dog"))
	expected := "0b242d361fda71bc"
	if h.String() != expected {
		t.Fatalf("xx64 hash calculation incorrect: got %s, want %s", h.String(), expected)
	}
}

func TestXx64Hash_BytesReturnsCorrectBytes(t *testing.T) {
	var h xx64Hash
	h.Calculate([]byte("The quick brown fox jumps over the lazy dog"))
	expectedBytes := []byte{0xbc, 0x71, 0xda, 0x1f, 0x36, 0x2d, 0x24, 0xb}
	actualBytes := h.Bytes()
	if len(actualBytes) != len(expectedBytes) {
		t.Fatalf("xx64 hash.Bytes() length incorrect: got %d, want %d", len(actualBytes), len(expectedBytes))
	}
	for i := range expectedBytes {
		if actualBytes[i] != expectedBytes[i] {
			t.Fatalf("xx64 hash byte at index %d incorrect: got %x, want %x", i, actualBytes[i], expectedBytes[i])
		}
	}
}

func TestXx64Hash_StringDoesProducePrefixZeros(t *testing.T) {
	var h xx64Hash
	h.Calculate([]byte("The quick brown fox jumps over the lazy dog"))
	expectedLength := 16 // not 16
	actual := h.String()
	if actual != "0b242d361fda71bc" {
		t.Fatalf("xx64 hash.String() incorrect: got %s, want %s", actual, "0b242d361fda71bc")
	}
	if len(actual) != expectedLength {
		t.Fatalf("xx64 hash.String() length incorrect: got %d, want %d", len(actual), expectedLength)
	}
}

func TestXx64Hash_FromStringParsesPrefixZeros(t *testing.T) {
	var h xx64Hash
	err := h.FromString("00b")
	if err != nil {
		t.Fatalf("Failed to parse xx64 hash from string: %v", err)
	}
	expected := "000000000000000b"
	if h.String() != expected {
		t.Fatalf("Parsed xx64 hash incorrect: got %s, want %s", h.String(), expected)
	}
}

func TestXx64Hash_FromStringParsesNoPrefixZeros(t *testing.T) {
	var h xx64Hash
	err := h.FromString("b")
	if err != nil {
		t.Fatalf("Failed to parse xx64 hash from string: %v", err)
	}
	expected := "000000000000000b"
	if h.String() != expected {
		t.Fatalf("Parsed xx64 hash incorrect: got %s, want %s", h.String(), expected)
	}
}
