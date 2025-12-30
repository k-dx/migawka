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

func NewGetFileListResponse(entries []*DirectoryEntry, status *Status) *GetFileListResponse {
	return &GetFileListResponse{
		Status:  status,
		Entries: entries,
	}
}

func NewTimelineEntriesResponse(entries []*TimelineEntry, status *Status) *TimelineEntriesResponse {
	return &TimelineEntriesResponse{
		Entries: entries,
		Status:  status,
	}
}
