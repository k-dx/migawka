package xyz.jdubiel.migawka.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "localMedia")
data class LocalMediaEntry(
    @PrimaryKey
    @ColumnInfo(name = "uri") val uri: String,

    @ColumnInfo(name = "hash") val hash: String

    // @ColumnInfo(name = "date") val date: Instant
)

