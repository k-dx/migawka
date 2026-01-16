package main

import (
	"flag"
	"fmt"
	"net"
	"os"
	"os/signal"
	"syscall"

	pb "migawka-server/grpc"

	"github.com/rs/zerolog"
	"github.com/rs/zerolog/log"
	"google.golang.org/grpc"
	"google.golang.org/grpc/credentials"
)

var (
	file []byte = nil
)

func initLogger(logLevel *string, logFormat *string) {
	zerolog.TimeFieldFormat = zerolog.TimeFormatUnix
	if *logFormat == "console" {
		// pretty console logging
		log.Logger = log.Output(zerolog.ConsoleWriter{Out: os.Stdout})
	} else {
		// JSON logging
		log.Logger = log.Output(os.Stdout)
	}

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
	port := flag.Int("port", 50051, "The server port")

	logLevel := flag.String("loglevel", "warn", "Log level: debug, info, warn, error, fatal, panic")
	logFormat := flag.String("logformat", "console", "Log format: console, json")

	MEDIA_DIR_ARG := "mediadir"
	mediaDirectory := flag.String(MEDIA_DIR_ARG, "", "Path to media directory (required), cannot contain tilde (~)")
	dbPath := flag.String("dbpath", "./migawka.sqlite", "Path to the database file")
	generateThumbnailsOnStartup := flag.Bool("generate-thumbs-on-startup", false, "Generate missing thumbnails on startup")

	INSECURE_noTLS := flag.Bool("insecure-no-tls", false, "Disable TLS - do not use in production!")
	tlsServerPrivateKeyPath := flag.String("tls-private-key", "../certs/server_key.pem", "Path to TLS server private key")
	tlsServerCertPath := flag.String("tls-cert", "../certs/server_cert.pem", "Path to TLS server certificate")

	flag.Parse()

	// Create a channel to listen for OS signals
	stop := make(chan os.Signal, 1)
	// notify about SIGINT and SIGTERM
	signal.Notify(stop, os.Interrupt, syscall.SIGTERM)

	if *mediaDirectory == "" {
		fmt.Fprintf(os.Stderr, "\n")
		fmt.Fprintf(os.Stderr, "	-%s is required!\n", MEDIA_DIR_ARG)
		fmt.Fprintf(os.Stderr, "\n")
		flag.Usage()
		os.Exit(1)
	}

	initLogger(logLevel, logFormat)

	dbRepo, err := NewDBRepository(*dbPath)
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to create DB repository")
	}

	mediaStore, err := NewMediaStore(*mediaDirectory, dbRepo, Xx64Hasher{})
	if err != nil {
		log.Fatal().Err(err).Msg("Failed to create media store")
	}
	defer func() {
		log.Debug().Msg("Closing media store")
		err := mediaStore.Close()
		if err != nil {
			log.Error().Err(err).Msg("Failed to close media store")
		}
	}()
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

	// Start the server in a goroutine so it doesn't block
	go func() {
		log.Info().Msgf("server listening at %v", lis.Addr())
		if err := grpcServer.Serve(lis); err != nil {
			log.Fatal().Msgf("failed to serve: %v", err)
		}
	}()

	// Block until we receive our signal.
	sig := <-stop
	log.Info().Msgf("Received signal: %v. Shutting down gracefully...", sig)

	// GracefulStop allows active RPCs to finish before closing connections
	grpcServer.GracefulStop()

	log.Info().Msg("Server stopped.")
}
