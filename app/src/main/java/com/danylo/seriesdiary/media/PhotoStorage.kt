package com.danylo.seriesdiary.media

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Робота з файлами фото у внутрішньому сховищі застосунку (filesDir/photos).
 * У Room зберігається лише шлях, а не бінарний вміст.
 */
object PhotoStorage {

    private const val PHOTOS_DIR = "photos"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    /**
     * Створює порожній файл .jpg у filesDir/photos з унікальним іменем за timestamp.
     * Камера запише до нього кадр через FileProvider URI.
     */
    fun createPhotoFile(context: Context): File {
        val dir = File(context.filesDir, PHOTOS_DIR).apply {
            if (!exists()) mkdirs()
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "series_$timestamp.jpg")
    }

    /**
     * URI, доступний для системного додатку камери через FileProvider.
     * Authority має співпадати з provider у AndroidManifest.xml.
     */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(
            context,
            context.packageName + FILE_PROVIDER_SUFFIX,
            file
        )
}
