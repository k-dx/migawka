package main

type Xx64Hasher struct{}

func (Xx64Hasher) CalculateHash(data []byte) Hash {
	var h xx64Hash
	h.Calculate(data)
	return &h
}

func (Xx64Hasher) HashFromString(s string) (Hash, error) {
	var h xx64Hash
	if err := h.FromString(s); err != nil {
		return nil, err
	}
	return &h, nil
}

func (Xx64Hasher) HashToKey(h Hash) Key {
	return Key(h.String())
}

func (Xx64Hasher) HashFromKey(k Key) Hash {
	var h xx64Hash
	h.FromString(string(k))
	return &h
}
