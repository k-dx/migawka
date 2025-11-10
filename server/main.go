package main

import (
	"context"
	"flag"
	"fmt"
	"net"
	"os"

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
	mediaDirectory := flag.String(MEDIA_DIR_ARG, "", "Path to media directory (required)")

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
