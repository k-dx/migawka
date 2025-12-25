package main

import (
	"context"
	"path/filepath"
	"sort"
	"strings"
	"time"

	pb "migawka-server/grpc"

	"github.com/rs/zerolog/log"
)

// server is used to implement helloworld.MigawkaServer.
type server struct {
	pb.UnimplementedMigawkaServer
	mediaStore MediaStore
}

func CreateServer(mediaStore MediaStore) *server {
	return &server{
		mediaStore: mediaStore,
	}
}

// SayHello implements helloworld.GreeterServer
func (s *server) SayHello(_ context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	log.Info().Str("name", in.GetName()).Msg("SayHello")
	return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}

func (s *server) UploadFile(_ context.Context, in *pb.FileUploadRequest) (*pb.FileUploadReply, error) {
	log.Info().Str("filename", in.GetFilename()).Int("size", len(in.GetContent())).Msg("UploadFile")

	file = in.GetContent()

	return &pb.FileUploadReply{Message: "File " + in.GetFilename() + " uploaded successfully"}, nil
}

func (s *server) DownloadFile(_ context.Context, in *pb.FileDownloadRequest) (*pb.FileDownloadReply, error) {
	log.Info().Str("filename", in.GetFilename()).Msg("DownloadFile")

	if file == nil {
		return &pb.FileDownloadReply{Message: "No file uploaded"}, nil
	}

	return &pb.FileDownloadReply{Message: "File " + in.GetFilename() + " downloaded successfully", Content: file}, nil
}

func (s *server) GetThumbnailsBeforeTimestamp(_ context.Context, in *pb.ThumbnailsTimestampRequest) (*pb.ThumbnailsTimestampResponse, error) {
	log.Info().
		Str("Timestamp", in.GetTimestamp()).
		Uint32("Count", in.GetCount()).
		Msgf("GetThumbnailsBeforeTimestamp")

	// parse timestamp
	timestamp := in.GetTimestamp()
	parsedTimestamp, err := time.Parse(time.RFC3339, timestamp)
	if err != nil {
		log.Error().Err(err).Str("timestamp", timestamp).Msg("Invalid timestamp format")
		status := pb.NewStatus(400, "Invalid timestamp format. Expected format ISO 8601: YYYY-MM-DDTHH:MM:SSZ")
		return pb.NewThumbnailsTimestampResponse(nil, status), nil
	}

	// retrieve thumbnails from media store
	thumbnails, err := s.mediaStore.GetThumbnailsBeforeTimestamp(parsedTimestamp, uint(in.GetCount()))
	if err != nil {
		log.Error().Err(err).Msg("Failed to get thumbnails from media store")
		status := pb.NewStatus(500, "Failed to get thumbnails from media store")
		return pb.NewThumbnailsTimestampResponse(nil, status), nil
	}

	// convert to gRPC thumbnails type
	var pbThumbnails []*pb.Thumbnail
	for _, thumbnail := range thumbnails {
		creationTime, err := s.mediaStore.GetCreationTimeOfMediaItem(thumbnail.ID)
		if err != nil {
			log.Error().Err(err).Msg("Failed to get creation time of media item")
			status := pb.NewStatus(500, "Failed to get creation time of media item")
			return pb.NewThumbnailsTimestampResponse(nil, status), nil
		}

		pbThumbnails = append(pbThumbnails, toPbThumbnail(thumbnail, creationTime))
	}
	return pb.NewThumbnailsTimestampResponse(
		pbThumbnails,
		pb.NewStatus(200, ""),
	), nil
}

func toPbThumbnail(thumbnail Thumbnail, creationTime time.Time) *pb.Thumbnail {
	return &pb.Thumbnail{
		Id:           thumbnail.ID.String(),
		CreationTime: creationTime.UTC().Format(time.RFC3339),
		Content:      thumbnail.Content,
	}
}

