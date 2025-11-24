# Server

## Prerequisites

* libvips library installed. For Ubuntu install `libvips-dev` package. [Installation for other platforms](https://www.libvips.org/install.html)

## Generating protobuf files

```sh
# run from root migawka directory
protoc \
--go_out=. --go_opt=paths=source_relative \
--go-grpc_out=. --go-grpc_opt=paths=source_relative \
grpc/grpc.proto
```

## Running the server

```sh
# replace mediadir with your photo directory
go run . -mediadir="$HOME/migawka_media2" -loglevel="debug"
```