package xyz.jdubiel.migawka.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.jdubiel.migawka.data.database.ILocalMediaRepository
import xyz.jdubiel.migawka.data.database.LocalMediaEntry
import xyz.jdubiel.migawka.data.network.GrpcResult
import xyz.jdubiel.migawka.hasher
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object LocalImageDataStoreKeys {
    val LAST_MODIFIED_GENERATION = intPreferencesKey("last_modified_generation")
    val DB_MEDIA_STORE_VERSION = stringPreferencesKey("db_media_store_version")
}

sealed interface IndexingState {
    object Idle : IndexingState
    data class Indexing(val processedCount: Int = 0, val totalCount: Int) : IndexingState
    data class Finished(val totalCount: Int) : IndexingState
    data class Error(val message: String) : IndexingState
}

class MediaStoreImageProvider(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val db: ILocalMediaRepository,
    scope: CoroutineScope,
    dataStore: DataStore<Preferences>
) : LocalImageProvider {

    // TODO: add logic to observe newly added photos, after initialization

    private val _indexingState = MutableStateFlow<IndexingState>(IndexingState.Idle)
    override val indexingState: StateFlow<IndexingState> = _indexingState.asStateFlow()

    val initializationJob = scope.launch(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        var lastKnownModifiedGeneration = prefs[LocalImageDataStoreKeys.LAST_MODIFIED_GENERATION] ?: -1
        var lastKnownMediaStoreVersion = prefs[LocalImageDataStoreKeys.DB_MEDIA_STORE_VERSION] ?: ""

        Log.d(TAG, "lastModifiedGeneration = $lastKnownModifiedGeneration")
        Log.d(TAG, "dbMediaStoreVersion = $lastKnownMediaStoreVersion")

        val currentMediaStoreVersion = MediaStore.getVersion(context)
        Log.d(TAG, "currentMediaStoreVersion = $currentMediaStoreVersion")

        val indexedModifiedGeneration = if (currentMediaStoreVersion != lastKnownMediaStoreVersion) {
            // do a full scan of the MediaStore
            Log.d(TAG, "full scan of MediaStore")

            db.deleteAll()
            indexMediaStore(-1)
        } else {
            Log.d(TAG, "partial scan of MediaStore")

            // do a partial scan of the MediaStore - detect new media via GENERATION_MODIFIED column
            indexMediaStore(lastKnownModifiedGeneration)
        }

        Log.d(TAG, "indexedModifiedGeneration = $indexedModifiedGeneration")

        dataStore.edit { settings ->
            settings[LocalImageDataStoreKeys.LAST_MODIFIED_GENERATION] = indexedModifiedGeneration
            settings[LocalImageDataStoreKeys.DB_MEDIA_STORE_VERSION] = currentMediaStoreVersion
        }
    }

    override suspend fun getImages(count: Int, imagesBefore: Instant): List<LocalImage> {
        initializationJob.join() // Wait for init to finish before returning data

        // Use withContext to ensure this IO-heavy operation runs on a background thread.
        return withContext(Dispatchers.IO) {

            /*
            To ensure that the images returned actually exist (e.g. have not been deleted after
            saving to database), query the MediaStore for the URIs that I want to return. If all of
            them are in MediaStore - great, else - repeat until no more images in database or we
            got `count` images to return.
             */

            val results = mutableListOf<LocalImage>()
            val deletedUris = mutableListOf<Uri>()
            while (results.size < count) {
                // query the database for appropriate entries
                // TODO: IDEA: it might be faster on average to query for e.g.
                // 2*count or count + X once than only querying for `count`
                // entries, to anticipate that some image might have been
                // deleted
                val dbEntries = db.getEntriesBeforeTimestamp(count, imagesBefore)

                // query the MediaStore to make sure they exist
                val mediaStoreEntries =
                    getEntriesFromMediaStore(dbEntries.map { Uri.parse(it.uri) })

                for (dbEntry in dbEntries) {
                    if (mediaStoreEntries.contains(Uri.parse(dbEntry.uri))) {
                        results.add(
                            LocalImage(
                                Uri.parse(dbEntry.uri),
                                dbEntry.date,
                                dbEntry.hash
                            )
                        )
                    } else {
                        deletedUris.add(Uri.parse(dbEntry.uri))
                    }
                }

                if (dbEntries.size < count) {
                    // there are no more entries in the database, so we can stop
                    break
                }
            }

            // update the database
            db.delete(deletedUris)

            results
        }
    }

    override suspend fun getEntries(): List<LocalImage> = getImages(Int.MAX_VALUE, Instant.now())

    private fun getEntriesFromMediaStore(uriList: List<Uri>): Set<Uri> {
        val contentResolver = context.contentResolver
        val results = mutableSetOf<Uri>()

        // Extract IDs from URIs to use in selection
        val ids = uriList.mapNotNull { ContentUris.parseId(it).toString() }
        if (ids.isEmpty()) return emptySet()

        // Create selection: "_id IN (?, ?, ...)"
        val placeholders = ids.joinToString { "?" }
        val selection = "${MediaStore.MediaColumns._ID} IN ($placeholders)"
        val selectionArgs = ids.toTypedArray() // this will substitute "?" in selection

        // Projection: Only request the ID column
        val projection = arrayOf(MediaStore.MediaColumns._ID)

        // Query external content (e.g., Images, Video, or Audio)
        contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                results.add(contentUri)
            }
        }
        return results
    }

    override suspend fun getImage(id: Hash): LocalImage {
        initializationJob.join() // Wait for init to finish before returning data

        // query the database
        val dbEntry = db.getByHash(id)
        if (dbEntry == null) {
            // TODO: probably returning null would be better
            throw NoSuchElementException("No image with given id=$id found in database")
        }

        // query the MediaStore
        val mediaStoreEntries =
            getEntriesFromMediaStore(listOf(Uri.parse(dbEntry.uri)))

        if (mediaStoreEntries.size != 1 || mediaStoreEntries.first() != Uri.parse(dbEntry.uri)) {
            // TODO: probably returning null would be better
            throw NoSuchElementException("No image with given hash=$id, uri=${dbEntry.uri} found in MediaStore, probably deleted")
        }

        return LocalImage(
            Uri.parse(dbEntry.uri),
            dbEntry.date,
            dbEntry.hash
        )
    }

    override fun extractExifMetadata(imageUri: Uri): GrpcResult<Map<MediaMetadata, String>> {
        val resolver = context.contentResolver
        val result = mutableMapOf<MediaMetadata, String>()
        var stream: InputStream? = null
        try {
            stream = resolver.openInputStream(imageUri)
                ?: return GrpcResult.Error("Failed to open input stream")
            val exif = ExifInterface(stream)

            // Basic tags
            exif.getAttribute(ExifInterface.TAG_DATETIME)
                ?.let { result[MediaMetadata.Exif_DateTime] = it }
            exif.getAttribute(ExifInterface.TAG_MAKE)
                ?.let { result[MediaMetadata.Exif_Make] = it }
            exif.getAttribute(ExifInterface.TAG_MODEL)
                ?.let { result[MediaMetadata.Exif_Model] = it }
            exif.getAttribute(ExifInterface.TAG_ORIENTATION)
                ?.let { result[MediaMetadata.Exif_Orientation] = it }
            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                ?.let { result[MediaMetadata.Exif_FocalLength] = it }
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                ?.let { result[MediaMetadata.Exif_ExposureTime] = it }
            exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                ?.let { result[MediaMetadata.Exif_FNumber] = it }
            exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                ?.let { result[MediaMetadata.Exif_ISO] = it }
            exif.getAttribute(ExifInterface.TAG_FLASH)
                ?.let { result[MediaMetadata.Exif_Flash] = it }
            exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
                ?.let { result[MediaMetadata.Exif_WhiteBalance] = it }

            return GrpcResult.Success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse EXIF data")
            e.printStackTrace()
            return GrpcResult.Error("Failed to parse EXIF data: ${e.message}", e)
        } finally {
            try {
                stream?.close()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * (Re)indexes the MediaStore for images and inserts them into the database.
     *
     * @param fromGenerationModified The generation modified to start from. Can be -1 to index everything.
     * @return highest GENERATION_MODIFIED.
     */
    private suspend fun indexMediaStore(fromGenerationModified: Int): Int {
        val entries: MutableList<LocalImage> = mutableListOf()
        var highestGenerationModified = fromGenerationModified

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.GENERATION_MODIFIED
        )
        val selection = "${MediaStore.MediaColumns.GENERATION_MODIFIED} > ?"
        val selectionArgs = arrayOf(fromGenerationModified.toString())


        withContext(Dispatchers.IO) {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val generationModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.GENERATION_MODIFIED)

                val totalCount = cursor.count
                _indexingState.update { IndexingState.Indexing(totalCount = totalCount) }

                var processedCount = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri =
                        ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val generationModified = cursor.getInt(generationModifiedColumn)

                    // queryDateTaken should not throw here, since we are passing a valid uri
                    val date = queryDateTaken(uri)
                    val hash = computeHash(uri)

                    if (hash == null) {
                        Log.e(TAG, "Failed to compute hash for $uri")
                        continue
                    }

                    entries.add(LocalImage(uri, date, hash))
                    highestGenerationModified = maxOf(highestGenerationModified, generationModified)

                    // Log.d(HERETAG, "uri $uri has date $date and hash $hash")

                    // Update progress
                    processedCount++
                    _indexingState.update {
                        when (it) {
                            is IndexingState.Indexing -> it.copy(processedCount = processedCount)
                            else -> IndexingState.Indexing(
                                processedCount = processedCount,
                                totalCount = totalCount
                            )
                        }
                    }
                }
            }


            // batch insert into database

            try {
                db.insertEntries(entries.map {
                    LocalMediaEntry(
                        it.contentUri.toString(),
                        it.hash,
                        it.date
                    )
                })
                Log.d(TAG, "indexed ${entries.size} images")

                _indexingState.update { IndexingState.Finished(totalCount = entries.size) }
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting entries", e)
                _indexingState.update {
                    IndexingState.Error(e.message ?: "Unknown error")
                }
            }
        }

        return highestGenerationModified
    }


    /**
     * Extracts the best date from the data available in MediaStore.
     */
    private fun queryDateTaken(uri: Uri): Instant {
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
                        val exif = ExifInterface(input)
                        exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { s ->
                            // parse "yyyy:MM:dd HH:mm:ss" optionally with offset tag
                            dateExif = try {
                                val fmt = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss")
                                val ldt = LocalDateTime.parse(s, fmt)
                                ldt.atZone(ZoneId.systemDefault()).toInstant()
                            } catch (_: Exception) { null }
                        }
                    }
                } catch (_: Exception) {
                    /* ignore unreadable EXIF */
                    Log.d(TAG, "failed to parse EXIF for $uri")
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
                Log.d(TAG, "using DATE_TAKEN")
                Instant.ofEpochMilli(dateTakenMilliSec)
            }
            dateAddedSec != null -> {
                Log.d(TAG, "using DATE_ADDED")
                Instant.ofEpochSecond(dateAddedSec)
            }
            else -> {
                Log.w(TAG, "using current time :(")
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

    companion object {
        private const val TAG = "MediaStoreImageProvider"
    }
}