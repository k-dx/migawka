FROM golang:alpine

RUN apk add --no-cache vips-dev build-base

WORKDIR /app

COPY . .

RUN go build -o main .

EXPOSE 50051
