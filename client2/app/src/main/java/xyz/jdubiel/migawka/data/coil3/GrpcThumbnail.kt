package xyz.jdubiel.migawka.data.coil3

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import okio.Buffer
import xyz.jdubiel.migawka.data.RemoteImageProvider
import xyz.jdubiel.migawka.hasher

// Define a marker type for requests that should use gRPC
// TODO: this should use a Hash, not a String
data class GrpcThumbnail(val id: String)

// Implement ImageFetcher
class GrpcFetcher(
    private val model: GrpcThumbnail,
    private val options: Options,
    private val remoteImageProvider: RemoteImageProvider
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        // Download image bytes via gRPC
        val response = remoteImageProvider.getThumbnailImage(hasher.fromHex(model.id))

        // Convert bytes to a Buffer (Okio)
        val buffer = Buffer().apply { write(response.bytes) }

        // Return the result
        return SourceFetchResult(
            source = ImageSource(
                source = buffer,
                fileSystem = options.fileSystem
            ),
            mimeType = "image/jpeg",
            dataSource = DataSource.NETWORK
        )
    }

    class Factory(private val remoteImageProvider: RemoteImageProvider) : Fetcher.Factory<GrpcThumbnail> {
        override fun create(data: GrpcThumbnail, options: Options, imageLoader: ImageLoader): Fetcher {
            return GrpcFetcher(data, options, remoteImageProvider)
        }
    }
}

// Keyer defines keys for caching the model
class GrpcKeyer : Keyer<GrpcThumbnail> {
    override fun key(data: GrpcThumbnail, options: Options): String {
        return "grpc_image_${data.id}"
    }
}