func (s *server) GetOptimizedMediaItem(_ context.Context, in *pb.GetMediaItemRequest) (*pb.GetMediaItemResponse, error) {
	log.Info().Str("id", in.GetId()).Msg("GetOptimizedMediaItem")

	// parse id
	id, err := s.mediaStore.GetHasher().HashFromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// get media item from media store
	mediaItem, err := s.mediaStore.GetOptimizedMediaItem(id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get media item from media store")
		status := pb.NewStatus(500, "Failed to get media item from media store")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// convert to gRPC media item type
	pbMediaItem := &pb.MediaItem{
		Id:           mediaItem.Metadata.ID.String(),
		CreationTime: mediaItem.Metadata.CreationTime.UTC().Format(time.RFC3339),
		Content:      mediaItem.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}

func (s *server) GetFullMediaItem(_ context.Context, in *pb.GetMediaItemRequest) (*pb.GetMediaItemResponse, error) {
	log.Info().Str("id", in.GetId()).Msg("GetFullMediaItem")

	// parse id
	id, err := s.mediaStore.GetHasher().HashFromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// get media item from media store
	mediaItem, err := s.mediaStore.GetFullMediaItem(id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get media item from media store")
		status := pb.NewStatus(500, "Failed to get media item from media store")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// convert to gRPC media item type
	pbMediaItem := &pb.MediaItem{
		Id:           mediaItem.Metadata.ID.String(),
		CreationTime: mediaItem.Metadata.CreationTime.UTC().Format(time.RFC3339),
		Path:         mediaItem.Metadata.Path,
		Content:      mediaItem.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}

func IsPathInsideBase(basePath, targetPath string) (bool, error) {
	absBasePath, err := filepath.Abs(basePath)
	if err != nil {
		return false, err
	}

	absTargetPath, err := filepath.Abs(targetPath)
	if err != nil {
		return false, err
	}

	relPath, err := filepath.Rel(absBasePath, absTargetPath)
	if err != nil {
		return false, err
	}

	return !strings.HasPrefix(relPath, ".."), nil
}

func (s *server) GetFileListPage(_ context.Context, in *pb.GetFileListPageRequest) (*pb.GetFileListPageResponse, error) {
	log.Info().
		Str("path", in.GetPath()).
		Uint32("page_number", in.GetPageNumber()).
		Uint32("page_size", in.GetPageSize()).
		Msg("GetFileListPage")

	requestedRelativePath := in.GetPath()
	pageNumber := in.GetPageNumber()
	pageSize := in.GetPageSize()

	mediaDir := s.mediaStore.GetMediaDirectory()
	thumbnailDir := s.mediaStore.GetThumbnailDirectory()

	requestedAbsPath := filepath.Join(mediaDir, requestedRelativePath)

	// check that requestedRelativePath is inside media store directory
	if ok, err := IsPathInsideBase(mediaDir, requestedAbsPath); err != nil || !ok {
		log.Warn().
			Str("path", requestedRelativePath).
			Msg("Requested path is outside media directory")
		status := pb.NewStatus(400, "Requested path is outside media directory")
		return pb.NewGetFileListPageResponse(nil, status), nil
	}

	dirs, err := GetDirsInDir(mediaDir, thumbnailDir, requestedRelativePath)
	if err != nil {
		log.Error().
			Err(err).
			Str("path", requestedRelativePath).
			Msg("Failed to get directories in path")
		status := pb.NewStatus(500, "Failed to get directories in path")
		return pb.NewGetFileListPageResponse(nil, status), nil
	}

	sort.Slice(dirs, func(i, j int) bool {
		return dirs[i].Name < dirs[j].Name
	})

	thumbnails, thumbnailFilenames, err := s.mediaStore.GetThumbnailsByPath(requestedRelativePath)
	if err != nil {
		log.Error().
			Err(err).
			Str("path", requestedRelativePath).
			Msg("Failed to get thumbnails in path")
		status := pb.NewStatus(500, "Failed to get thumbnails in path")
		return pb.NewGetFileListPageResponse(nil, status), nil
	}

	// sort thumbnails and thumbnailsFilenames by filename
	// so that they correspond to each other
	// (assuming thumbnailFilenames are unique)
	type thumbWithName struct {
		thumb    Thumbnail
		filename string
	}
	var thumbsWithNames []thumbWithName
	for i, thumb := range thumbnails {
		thumbsWithNames = append(thumbsWithNames, thumbWithName{
			thumb:    thumb,
			filename: thumbnailFilenames[i],
		})
	}
	// sort by filename
	sort.Slice(thumbsWithNames, func(i, j int) bool {
		return thumbsWithNames[i].filename < thumbsWithNames[j].filename
	})

	var entries []*pb.DirectoryEntry
	for _, dir := range dirs {
		entries = append(entries, &pb.DirectoryEntry{
			Name: dir.Name,
			Type: pb.DirectoryEntry_DIRECTORY,
		})
	}

	// convert to gRPC thumbnails type
	for _, thumb := range thumbsWithNames {
		creationTime, err := s.mediaStore.GetCreationTimeOfMediaItem(thumb.thumb.ID)
		if err != nil {
			log.Error().Err(err).Msg("Failed to get creation time of media item")
			status := pb.NewStatus(500, "Failed to get creation time of media item")
			return pb.NewGetFileListPageResponse(nil, status), nil
		}

		entries = append(entries, &pb.DirectoryEntry{
			Name:      thumb.filename,
			Type:      pb.DirectoryEntry_MEDIA,
			Thumbnail: toPbThumbnail(thumb.thumb, creationTime),
		})
	}

	// pagination
	startIndex := int(pageNumber * pageSize)
	endIndex := startIndex + int(pageSize)
	if startIndex > len(entries) {
		startIndex = len(entries)
	}
	if endIndex > len(entries) {
		endIndex = len(entries)
	}
	entries = entries[startIndex:endIndex]

	return pb.NewGetFileListPageResponse(entries, pb.NewStatus(200, "")), nil
}

func (s *server) GetTimelineEntries(_ context.Context, in *pb.TimelineEntriesRequest) (*pb.TimelineEntriesResponse, error) {
	log.Info().Msg("GetTimelineEntries")

	// retrieve thumbnails from media store
	largestUint := ^uint(0)
	dateInfinity := time.Date(1000000, time.January, 1, 0, 0, 0, 0, time.UTC)
	thumbnails, err := s.mediaStore.GetThumbnailsBeforeTimestamp(dateInfinity, largestUint)
	if err != nil {
		log.Error().Err(err).Msg("Failed to get entries from media store")
		status := pb.NewStatus(500, "Failed to get entries from media store")
		return pb.NewTimelineEntriesResponse(nil, status), nil
	}

	log.Debug().Int("count", len(thumbnails)).Msg("Retrieved thumbnails for timeline")

	// convert to gRPC TimelineEntry type
	var pbTimelineEntries []*pb.TimelineEntry
	for _, thumbnail := range thumbnails {
		creationTime, err := s.mediaStore.GetCreationTimeOfMediaItem(thumbnail.ID)
		if err != nil {
			log.Error().Err(err).Msg("Failed to get creation time of media item")
			continue
		}

		pbTimelineEntries = append(pbTimelineEntries, toPbTimelineEntry(thumbnail.ID, creationTime))
	}

	return pb.NewTimelineEntriesResponse(
		pbTimelineEntries,
		pb.NewStatus(200, ""),
	), nil

}

func toPbTimelineEntry(id Hash, creationTime time.Time) *pb.TimelineEntry {
	return &pb.TimelineEntry{
		Id:           id.String(),
		CreationTime: creationTime.UTC().Format(time.RFC3339),
	}
}
