package main

type Sha1Hasher struct{}

func (Sha1Hasher) CalculateHash(data []byte) Hash {
	var h sha1Hash
	h.Calculate(data)
	return &h
}

func (Sha1Hasher) HashFromString(s string) (Hash, error) {
	var h sha1Hash
	if err := h.FromString(s); err != nil {
		return nil, err
	}
	return &h, nil
}

func (Sha1Hasher) HashToKey(h Hash) Key {
	return Key(h.String())
}

func (Sha1Hasher) HashFromKey(k Key) Hash {
	var h sha1Hash
	h.FromString(string(k))
	return &h
}
