package xyz.jdubiel.migawka.data

import java.time.Instant

sealed class DirectoryEntryK { // K as in Kotlin
    abstract val name: String
    class Directory(
        override val name: String
    ) : DirectoryEntryK()
    class Image(
        override val name: String,
        val id: Hash,
        val date: Instant
    ) : DirectoryEntryK()
}


fun List<DirectoryEntryK>.sortedDirectoriesThenImagesByDateDesc(): List<DirectoryEntryK> {
    return this.sortedWith { a, b ->
        when {
            a is DirectoryEntryK.Directory && b is DirectoryEntryK.Directory -> a.name.compareTo(b.name)
            a is DirectoryEntryK.Directory && b is DirectoryEntryK.Image -> -1   // directory before image
            a is DirectoryEntryK.Image && b is DirectoryEntryK.Directory -> 1    // image after directory
            a is DirectoryEntryK.Image && b is DirectoryEntryK.Image -> {
                // newer images first (descending by date); if equal, fall back to name
                val cmp = b.date.compareTo(a.date)
                if (cmp != 0) cmp else a.name.compareTo(b.name)
            }
            else -> 0
        }
    }
}