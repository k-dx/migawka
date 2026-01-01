package xyz.jdubiel.migawka.data.database

import android.net.Uri
import androidx.room.TypeConverter
import xyz.jdubiel.migawka.data.Hash
import xyz.jdubiel.migawka.hasher
import java.time.Instant

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun uriToString(uri: Uri?): String? {
        return uri?.toString()
    }

    @TypeConverter
    fun hashToString(hash: Hash?): String? {
        return hash?.toString()
    }

    @TypeConverter
    fun hashFromString(hash: String?): Hash? {
        return hash?.let { hasher.fromString(it) }
    }
}
