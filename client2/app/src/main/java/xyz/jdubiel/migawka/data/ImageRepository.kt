package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.data.network.GrpcResult
import java.io.File

/**
 * Represents a result of fetching entries. `err` can be non-null even if `entries` are non-empty,
 * because e.g. only local images were available.
 */
data class EntriesResult(val entries: List<TimelineEntryK>, val err: GrpcResult.Error?)

class ImageRepository(
    private val contentResolver: ContentResolver,
    private val remoteImageProvider: RemoteImageProvider,
    private val localImageProvider: LocalImageProvider,
) {

    val localMediaIndexingState: StateFlow<IndexingState> = localImageProvider.indexingState
    private var entries: Map<Hash, TimelineEntryK> = mapOf()
    /**
     * @return entries that are both local and remote, unique by hash.
     */
    suspend fun getEntries(): EntriesResult = coroutineScope {
        val localDeferred = async { localImageProvider.getEntries() }
        val remoteDeferred = async { remoteImageProvider.getEntries() }

        val localImages = localDeferred.await()
        val remoteResult = remoteDeferred.await()

        val remoteEntries = when(remoteResult) {
            is GrpcResult.Success -> remoteResult.data
            is GrpcResult.Error -> emptyList()
        }

        // mark local entries that are present on the remote
        val remoteIds: Set<Hash> = remoteEntries.map { it.id }.toSet()
        val localEntries = localImages.map {
            TimelineEntryK.Local(
                contentUri = it.contentUri,
                id = it.hash,
                date = it.date,
                onRemote = remoteIds.contains(it.hash)
            )
        }
        Log.d(TAG, "getEntries (before merge): local: ${localEntries.size}, remote: ${remoteEntries.size}")

        val results: MutableList<TimelineEntryK> = mutableListOf()

        // remove remote entries that we have locally
        val localIds: Set<Hash> = localEntries.map { it.id }.toSet()
        val remoteOnlyEntries = remoteEntries.filter { !localIds.contains(it.id) }

        // combine local and remote results chronologically
        var localEntriesIndex = 0
        var remoteOnlyEntriesIndex = 0
        while (localEntriesIndex < localEntries.size || remoteOnlyEntriesIndex < remoteOnlyEntries.size) {
            if (localEntriesIndex < localEntries.size && remoteOnlyEntriesIndex < remoteOnlyEntries.size) {
                if (localEntries[localEntriesIndex].date > remoteOnlyEntries[remoteOnlyEntriesIndex].date) {
                    results.add(localEntries[localEntriesIndex++])
                } else {
                    results.add(remoteOnlyEntries[remoteOnlyEntriesIndex++])
                }
            } else if (localEntriesIndex < localEntries.size) {
                results.add(localEntries[localEntriesIndex++])
            } else if (remoteOnlyEntriesIndex < remoteOnlyEntries.size) {
                results.add(remoteOnlyEntries[remoteOnlyEntriesIndex++])
            }
        }

        val err = when(remoteResult) {
            is GrpcResult.Success -> null
            is GrpcResult.Error -> remoteResult
        }

        entries = results.map { it.id to it }.toMap()

        return@coroutineScope EntriesResult(results, err)
    }

    suspend fun getRemoteOptimizedImage(id: Hash): GrpcResult<RemoteImage> {
        return remoteImageProvider.getOptimizedImage(id)
    }

    suspend fun getRemoteFullImage(id: Hash): GrpcResult<RemoteFullImage> {
        return remoteImageProvider.getFullImage(id)
    }

    fun saveImageToGallery(img: RemoteFullImage) {
        val file = File(img.path)
        val filename = file.name
        val path = "Pictures/Migawka/${file.parent}"
        // Prepare ContentValues for MediaStore
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            // TODO: It would be best to set DATE_MODIFIED (ie the modified date on the file)
            // when downloading a photo that does not have EXIF metadata, as modified date
            // is the fallback on the server, but this doesn't seem to work - maybe needs
            // stronger permissions.
            // This is problematic since photos can "move" to the top of the timeline after
            // download.
            // put(MediaStore.Images.Media.DATE_MODIFIED, date_modified.epochSecond)
            put(MediaStore.Images.Media.RELATIVE_PATH, path)
            put(MediaStore.Images.Media.IS_PENDING, true)
        }

        // Insert to MediaStore
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ) ?: throw Exception("Failed to insert image into MediaStore")

        // Write bytes to the URI
        contentResolver.openOutputStream(uri)?.use { output ->
            output.write(img.bytes)
        }

        // Mark as complete
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, false)
        contentResolver.update(uri, values, null, null)
    }

    suspend fun getMetadata(context: Context, id: Hash): GrpcResult<FullMediaMetadata> {
        return when (val entry = entries[id]) {
            is TimelineEntryK.Local -> {
                // annotate with ID if successful
                when (val data = localImageProvider.extractExifMetadata(entry.contentUri)) {
                    is GrpcResult.Success -> {
                        val metadata = data.data.toMutableMap()
                        metadata[MediaMetadata.ID] = entry.id.toString()
                        // TODO: format date better
                        metadata[MediaMetadata.CreationDate] = entry.date.toString()

                        if (entry.onRemote) {
                            // TODO: this should not put actual strings into the metadata map,
                            // just enums. Strings for display should be done in UI layer.
                            metadata[MediaMetadata.IsLocalIsRemote] =
                                context.getString(R.string.on_device_and_server)
                        }
                        else {
                            metadata[MediaMetadata.IsLocalIsRemote] =
                                context.getString(R.string.on_device)
                        }
                        GrpcResult.Success(metadata)
                    }
                    is GrpcResult.Error -> {
                        data
                    }
                }
            }
            is TimelineEntryK.Remote -> {
                when (val data = remoteImageProvider.getMetadata(entry.id)) {
                    is GrpcResult.Success -> {
                        val metadata = data.data.toMutableMap()
                        metadata[MediaMetadata.ID] = entry.id.toString()
                        metadata[MediaMetadata.IsLocalIsRemote] =
                            context.getString(R.string.on_server)
                        GrpcResult.Success(metadata)
                    }

                    is GrpcResult.Error -> {
                        data
                    }
                }

            }
            null -> {
                GrpcResult.Error("Entry not found")
            }
        }
    }

    companion object {
        private const val TAG = "ImageRepository"
    }
}
