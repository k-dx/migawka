package main

type Sha256Hasher struct{}

func (Sha256Hasher) CalculateHash(data []byte) Hash {
	var h sha256Hash
	h.Calculate(data)
	return &h
}

func (Sha256Hasher) HashFromString(s string) (Hash, error) {
	var h sha256Hash
	if err := h.FromString(s); err != nil {
		return nil, err
	}
	return &h, nil
}

func (Sha256Hasher) HashToKey(h Hash) Key {
	return Key(h.String())
}

func (Sha256Hasher) HashFromKey(k Key) Hash {
	var h sha256Hash
	h.FromString(string(k))
	return &h
}
