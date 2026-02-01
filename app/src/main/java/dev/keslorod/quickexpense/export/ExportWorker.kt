package dev.keslorod.quickexpense.export

import android.content.Context
import android.util.Log
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dev.keslorod.quickexpense.App
import dev.keslorod.quickexpense.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportWorker(ctx: Context, params: WorkerParameters): CoroutineWorker(ctx, params) {
    override suspend fun doWork() = withContext(Dispatchers.IO) {
        return@withContext try {
            val app = applicationContext as App

            val from = inputData.getLong("from", 0L)
            val to = inputData.getLong("to", System.currentTimeMillis())

            // папка
            val exportDir = File(applicationContext.cacheDir, "exports").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())

            // 1) CSV
            val csvFile = File(exportDir, "quickexpense_$stamp.csv")
            try {
                csvFile.outputStream().buffered().writer(Charsets.UTF_8).use { w ->
                    w.appendLine("id,created_at,amount_cents,currency,source_name,category_name")
                    val items = app.db.expenses().expensesInRangeWithNames(from, to)
                    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    items.forEach { e ->
                        val ts = fmt.format(java.util.Date(e.createdAt))
                        val sourceName = e.sourceName ?: "Unknown"
                        val categoryName = e.categoryName ?: "Unknown"
                        w.appendLine(
                            listOf(
                                e.id,
                                ts,
                                e.amount.toString(),
                                e.currency,
                                sourceName,
                                categoryName
                            ).joinToString(",") { it.toSafeCsv() }
                        )
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.d("Export.worker.create", e.message.toString())
                return@withContext Result.failure()
            }

            if (!csvFile.exists()) {
                return@withContext Result.failure()
            }

            // 2) ZIP
            val zipFile = File(exportDir, "quickexpense_$stamp.zip")
            try {
                ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    val entry = ZipEntry(csvFile.name)
                    zos.putNextEntry(entry)
                    csvFile.inputStream().buffered().use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                    zos.finish()
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.d("Export.worker.save", e.message.toString())
                csvFile.delete()
                zipFile.delete()
                return@withContext Result.failure()
            }

            // 3) Возвращаем URI как строку (через FileProvider)
            val uri = try {
                FileProvider.getUriForFile(
                    applicationContext,
                    "${applicationContext.packageName}.fileprovider",
                    zipFile
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.d("Export.worker.uri", e.message.toString())
                csvFile.delete()
                zipFile.delete()
                return@withContext Result.failure()
            }

            val out = workDataOf(
                "zip_uri" to uri.toString(),
                "zip_name" to zipFile.name
            )
            Result.success(out)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.d("Export.worker.failed", e.message.toString())
            Result.failure()
        }
    }
}

private fun String.toSafeCsv(): String {
    if (isEmpty()) return ""
    val needsFormulaEscape = first() in listOf('=', '+', '-', '@') || first() == '\t'
    val safe = if (needsFormulaEscape) "'$this" else this
    val needsQuotes = safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    val escaped = safe.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}
