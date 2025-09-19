package dev.keslorod.quickexpense.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.keslorod.quickexpense.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext as App

        val from = inputData.getLong("from", 0L)
        val to = inputData.getLong("to", System.currentTimeMillis())
        // val includePhotos = inputData.getBoolean("includePhotos", false) // на будущее

        // подготовим папку
        val exportDir = File(applicationContext.cacheDir, "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

        // 1) CSV
        val csvFile = File(exportDir, "quickexpense_$stamp.csv")
        csvFile.outputStream().buffered().writer(Charsets.UTF_8).use { w ->
            w.appendLine("id,created_at,amount_cents,currency,source_id,category_id")
            val items = app.db.expenses().expensesInRange(from, to)
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            items.forEach { e ->
                val ts = fmt.format(java.util.Date(e.createdAt))
                w.appendLine("${e.id},$ts,${e.amount},${e.currency},${e.sourceId},${e.categoryId}")
            }
        }

        // 2) ZIP (пока только CSV внутрь; фото чеков добавим позже)
        val zipFile = File(exportDir, "quickexpense_$stamp.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry(csvFile.name))
            csvFile.inputStream().use { it.copyTo(zos) }
            zos.closeEntry()
            // если будут фото: положим их как /receipts/<id>.jpg
        }

        // 3) share
        val uri = FileProvider.getUriForFile(
            applicationContext,
            "${applicationContext.packageName}.fileprovider",
            zipFile
        )
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_SUBJECT, "QuickExpense export $stamp")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        applicationContext.startActivity(Intent.createChooser(share, "Отправить экспорт"))

        Result.success()
    }
}