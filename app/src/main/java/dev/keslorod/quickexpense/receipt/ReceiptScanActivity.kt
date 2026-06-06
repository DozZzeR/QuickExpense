package dev.keslorod.quickexpense.receipt

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.Surface
import androidx.activity.ComponentActivity
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
import androidx.compose.ui.unit.dp
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
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class ReceiptScanActivity : ComponentActivity() {
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (!granted) {
                setResult(Activity.RESULT_CANCELED)
                finish()
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            ReceiptScanScreen(
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

    val previewView = remember {
        PreviewView(appContext).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    LaunchedEffect(Unit) {
        previewView.post {
            val width = previewView.width
            val height = previewView.height
            if (width <= 0 || height <= 0) return@post

            val cameraProvider = ProcessCameraProvider.getInstance(appContext).get()
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
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Align the next frame with the overlay (20% overlap)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Pages: ${capturedFiles.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }

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
                                    Handler(Looper.getMainLooper()).post {
                                        capturedFiles.add(file)
                                        lastFile = file
                                        lastSignature = computeSignatureFromBitmap(
                                            file,
                                            MATCH_RATIO,
                                            SIG_COLS,
                                            SIG_ROWS
                                        )
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
                ) { Text("Capture") }

                Spacer(Modifier.width(12.dp))

                Button(
                    onClick = { onDone(capturedFiles.toList()) },
                    modifier = Modifier.weight(1f),
                    enabled = capturedFiles.isNotEmpty()
                ) { Text("Done") }
            }
        }
    }
}

private fun createReceiptPageFile(context: android.content.Context): File {
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    return File(dir, "receipt_page_$stamp.jpg")
}

private const val OVERLAY_RATIO = 0.20f
private const val MATCH_RATIO = OVERLAY_RATIO * 0.20f
private const val SIG_COLS = 32
private const val SIG_ROWS = 8
private const val SIGNATURE_DIFF_THRESHOLD = 50.0
private const val OVERLAY_SCALE_Y = 0.95f

private fun computeSignatureFromBitmap(
    file: File,
    overlapRatio: Float,
    cols: Int,
    rows: Int
): IntArray? {
    val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
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
