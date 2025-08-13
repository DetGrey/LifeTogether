package com.example.lifetogether.domain.callback

import java.io.File

sealed class TempFileDownloadResult {
    data class Success(val downloadedFile: File) : TempFileDownloadResult()
    data class Failure(val message: String) : TempFileDownloadResult()
}
