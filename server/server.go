package main

import (
	"context"
	"io"
	"os"
	"path/filepath"
	"strconv"
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

func (s *server) GetFileList(_ context.Context, in *pb.GetFileListRequest) (*pb.GetFileListResponse, error) {
	log.Info().
		Str("path", in.GetPath()).
		Msg("GetFileList")

	requestedRelativePath := in.GetPath()

	mediaDir := s.mediaStore.GetMediaDirectory()
	thumbnailDir := s.mediaStore.GetThumbnailDirectory()

	requestedAbsPath := filepath.Join(mediaDir, requestedRelativePath)

	// check that requestedRelativePath is inside media store directory
	if ok, err := IsPathInsideBase(mediaDir, requestedAbsPath); err != nil || !ok {
		log.Warn().
			Str("path", requestedRelativePath).
			Msg("Requested path is outside media directory")
		status := pb.NewStatus(400, "Requested path is outside media directory")
		return pb.NewGetFileListResponse(nil, status), nil
	}

	dirs, err := GetDirsInDir(mediaDir, thumbnailDir, requestedRelativePath)
	if err != nil {
		log.Error().
			Err(err).
			Str("path", requestedRelativePath).
			Msg("Failed to get directories in path")
		status := pb.NewStatus(500, "Failed to get directories in path")
		return pb.NewGetFileListResponse(nil, status), nil
	}

	mediaEntries, mediaEntriesFilenames, err := s.mediaStore.GetTimelineEntriesByPath(requestedRelativePath)
	if err != nil {
		log.Error().
			Err(err).
			Str("path", requestedRelativePath).
			Msg("Failed to get media entries for path")
		status := pb.NewStatus(500, "Failed to get media entries for path")
		return pb.NewGetFileListResponse(nil, status), nil
	}

	var entries []*pb.DirectoryEntry
	for _, dir := range dirs {
		entries = append(entries, &pb.DirectoryEntry{
			Name: dir.Name,
			Type: pb.DirectoryEntry_DIRECTORY,
		})
	}

	// convert to gRPC TimelineEntry type
	for i, mediaEntry := range mediaEntries {
		creationTime, err := s.mediaStore.GetCreationTimeOfMediaItem(mediaEntry.ID)
		if err != nil {
			log.Error().Err(err).Msg("Failed to get creation time of media item")
			continue
		}

		entries = append(entries, &pb.DirectoryEntry{
			Name:  mediaEntriesFilenames[i],
			Type:  pb.DirectoryEntry_MEDIA,
			Media: toPbTimelineEntry(mediaEntry.ID, creationTime),
		})
	}

	return pb.NewGetFileListResponse(entries, pb.NewStatus(200, "")), nil
}

func (s *server) GetTimelineEntries(_ context.Context, in *pb.TimelineEntriesRequest) (*pb.TimelineEntriesResponse, error) {
	log.Info().Msg("GetTimelineEntries")

	// retrieve thumbnails from media store
	entries, err := s.mediaStore.GetTimelineEntries()
	if err != nil {
		log.Error().Err(err).Msg("Failed to get entries from media store")
		status := pb.NewStatus(500, "Failed to get entries from media store")
		return pb.NewTimelineEntriesResponse(nil, status), nil
	}

	log.Debug().Int("count", len(entries)).Msg("Retrieved entries for timeline")

	// convert to gRPC TimelineEntry type
	var pbTimelineEntries []*pb.TimelineEntry
	for _, entry := range entries {
		creationTime, err := s.mediaStore.GetCreationTimeOfMediaItem(entry.ID)
		if err != nil {
			log.Error().Err(err).Msg("Failed to get creation time of media item")
			continue
		}

		pbTimelineEntries = append(pbTimelineEntries, toPbTimelineEntry(entry.ID, creationTime))
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

func (s *server) GetThumbnail(_ context.Context, in *pb.GetMediaItemRequest) (*pb.GetMediaItemResponse, error) {
	log.Info().Str("id", in.GetId()).Msg("GetThumbnail")

	// parse id
	id, err := s.mediaStore.GetHasher().HashFromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// get media item from media store
	thumbnail, err := s.mediaStore.GetThumbnailByID(id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get media item from media store")
		status := pb.NewStatus(500, "Failed to get media item from media store")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	creationDate, err := s.mediaStore.GetCreationTimeOfMediaItem(thumbnail.ID)
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Failed to get creation time of media item")
		status := pb.NewStatus(500, "Failed to get creation time of media item")
		return pb.NewGetMediaItemResponse(nil, status), nil
	}

	// convert to gRPC media item type
	pbMediaItem := &pb.MediaItem{
		Id:           thumbnail.ID.String(),
		CreationTime: creationDate.UTC().Format(time.RFC3339),
		Content:      thumbnail.Content,
	}

	status := pb.NewStatus(200, "")

	return pb.NewGetMediaItemResponse(pbMediaItem, status), nil
}

func (s *server) GetFullMetadata(_ context.Context, in *pb.FullMetadataRequest) (*pb.FullMetadataReply, error) {
	log.Info().Str("id", in.GetId()).Msg("GetFullMetadata")

	// parse id
	id, err := s.mediaStore.GetHasher().HashFromString(in.GetId())
	if err != nil {
		log.Error().Err(err).Str("id", in.GetId()).Msg("Invalid ID format")
		status := pb.NewStatus(400, "Invalid ID format")
		return pb.NewFullMetadataReply(nil, status), nil
	}

	// get metadata from media store
	metadata, err := s.mediaStore.GetFullMetadata(id)
	if err != nil {
		log.Error().Err(err).
			Str("id", in.GetId()).
			Msg("Failed to get metadata from media store")
		status := pb.NewStatus(500, "Failed to get metadata from media store")
		return pb.NewFullMetadataReply(nil, status), nil
	}

	// convert to gRPC FullMetadataReply type
	values := make(map[int32]string)

	values[int32(pb.MetadataKey_ID)] = metadata.Metadata.ID.String()
	values[int32(pb.MetadataKey_Path)] = metadata.Metadata.Path
	values[int32(pb.MetadataKey_CreationDate)] = metadata.Metadata.CreationTime.String()

	m := GetStatusMap()

	for tag, pbKey := range m {
		val, ok := metadata.ExifValues[tag]
		if ok {
			values[int32(pbKey)] = val
		}
	}

	status := pb.NewStatus(200, "")

	return pb.NewFullMetadataReply(values, status), nil
}

func GetStatusMap() map[ExifTag]pb.MetadataKey {
	// Map ExifTag to pb.MetadataKey
	// UPDATE THIS IF YOU ADD MORE EXIF TAGS!!!
	m := map[ExifTag]pb.MetadataKey{
		DateTime:     pb.MetadataKey_Exif_DateTime,
		Make:         pb.MetadataKey_Exif_Make,
		Model:        pb.MetadataKey_Exif_Model,
		Orientation:  pb.MetadataKey_Exif_Orientation,
		FocalLength:  pb.MetadataKey_Exif_FocalLength,
		ExposureTime: pb.MetadataKey_Exif_ExposureTime,
		FNumber:      pb.MetadataKey_Exif_FNumber,
		ISO:          pb.MetadataKey_Exif_ISO,
		Flash:        pb.MetadataKey_Exif_Flash,
		WhiteBalance: pb.MetadataKey_Exif_WhiteBalance,
	}
	return m
}

func (s *server) UploadPhotos(stream pb.Migawka_UploadPhotosServer) error {
	log.Info().Msg("UploadPhotos")
	var file *os.File
	var fileName string

	// defer s.mediaStore.RefreshAfterUpload() // TODO

	for {
		// Receive the next message from the stream
		req, err := stream.Recv()

		// Check if the stream is finished
		if err == io.EOF {
			if file != nil {
				file.Close()
				file = nil
			}
			return stream.SendAndClose(&pb.UploadResponse{
				Status: pb.NewStatus(200, "OK"),
			})
		}
		if err != nil {
			return err
		}

		uploadsDir, err := s.mediaStore.GetUploadsDirectory()
		if (err) != nil {
			log.Error().Err(err).Msg("Failed to get uploads directory")
			return err
		}
		getPathGivenBasename := func(basename string) string {
			return filepath.Join(uploadsDir, basename+".jpg")
		}

		// Handle the "oneof" request types
		switch x := req.Request.(type) {
		case *pb.UploadRequest_Metadata:
			// Close previous file if multiple photos are sent in one stream
			if file != nil {
				file.Close()
				file = nil
			}

			creationDate := x.Metadata.CreationTime
			parsedTime, err := time.Parse(time.RFC3339, creationDate)
			if err != nil {
				// Fallback: use hash as filename
				fileName = "unknown_date_" + x.Metadata.Id

				log.Warn().Err(err).
					Str("creation_date (received)", creationDate).
					Msg("Invalid creation date format for file with filename: " + fileName)
			} else {
				fileName = parsedTime.Format("2006-01-02_15-04-05")
			}

			// To avoid overwriting files, add a counter if file already exists
			originalFileName := fileName
			counter := 1
			for {
				if _, err := os.Stat(getPathGivenBasename(fileName)); os.IsNotExist(err) {
					break
				}
				fileName = originalFileName + "_" + strconv.Itoa(counter)
				counter++
			}

			log.Debug().
				Str("creation_time (received)", creationDate).
				Str("hash (received)", x.Metadata.Id).
				Str("filename (computed)", getPathGivenBasename(fileName)).
				Msgf("Receiving file upload")

			f, err := os.Create(getPathGivenBasename(fileName))
			if err != nil {
				log.Error().Err(err).
					Str("filename", getPathGivenBasename(fileName)).
					Msg("Failed to create file")
				return err
			}
			file = f

		case *pb.UploadRequest_Chunk:
			if file == nil {
				log.Warn().Msg("Received chunk before metadata!")
				continue
			}
			// Write chunk directly to disk (Memory efficient)
			if _, err := file.Write(x.Chunk); err != nil {
				return err
			}
		}
	}
}
