package dev.keslorod.quickexpense.receipt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Surface
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.keslorod.quickexpense.R
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import coil.compose.AsyncImage
import android.graphics.BitmapFactory
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.graphicsLayer
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class ReceiptScanActivity : AppCompatActivity() {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // The camera may only be bound once this is true — binding it before the permission
    // dialog is answered left the preview black even after the user granted access.
    private var hasCameraPermission by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                hasCameraPermission = true
            } else {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            ReceiptScanScreen(
                hasCameraPermission = hasCameraPermission,
                onDone = { files ->
                    val data = intent.apply {
                        putStringArrayListExtra(EXTRA_RECEIPT_PATHS, ArrayList(files.map { it.absolutePath }))
                    }
                    setResult(Activity.RESULT_OK, data)
                    finish()
                },
                onCancel = {
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                },
                cameraExecutor = cameraExecutor
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

@Composable
private fun ReceiptScanScreen(
    hasCameraPermission: Boolean,
    onDone: (List<File>) -> Unit,
    onCancel: () -> Unit,
    cameraExecutor: ExecutorService
) {
    val context = LocalLifecycleOwner.current
    val appContext = androidx.compose.ui.platform.LocalContext.current

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lastFile by remember { mutableStateOf<File?>(null) }
    var lastSignature by remember { mutableStateOf<IntArray?>(null) }
    var currentDiff by remember { mutableStateOf<Double?>(null) }
    val capturedFiles = remember { mutableStateListOf<File>() }

    // Leaving without "Done" discards this session's pages — nothing else will ever reference
    // them, so they'd otherwise sit in the app's storage forever.
    val discardAndCancel = {
        capturedFiles.forEach { it.delete() }
        onCancel()
    }
    BackHandler { discardAndCancel() }

    val previewView = remember {
        PreviewView(appContext).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        previewView.post {
            val width = previewView.width
            val height = previewView.height
            if (width <= 0 || height <= 0) return@post

            // Wait for the provider asynchronously — get() on the main thread blocks the UI
            // (and risks an ANR) for as long as CameraX takes to initialize.
            val providerFuture = ProcessCameraProvider.getInstance(appContext)
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
                val viewPort = ViewPort.Builder(Rational(width, height), rotation)
                    .setScaleType(ViewPort.FILL_CENTER)
                    .build()

                val preview = Preview.Builder()
                    .setTargetRotation(rotation)
                    .build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation)
                    .build()
                imageCapture = capture

                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                analysis.setAnalyzer(cameraExecutor) { image ->
                    val sig = computeSignatureFromImageProxy(image, OVERLAY_RATIO, SIG_COLS, SIG_ROWS)
                    val diff = signatureDiff(lastSignature, sig)
                    currentDiff = diff
                    image.close()
                }

                val useCaseGroup = UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .addUseCase(analysis)
                    .build()

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    useCaseGroup
                )
            }, ContextCompat.getMainExecutor(appContext))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Vertical alignment guides (5% from edges)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center)
        ) {
            val inset = maxWidth * 0.05f
            val lineWidth = 2.dp
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(lineWidth)
                    .offset(x = inset)
                    .background(Color.Yellow.copy(alpha = 0.5f))
                    .align(Alignment.TopStart)
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(lineWidth)
                    .offset(x = -inset)
                    .background(Color.Yellow.copy(alpha = 0.5f))
                    .align(Alignment.TopEnd)
            )
        }

        // Overlay from previous frame (bottom strip) to guide overlap
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .align(Alignment.TopCenter)
        ) {
            val overlayHeight = maxHeight * OVERLAY_RATIO
            if (lastFile != null) {
                val isMatched = (currentDiff ?: Double.MAX_VALUE) < SIGNATURE_DIFF_THRESHOLD
                AsyncImage(
                    model = lastFile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.BottomCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(overlayHeight)
                        .background(if (isMatched) Color.Green.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f))
                        .align(Alignment.TopCenter),
                    alpha = if (isMatched) 0.6f else 0.35f
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Camera preview stays full-bleed; only the hint and buttons keep clear of the
                // status and navigation bars (the hint used to overlap the clock).
                .safeDrawingPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.receipt_scan_align_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.receipt_scan_pages_fmt, capturedFiles.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = discardAndCancel,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.cancel)) }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val file = createReceiptPageFile(appContext)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                        capture.takePicture(
                            outputOptions,
                            cameraExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    // Still on cameraExecutor here: decode the photo for its
                                    // signature now, off the main thread, then hand the result over.
                                    val signature = computeSignatureFromBitmap(
                                        file,
                                        MATCH_RATIO,
                                        SIG_COLS,
                                        SIG_ROWS
                                    )
                                    Handler(Looper.getMainLooper()).post {
                                        capturedFiles.add(file)
                                        lastFile = file
                                        lastSignature = signature
                                    }
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Handler(Looper.getMainLooper()).post {
                                        file.delete()
                                    }
                                }
                            }
                        )
                    },
                    enabled = true,
                    modifier = Modifier
                        .size(96.dp)
                        .align(Alignment.CenterVertically),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.4f),
                        contentColor = Color.Black
                    )
                ) { Text(stringResource(R.string.capture)) }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { onDone(capturedFiles.toList()) },
                    modifier = Modifier.weight(1f),
                    enabled = capturedFiles.isNotEmpty()
                ) { Text(stringResource(R.string.done)) }
            }
        }
    }
}

