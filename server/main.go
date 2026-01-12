package main

import (
	"flag"
	"fmt"
	"net"
	"os"

	pb "migawka-server/grpc"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

var (
	file []byte = nil
	port        = flag.Int("port", 50051, "The server port")
)

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
	generateThumbnailsOnStartup := flag.Bool("generate-thumbs-on-startup", false, "Generate missing thumbnails on startup")
	INSECURE_noTLS := flag.Bool("insecure-no-tls", false, "Disable TLS - do not use in production!")
	tlsServerPrivateKeyPath := flag.String("tls-private-key", "../certs/server_key.pem", "Path to TLS server private key")
	tlsServerCertPath := flag.String("tls-cert", "../certs/server_cert.pem", "Path to TLS server certificate")

	flag.Parse()

	if *mediaDirectory == "" {
		fmt.Fprintf(os.Stderr, "\n")
		fmt.Fprintf(os.Stderr, "	-%s is required!\n", MEDIA_DIR_ARG)
		fmt.Fprintf(os.Stderr, "\n")
		flag.Usage()
		os.Exit(1)
	}

	initLogger(logLevel)

	mediaStore, err := NewMediaStore(*mediaDirectory, Xx64Hasher{})
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to create media store")
	}
	if *generateThumbnailsOnStartup {
		mediaStore.GenerateMissingThumbnails()
	}

	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatal().Msgf("failed to listen: %v", err)
	}

	var creds credentials.TransportCredentials
	if !(*INSECURE_noTLS) {
		creds, err = credentials.NewServerTLSFromFile(*tlsServerCertPath, *tlsServerPrivateKeyPath)
		if err != nil {
			log.Fatal().Msgf("failed to create credentials for TLS: %v", err)
			os.Exit(1)
		}
	} else {
		log.Warn().Msg("TLS is disabled! Do not use in production!")
		creds = nil
	}

	grpcServer := grpc.NewServer(
		grpc.UnaryInterceptor(UnaryAuthInterceptor),
		grpc.StreamInterceptor(StreamAuthInterceptor),
		grpc.Creds(creds),
	)
	migawkaServer := CreateServer(mediaStore)

	pb.RegisterMigawkaServer(grpcServer, migawkaServer)
	log.Info().Msgf("server listening at %v", lis.Addr())
	if err := grpcServer.Serve(lis); err != nil {
		log.Fatal().Msgf("failed to serve: %v", err)
	}
}
