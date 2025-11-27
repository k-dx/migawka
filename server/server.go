package main

import (
	"context"
	"time"

	pb "migawka-server/grpc"

	"github.com/rs/zerolog/log"
)

// server is used to implement helloworld.MigawkaServer.
type server struct {
	pb.UnimplementedMigawkaServer
	mediaStore MediaStore
}

func CreateServer(mediaDirectory string) *server {
	mediaStore, err := NewMediaStore(mediaDirectory, Xx64Hasher{})
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to create media store")
	}
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

		pbThumbnails = append(pbThumbnails, &pb.Thumbnail{
			Id:           thumbnail.ID.String(),
			CreationTime: creationTime.UTC().Format(time.RFC3339),
			Content:      thumbnail.Content,
		})
	}
	return pb.NewThumbnailsTimestampResponse(
		pbThumbnails,
		pb.NewStatus(200, ""),
	), nil
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
		Content:      mediaItem.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}