private fun createReceiptPageFile(context: android.content.Context): File {
    // filesDir, not cacheDir: receipts back warranty claims, so the OS clearing the cache
    // under storage pressure (which it can do at any time, silently) must not lose them.
    // Only removed on uninstall/"clear data" now, same as the rest of the app's data.
    val dir = File(context.filesDir, "receipts").apply { mkdirs() }
    // A per-second timestamp alone collided when two pages were captured within the same
    // second — the second shot overwrote the first. The random suffix keeps every page unique.
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val suffix = UUID.randomUUID().toString().take(8)
    return File(dir, "receipt_page_${stamp}_$suffix.jpg")
}

private const val OVERLAY_RATIO = 0.20f
private const val MATCH_RATIO = OVERLAY_RATIO * 0.20f
private const val SIG_COLS = 32
private const val SIG_ROWS = 8
private const val SIGNATURE_DIFF_THRESHOLD = 50.0

private fun computeSignatureFromBitmap(
    file: File,
    overlapRatio: Float,
    cols: Int,
    rows: Int
): IntArray? {
    // Only a cols x rows thumbnail of one strip is needed — decoding the full-resolution photo
    // just to shrink it again costs tens of MB per page.
    val bitmap = BitmapFactory.decodeFile(
        file.absolutePath,
        BitmapFactory.Options().apply { inSampleSize = 8 }
    ) ?: return null
    val stripHeight = (bitmap.height * overlapRatio).toInt().coerceAtLeast(1)
    val y = (bitmap.height - stripHeight).coerceAtLeast(0)
    val strip = android.graphics.Bitmap.createBitmap(bitmap, 0, y, bitmap.width, stripHeight)
    val scaled = android.graphics.Bitmap.createScaledBitmap(strip, cols, rows, true)
    val pixels = IntArray(cols * rows)
    scaled.getPixels(pixels, 0, cols, 0, 0, cols, rows)
    strip.recycle()
    if (scaled != strip) scaled.recycle()
    bitmap.recycle()
    return pixels.map { px ->
        val r = (px shr 16) and 0xFF
        val g = (px shr 8) and 0xFF
        val b = px and 0xFF
        (r + g + b) / 3
    }.toIntArray()
}

private fun computeSignatureFromImageProxy(
    image: androidx.camera.core.ImageProxy,
    overlapRatio: Float,
    cols: Int,
    rows: Int
): IntArray {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val width = image.width
    val height = image.height
    val stripHeight = (height * overlapRatio).toInt().coerceAtLeast(1)
    val sig = IntArray(cols * rows)

    for (r in 0 until rows) {
        val srcY = (r * (stripHeight - 1) / (rows - 1)).coerceAtLeast(0)
        for (c in 0 until cols) {
            val srcX = (c * (width - 1) / (cols - 1)).coerceAtLeast(0)
            val index = srcY * rowStride + srcX * pixelStride
            val value = buffer.get(index).toInt() and 0xFF
            sig[r * cols + c] = value
        }
    }
    return sig
}

private fun signatureDiff(a: IntArray?, b: IntArray): Double {
    if (a == null || a.size != b.size) return Double.MAX_VALUE
    var sum = 0.0
    for (i in a.indices) {
        sum += abs(a[i] - b[i])
    }
    return sum / a.size
}
