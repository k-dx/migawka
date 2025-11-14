package xyz.jdubiel.migawka

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.time.Instant

class MediaStoreImageProvider(
    private val contentResolver: ContentResolver
) : LocalImageProvider {

    // TODO: change String to Sha256 (needs proper comparison for Sha256)
    private var sha256ToUri: MutableMap<String, Uri> = mutableMapOf()

    override suspend fun getImages(limit: Int, imagesBefore: Instant?): List<LocalImage> {
        // Use withContext to ensure this IO-heavy operation runs on a background thread.
        return withContext(Dispatchers.IO) {
            val localImages = mutableListOf<LocalImage>()
            val selection = imagesBefore?.let { "${MediaStore.Images.Media.DATE_ADDED} < ?" }
            val selectionArgs = imagesBefore?.let { arrayOf(it.epochSecond.toString()) }

            // TODO: make the dates right!
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DATE_ADDED),
                selection,
                selectionArgs,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)

                while (cursor.moveToNext() && localImages.size < limit) {
                    val id = cursor.getLong(idColumn)
                    val date = Instant.ofEpochSecond(cursor.getLong(dateColumn))
                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    // Compute SHA256 hash
                    // This is an I/O operation and can be slow. It's correctly placed within withContext(Dispatchers.IO).
                    val sha256 = computeSha256(contentUri)
                    sha256?.let {
                        localImages.add(LocalImage(contentUri, date, it))
                        sha256ToUri[it.toHex()] = contentUri
                        Log.d(TAG, "calculated sha256 of uri $contentUri: $it")
                    }
                }
            }
            localImages
        }
    }

    override suspend fun getImage(id: Sha256): LocalImage {
        val idString = id.toHex()
        Log.d(TAG, "MediaStoreImageProvider, getImage(${id.toHex()})")
//        throw Exception("Not implemented")
        if (!sha256ToUri.containsKey(idString)) {
            Log.e("MediaStoreImageProvider.getImage", "No image with SHA-256 $id found")
            throw IllegalArgumentException("No image with SHA-256 $id found")
        }
        Log.d(TAG, "MediaStoreImageProvider, getImage, accessing sha256ToUri")
        val contentUri = sha256ToUri[idString]!!
        Log.d(TAG, "MediaStoreImageProvider, getImage, contentUri = $contentUri")
        val date = queryDateTaken(contentUri); // TODO: should this happen in a coroutine?
        if (date == null) {
            Log.e("MediaStoreImageProvider.getImage", "No date found for image with SHA-256 $id")
            throw IllegalArgumentException("No date found for image with SHA-256 $id")
        }
        Log.d(TAG, "MediaStoreImageProvider, getImage, date = $date")
        return LocalImage(contentUri = contentUri, date = date, sha256 = id)
    }


    private fun queryDateTaken(uri: Uri): Instant? {
        val projection = arrayOf(MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.DATE_ADDED)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                // DATE_TAKEN is milliseconds since epoch (nullable)
//                val dateTakenIdx = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
//                val dateTaken = if (!cursor.isNull(dateTakenIdx)) cursor.getLong(dateTakenIdx) else null
//                if (dateTaken != null && dateTaken > 0L) return Instant.fromEpochSeconds(dateTaken)
//
//                // fallback: DATE_MODIFIED is seconds since epoch -> convert to millis
//                val modIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
//                if (!cursor.isNull(modIdx)) {
//                    val mod = cursor.getLong(modIdx)
//                    if (mod > 0L) return kotlin.time.Instant(mod * 1000L)
//                }

                // fallback: DATE_ADDED is seconds since epoch -> convert to millis
                val addedIdx = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                if (!cursor.isNull(addedIdx)) {
                    val added = cursor.getLong(addedIdx)
                    if (added > 0L) return Instant.ofEpochSecond(added)
                }
            }
        }
        return null
    }

    private fun computeSha256(uri: Uri): Sha256? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                Sha256.of(digest.digest()) // Assumes Sha256 class wraps a ByteArray
            }
        } catch (e: Exception) {
            Log.e("MediaStoreImageProvider", "Failed to compute SHA-256 for $uri", e)
            null
        }
    }
}