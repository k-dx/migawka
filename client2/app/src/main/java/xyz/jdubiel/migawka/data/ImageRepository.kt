package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.MediaStore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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

        val localEntries = localImages.map {
            TimelineEntryK.Local(contentUri = it.contentUri, id = it.hash, date = it.date)
        }

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

        return@coroutineScope EntriesResult(results, err)
    }

    suspend fun getRemoteOptimizedImage(id: Hash): GrpcResult<RemoteImage> {
        return remoteImageProvider.getOptimizedImage(id)
    }

    suspend fun getRemoteFullImage(id: Hash): RemoteFullImage {
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
}
