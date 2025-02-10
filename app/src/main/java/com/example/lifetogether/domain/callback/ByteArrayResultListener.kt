package com.example.lifetogether.domain.callback

sealed class ByteArrayResultListener {
    data class Success(val byteArray: ByteArray) : ByteArrayResultListener() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Success

            return byteArray.contentEquals(other.byteArray)
        }

        override fun hashCode(): Int {
            return byteArray.contentHashCode()
        }
    }

    data class Failure(val message: String) : ByteArrayResultListener()
}
