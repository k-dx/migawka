# Server

## Running in Docker

First, adjust the `mediadir` volume path in `docker-compose.yaml` to point to your photo directory. You can also change other command line arguments (e.g. loglevel) as needed.

Then run:
```sh
docker compose up
```

## Running locally, without Docker


### Prerequisites

* libvips library installed. For Ubuntu install `libvips-dev` package. [Installation for other platforms](https://www.libvips.org/install.html)

### Generating protobuf files

```sh
# run from root migawka directory
protoc \
--go_out=./server --go_opt=paths=source_relative \
--go-grpc_out=./server --go-grpc_opt=paths=source_relative \
grpc/grpc.proto
```

### Running the server

```sh
# replace mediadir with your photo directory
go run . \
-mediadir="$HOME/migawka_media2" \
-loglevel="debug" \
-tls-private-key="../certs/migawka_server.key.pem" \
-tls-cert="../certs/migawka_server.crt"
```