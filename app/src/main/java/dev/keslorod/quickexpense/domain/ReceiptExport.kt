package dev.keslorod.quickexpense.domain

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileInputStream

/**
 * Copies a receipt photo stored in the app's private storage into the device's shared Pictures
 * collection, so it shows up in the gallery and can be attached/backed up outside this app —
 * e.g. for a warranty claim. Returns true on success.
 *
 * Scoped storage (API 29+) needs no permission for this. Below that, MediaStore inserts
 * generally require WRITE_EXTERNAL_STORAGE, which this app doesn't request — export will just
 * fail gracefully (return false) on those older versions rather than crash.
 */
fun exportReceiptToGallery(context: Context, file: File): Boolean {
    if (!file.exists()) return false
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/QuickExpense")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val wrote = resolver.openOutputStream(uri)?.use { out ->
            FileInputStream(file).use { input -> input.copyTo(out) }
            true
        } ?: false
        if (!wrote) {
            resolver.delete(uri, null, null)
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (e: Exception) {
        false
    }
}
