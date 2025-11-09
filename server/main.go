package main

import (
	"context"
	"flag"
	"fmt"
	"log"
	"net"

	pb "migawka-server/grpc"

	"google.golang.org/grpc"
)

var (
	file []byte = nil
	port        = flag.Int("port", 50051, "The server port")
)

// server is used to implement helloworld.GreeterServer.
type server struct {
	pb.UnimplementedGreeterServer
}

// SayHello implements helloworld.GreeterServer
func (s *server) SayHello(_ context.Context, in *pb.HelloRequest) (*pb.HelloReply, error) {
	log.Printf("Received: %v", in.GetName())
	return &pb.HelloReply{Message: "Hello " + in.GetName()}, nil
}

func (s *server) UploadFile(_ context.Context, in *pb.FileUploadRequest) (*pb.FileUploadReply, error) {
	log.Printf("Received: %v", in.GetFilename())

	file = in.GetContent()

	return &pb.FileUploadReply{Message: "File " + in.GetFilename() + " uploaded successfully"}, nil
}

func (s *server) DownloadFile(_ context.Context, in *pb.FileDownloadRequest) (*pb.FileDownloadReply, error) {
	log.Printf("Received: %v", in.GetFilename())

	if file == nil {
		return &pb.FileDownloadReply{Message: "No file uploaded"}, nil
	}

	return &pb.FileDownloadReply{Message: "File " + in.GetFilename() + " downloaded successfully", Content: file}, nil
}

func main() {
	flag.Parse()
	lis, err := net.Listen("tcp", fmt.Sprintf(":%d", *port))
	if err != nil {
		log.Fatalf("failed to listen: %v", err)
	}
	s := grpc.NewServer()
	pb.RegisterGreeterServer(s, &server{})
	log.Printf("server listening at %v", lis.Addr())
	if err := s.Serve(lis); err != nil {
		log.Fatalf("failed to serve: %v", err)
	}
}
