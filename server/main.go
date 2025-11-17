package main

import (
	"context"
	"flag"
	"fmt"
	"net"
	"os"
	"time"

	pb "migawka-server/grpc"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
)

var (
	file []byte = nil
	port        = flag.Int("port", 50051, "The server port")
)

// server is used to implement helloworld.GreeterServer.
type server struct {
	pb.UnimplementedGreeterServer // TODO: rename to MigawkaServer
	mediaStore                    MediaStore
}

func CreateServer(mediaDirectory string) *server {
	mediaStore, err := NewMediaStore(mediaDirectory)
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
		pbThumbnails = append(pbThumbnails, &pb.Thumbnail{
			Id:           thumbnail.ID.String(),
			CreationTime: thumbnail.CreationTime.UTC().Format(time.RFC3339),
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
	id, err := NewSha256FromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// get media item from media store
	mediaItem, err := s.mediaStore.GetOptimizedMediaItem(*id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get media item from media store")
		status := pb.NewStatus(500, "Failed to get media item from media store")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// convert to gRPC media item type
	pbMediaItem := &pb.MediaItem{
		Id:           mediaItem.ID.String(),
		CreationTime: mediaItem.CreationTime.UTC().Format(time.RFC3339),
		Content:      mediaItem.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}

func (s *server) GetFullMediaItem(_ context.Context, in *pb.GetMediaItemRequest) (*pb.GetMediaItemResponse, error) {
	log.Info().Str("id", in.GetId()).Msg("GetFullMediaItem")

	// parse id
	id, err := NewSha256FromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// get media item from media store
	mediaItem, err := s.mediaStore.GetFullMediaItem(*id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get media item from media store")
		status := pb.NewStatus(500, "Failed to get media item from media store")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// convert to gRPC media item type
	pbMediaItem := &pb.MediaItem{
		Id:           mediaItem.ID.String(),
		CreationTime: mediaItem.CreationTime.UTC().Format(time.RFC3339),
		Content:      mediaItem.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}

func initLogger(logLevel *string) {
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	// comment line below to disable pretty logging and log in JSON instead
	log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout})

	// Set log level based on flag
	switch *logLevel {
	case "debug":
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	case "info":
		zerolog.SetGlobalLevel(zerolog.InfoLevel)
	case "warn":
		zerolog.SetGlobalLevel(zerolog.WarnLevel)
	case "error":
		zerolog.SetGlobalLevel(zerolog.ErrorLevel)
	case "fatal":
		zerolog.SetGlobalLevel(zerolog.FatalLevel)
	case "panic":
		zerolog.SetGlobalLevel(zerolog.PanicLevel)
	default:
		zerolog.SetGlobalLevel(zerolog.DebugLevel)
	}
}

func main() {
	logLevel := flag.String("loglevel", "warn", "Log level: debug, info, warn, error, fatal, panic")
	MEDIA_DIR_ARG := "mediadir"
	mediaDirectory := flag.String(MEDIA_DIR_ARG, "", "Path to media directory (required), cannot contain tilde (~)")

	flag.Parse()

	if *mediaDirectory == "" {
		fmt.Fprintf(os.Stderr, "\n")
		fmt.Fprintf(os.Stderr, "	-%s is required!\n", MEDIA_DIR_ARG)
		fmt.Fprintf(os.Stderr, "\n")
		flag.Usage()
		os.Exit(1)
	}

	initLogger(logLevel)

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatal().Msgf("failed to listen: %v", err)
	}

	grpcServer := grpc.NewServer()
	migawkaServer := CreateServer(*mediaDirectory)

	pb.RegisterGreeterServer(grpcServer, migawkaServer)
	log.Info().Msgf("server listening at %v", lis.Addr())
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatal().Msgf("failed to serve: %v", err)
	}
}
