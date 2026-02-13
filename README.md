# Migawka

An application for photo browsing and synchronization.

<div>
    <img src="./assets/screenshots/gallery.png" style="width: 24%"/>
    <img src="./assets/screenshots/gallery_settings.png" style="width: 24%"/>
    <img src="./assets/screenshots/folders.png" style="width: 24%"/>
    <img src="./assets/screenshots/single_photo.png" style="width: 24%"/>
<div>

## Getting started

Get up and running via Docker Compose, without TLS enabled. Tested on Linux and Windows.

### Server

1. Make sure you have `docker compose` command available in your shell. The easiest way is to install [Docker Desktop](https://docs.docker.com/get-started/get-docker/)
2. Download or clone the repository.
3. In [`server/docker-compose.yaml`](./server/docker-compose.yaml)
    1. Replace `/path/to/your/media/directory` with path to the directory containing your photos, e.g. `/home/myuser/photos` or `C:\Users\myuser\photos`.
    2. Similarly replace `/path/where/to/store/the/database_file`
4. In the `server` directory run `docker compose up`. This will build the server and start it. **It can take a few minutes.** After starting, the server should begin generating thumbnails.

### Client

1. Download the `.apk` file [from releases page](https://github.com/k-dx/migawka/releases) and install it on your phone.
2. Go to *Menu* > *Settings*.
    1. Fill the server address (IP or domain).
    2. Disable TLS at the bottom of the *Settings* page.
    3. Exit the settings.
3. Check the *Folders* tab. It should show photos and directories on the server.
4. Enjoy using the app!

---

### Client

See [client2/README.md](client2/README.md).

### Server

See [server/README.md](server/README.md).

### Dev installation

* [Golang v1.25.3](https://go.dev/doc/install)
* [protoc v32.1](https://protobuf.dev/installation/)
* [protoc go plugin (v1.36.10)](https://grpc.io/docs/languages/go/quickstart/)

