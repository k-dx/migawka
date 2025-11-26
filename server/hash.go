package main

type Hash interface {
	FromBytes(b []byte) error
	Bytes() []byte

	FromString(s string) error
	String() string

	// Computes the hash of the given data and updates the receiver.
	Calculate(data []byte) Hash
}
