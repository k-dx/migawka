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

    override suspend fun getImages(limit: Int, imagesBefore: Instant?): List<LocalImage> {
        // Use withContext to ensure this IO-heavy operation runs on a background thread.
        return withContext(Dispatchers.IO) {
            val localImages = mutableListOf<LocalImage>()
            val selection = imagesBefore?.let { "${MediaStore.Images.Media.DATE_ADDED} < ?" }
            val selectionArgs = imagesBefore?.let { arrayOf(it.epochSecond.toString()) }

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
                    }
                }
            }
            localImages
        }
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