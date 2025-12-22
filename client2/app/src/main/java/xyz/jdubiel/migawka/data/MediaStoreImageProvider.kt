package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.content.ContentUris
import android.media.ExifInterface
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.hasher
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val HERETAG = "MediaStoreImageProvider"

class MediaStoreImageProvider(
    private val contentResolver: ContentResolver
) : LocalImageProvider {

    private var hashToUri: MutableMap<Hash, Uri> = mutableMapOf()

    // TODO: change this to be saved in a database provided by Android
    //       (or at least be sorted by Instant)
    private var uriToDate: MutableList<Pair<Uri, Instant>> = mutableListOf()

    override suspend fun getImages(count: Int, imagesBefore: Instant?): List<LocalImage> {
        // Use withContext to ensure this IO-heavy operation runs on a background thread.
        return withContext(Dispatchers.IO) {
            if (uriToDate.isEmpty()) {
                // initialize on first getImages call

                contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Images.Media._ID),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idColumn)
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                        // queryDateTaken should not throw here, since we are passing a valid uri
                        val date = queryDateTaken(uri)

                        uriToDate.add(uri to date)
                        Log.d(HERETAG, "uri $uri has date $date")
                    }
                }
                uriToDate.sortByDescending { it.second }
            }

            val uriToDateBefore = uriToDate.filter { it.second < imagesBefore ?: Instant.now() }

            val localImages = mutableListOf<LocalImage>()
            for (i in 0 until minOf(count, uriToDateBefore.size)) {
                val uri = uriToDateBefore[i].first
                val date = uriToDateBefore[i].second
                val id = computeHash(uri)
                id?.let {
                    localImages.add(LocalImage(uri, date, it))
                    hashToUri[it] = uri
//                    Log.d(TAG, "calculated hash of uri $uri: $it")
                }
            }
            localImages
        }
    }

    override suspend fun getImage(id: Hash): LocalImage {
        Log.d(TAG, "MediaStoreImageProvider, getImage(${id.toHex()})")
        if (!hashToUri.containsKey(id)) {
            Log.e("MediaStoreImageProvider.getImage", "No image with hash $id found")
            throw IllegalArgumentException("No image with hash $id found")
        }
        Log.d(TAG, "MediaStoreImageProvider, getImage, accessing hashToUri")
        val contentUri = hashToUri[id]!!
        Log.d(TAG, "MediaStoreImageProvider, getImage, contentUri = $contentUri")
        try {
            val date = queryDateTaken(contentUri); // TODO: should this happen in a coroutine?
            Log.d(TAG, "MediaStoreImageProvider, getImage, date = $date")
            return LocalImage(contentUri = contentUri, date = date, hash = id)

        } catch (e: NoSuchElementException) {
            Log.e("MediaStoreImageProvider.getImage", "No date found for image with hash $id")
            throw NoSuchElementException("No image with given id found in MediaStore")
        }
    }


    // TODO: now
    private fun queryDateTaken(uri: Uri): Instant {
        val index = uriToDate.find { it.first == uri }
        if (index != null) {
            return index.second
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN
        )
        var dateAddedSec: Long? = null
        var dateTakenMilliSec: Long? = null
        var dateExif: Instant? = null

        contentResolver.query(
            uri, projection, null, null, null
        )?.use { cursor ->
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)

            if (cursor.moveToFirst()) {
                dateAddedSec = cursor.getLong(dateAddedColumn).takeIf { it > 0 }
                dateTakenMilliSec = cursor.getLong(dateTakenColumn).takeIf { it > 0 }

                // parse EXIF (may be slow — TODO: consider dispatching to IO dispatcher)
                try {
                    contentResolver.openInputStream(uri)?.use { input ->
                        // TODO: handle timezone from EXIF?
                        val exif = androidx.exifinterface.media.ExifInterface(input)
                        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { s ->
                            // parse "yyyy:MM:dd HH:mm:ss" optionally with offset tag
                            dateExif = try {
                                val fmt = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                                val ldt = LocalDateTime.parse(s, fmt)
                                ldt.atZone(ZoneId.systemDefault()).toInstant()
                            } catch (e: Exception) { null }
                        }
                    }
                } catch (_: Exception) {
                    /* ignore unreadable EXIF */
                    Log.d(HERETAG, "failed to parse EXIF for $uri")
                }

            } else {
                throw NoSuchElementException("No media found for URI: $uri")
            }
        }

        val date = when {
            dateExif != null -> {
                //Log.d(HERETAG, "using exif")
                dateExif
            }
            dateTakenMilliSec != null -> {
                Log.d(HERETAG, "using DATE_TAKEN")
                Instant.ofEpochMilli(dateTakenMilliSec)
            }
            dateAddedSec != null -> {
                Log.d(HERETAG, "using DATE_ADDED")
                Instant.ofEpochSecond(dateAddedSec)
            }
            else -> {
                Log.w(HERETAG, "using current time :(")
                Instant.now() // fallback to current time
            }
        }
        return date
    }

    private fun computeHash(uri: Uri): Hash? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val digest = hasher.getInstance()
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }

                hasher.fromBytes(digest.digest())
            }
        } catch (e: Exception) {
            Log.e("MediaStoreImageProvider", "Failed to compute hash for $uri", e)
            null
        }
    }
}