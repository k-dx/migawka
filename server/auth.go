package main

import (
	"context"
	"os"
	"strings"

	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/metadata"
	"google.golang.org/grpc/status"
)

func loadTokens() []string {
	s := os.Getenv("CLIENT_TOKENS")
	tokens := strings.Split(s, ",")

	var cleanedTokens []string
	for _, token := range tokens {
		trimmed := strings.TrimSpace(token)
		if trimmed != "" {
			cleanedTokens = append(cleanedTokens, trimmed)
		}
	}
	return cleanedTokens
}

func validateToken(ctx context.Context) (context.Context, error) {
	md, ok := metadata.FromIncomingContext(ctx)
	if !ok {
		return nil, status.Errorf(codes.Unauthenticated, "metadata is not provided")
	}

	values := md["authorization"]
	if len(values) == 0 {
		return nil, status.Errorf(codes.Unauthenticated, "authorization token is not provided")
	}

	requestToken := values[0]

	allowedTokens := loadTokens()
	for _, allowedToken := range allowedTokens {
		if requestToken == allowedToken {
			return ctx, nil
		}
	}

	return nil, status.Errorf(codes.Unauthenticated, "token is invalid")
}

// Intercepts unary RPCs
func UnaryAuthInterceptor(ctx context.Context, req interface{}, info *grpc.UnaryServerInfo, handler grpc.UnaryHandler) (interface{}, error) {
	// Call core validation logic
	newCtx, err := validateToken(ctx)
	if err != nil {
		return nil, err
	}

	return handler(newCtx, req)
}

// Intercepts streaming RPCs
func StreamAuthInterceptor(srv interface{}, ss grpc.ServerStream, info *grpc.StreamServerInfo, handler grpc.StreamHandler) error {
	ctx := ss.Context()

	newCtx, err := validateToken(ctx)
	if err != nil {
		return err
	}

	wrappedStream := &wrappedServerStream{
		ServerStream: ss,
		ctx:          newCtx,
	}

	return handler(srv, wrappedStream)
}

// Helper struct to wrap the stream with a modified context
type wrappedServerStream struct {
	grpc.ServerStream
	ctx context.Context
}

func (w *wrappedServerStream) Context() context.Context {
	return w.ctx
}
