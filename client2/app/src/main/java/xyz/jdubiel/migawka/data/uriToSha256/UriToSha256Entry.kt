package xyz.jdubiel.migawka.data.uriToSha256

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uriToSha256")
data class UriToSha256Entry(
    @PrimaryKey
    @ColumnInfo(name = "uri") val uri: String,

    @ColumnInfo(name = "sha256") val sha256: String
)
