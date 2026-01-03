package xyz.jdubiel.migawka.data

typealias FullMediaMetadata = Map<MediaMetadata, String>

enum class MediaMetadata {
    ID,
    Path,
    CreationDate,
    Exif_DateTime,
    Exif_Make,
    Exif_Model,
    Exif_Orientation,
    Exif_FocalLength,
    Exif_ExposureTime,
    Exif_FNumber,
    Exif_ISO,
    Exif_Flash,
    Exif_WhiteBalance,
}
