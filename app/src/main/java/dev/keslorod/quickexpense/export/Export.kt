package dev.keslorod.quickexpense.export

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

fun enqueueExport(context: Context, from: Long, to: Long, includePhotos: Boolean = false) {
    val data = Data.Builder()
        .putLong("from", from)
        .putLong("to", to)
        .putBoolean("includePhotos", includePhotos)
        .build()

    val req = OneTimeWorkRequestBuilder<ExportWorker>()
        .setInputData(data)
        .build()

    WorkManager.getInstance(context).enqueue(req)
}