package grpc

func NewGetMediaItemResponse(mediaItem *MediaItem, status *Status) *GetMediaItemResponse {
	return &GetMediaItemResponse{
		MediaItem: mediaItem,
		Status:    status,
	}
}

func NewThumbnailsTimestampResponse(thumbnails []*Thumbnail, status *Status) *ThumbnailsTimestampResponse {
	return &ThumbnailsTimestampResponse{
		Thumbnails: thumbnails,
		Status:     status,
	}
}

func NewStatus(code int32, message string) *Status {
	return &Status{
		Code:    code,
		Message: message,
	}
}
