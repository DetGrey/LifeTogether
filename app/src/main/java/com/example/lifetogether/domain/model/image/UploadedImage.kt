package com.example.lifetogether.domain.model.image

data class UploadedImage(
    val downloadUrl: String,
    val byteArray: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UploadedImage

        if (downloadUrl != other.downloadUrl) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = downloadUrl.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}
