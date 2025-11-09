# Server

Generating protobuf files

```sh
# run from root migawka directory
protoc \
--go_out=. --go_opt=paths=source_relative \
--go-grpc_out=. --go-grpc_opt=paths=source_relative \
grpc/grpc.proto
```