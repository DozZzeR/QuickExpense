package dev.keslorod.quickexpense.receipt

import android.net.Uri
import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/**
 * Слой сканирования чеков. Можно заменить реализацию через CompositionLocal.
 */
data class ReceiptScanResult(
    val uris: List<Uri>,
    val files: List<File>,
    val displayName: String
)

interface ReceiptScanner {
    @Composable
    fun rememberLauncher(onResult: (ReceiptScanResult?) -> Unit): ReceiptScannerHandle
}

class ReceiptScannerHandle(val start: () -> Unit)

class CameraReceiptScanner : ReceiptScanner {
    @Composable
    override fun rememberLauncher(onResult: (ReceiptScanResult?) -> Unit): ReceiptScannerHandle {
        val context = LocalContext.current
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val data = result.data ?: run {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val paths = data.getStringArrayListExtra(EXTRA_RECEIPT_PATHS).orEmpty()
            if (paths.isEmpty()) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val files = paths.map { File(it) }.filter { it.exists() }
            if (files.isEmpty()) {
                onResult(null)
                return@rememberLauncherForActivityResult
            }
            val uris = files.map { file ->
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            }
            onResult(
                ReceiptScanResult(
                    uris = uris,
                    files = files,
                    displayName = files.last().name
                )
            )
        }

        val startScan = {
            val intent = Intent(context, ReceiptScanActivity::class.java)
            launcher.launch(intent)
        }

        return ReceiptScannerHandle(start = startScan)
    }
}

val LocalReceiptScanner = staticCompositionLocalOf<ReceiptScanner> { CameraReceiptScanner() }

const val EXTRA_RECEIPT_PATHS = "extra_receipt_paths"
