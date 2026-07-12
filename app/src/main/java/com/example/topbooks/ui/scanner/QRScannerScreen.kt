package com.example.topbooks.ui.scanner

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.topbooks.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.example.topbooks.ui.theme.ColorArcDarkBrown
import com.example.topbooks.ui.theme.ColorArcMediumBrown
import java.util.concurrent.Executors

/**
 * PANTALLA DE ESCÁNER DE CÓDIGOS DE BARRAS (Stateful Composable).
 * Provee una interfaz de cámara para escanear el ISBN de libros físicos y buscar su información.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onBackClick: () -> Unit,
    onBookFound: (String) -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {

            // 1. Capa de la cámara (Fondo)
            CameraPreview(
                onBarcodeScanned = { barcode ->
                    viewModel.onIsbnDetected(barcode)
                }
            )

            // 2. Capa visual: Retícula y Láser animado
            ScannerOverlay()

            // 3. Interfaz de control: Botón de cierre superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(R.string.scanner_close_desc), tint = Color.White)
                }
            }

            // 4. Texto de ayuda al usuario
            Text(
                text = "Apunta al código de barras (ISBN)",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 64.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // --- DIÁLOGOS DE RESULTADOS ---

            state.foundBook?.let { book ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissBookInfo() },
                    title = { Text(text = stringResource(R.string.scanner_book_found_title), fontWeight = FontWeight.Bold) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(book.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = stringResource(R.string.scanner_book_cover_desc),
                                modifier = Modifier.size(120.dp, 180.dp),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(book.title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text(book.authors.joinToString(", "), color = Color.Gray, fontSize = 14.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissBookInfo()
                                onBookFound(book.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorArcDarkBrown())
                        ) {
                            Text(stringResource(R.string.scanner_action_view_details))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissBookInfo() }) {
                            Text(stringResource(R.string.scanner_action_keep_scanning), color = ColorArcMediumBrown())
                        }
                    }
                )
            }

            state.notFoundIsbn?.let { isbn ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = { Text(stringResource(R.string.scanner_error_title)) },
                    text = { Text(stringResource(R.string.scanner_error_body, isbn)) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissError() },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorArcDarkBrown())
                        ) {
                            Text(stringResource(R.string.scanner_action_accept))
                        }
                    }
                )
            }

            // Overlay de carga (Oculta la retícula mientras descarga el libro)
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ColorArcMediumBrown())
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Buscando en bibliotecas...", color = Color.White)
                    }
                }
            }

        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.scanner_permission_rationale))
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text(stringResource(R.string.scanner_action_request_permission))
                }
            }
        }
    }
}

/**
 * OVERLAY VISUAL DE ESCANEO (Máscara invertida y láser).
 * Dibuja un fondo oscuro con un rectángulo transparente en el centro.
 */
@Composable
fun ScannerOverlay() {
    // Animación infinita para el láser que sube y baja
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_animation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Dimensiones del "agujero" (formato rectangular apaisado para códigos de barras)
        val rectWidth = canvasWidth * 0.7f
        val rectHeight = rectWidth * 0.5f

        val left = (canvasWidth - rectWidth) / 2f
        val top = (canvasHeight - rectHeight) / 2f
        val right = left + rectWidth
        val bottom = top + rectHeight

        val cornerLength = 30.dp.toPx()
        val strokeWidth = 4.dp.toPx()

        // 1. DIBUJAMOS LA MÁSCARA OSCURA CON EL AGUJERO (Técnica EvenOdd)
        val path = Path().apply {
            addRect(Rect(0f, 0f, canvasWidth, canvasHeight)) // Rectángulo exterior (toda la pantalla)
            addRoundRect(RoundRect(left, top, right, bottom, CornerRadius(16.dp.toPx()))) // Rectángulo interior (el agujero)
            fillType = PathFillType.EvenOdd // Esta propiedad es la que hace la "resta" geométrica
        }
        drawPath(path, Color.Black.copy(alpha = 0.6f))

        // 2. DIBUJAMOS LAS ESQUINAS DE ENFOQUE
        val cornerColor = Color(0xFFC89B8C)

        // Arriba - Izquierda
        drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth)
        // Arriba - Derecha
        drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth)
        drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth)
        // Abajo - Izquierda
        drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth)
        drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth)
        // Abajo - Derecha
        drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth)
        drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth)

        // 3. DIBUJAMOS EL LÁSER ANIMADO
        val laserY = top + (laserPosition * rectHeight)
        drawLine(
            color = Color(0xFFC89B8C).copy(alpha = 0.8f),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * COMPONENTE DE VISTA PREVIA DE CÁMARA.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun CameraPreview(onBarcodeScanned: (String) -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                            processImageProxy(imageProxy, onBarcodeScanned)
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalyzer
                    )
                } catch (exc: Exception) {
                    Log.e("QRScanner", "Error al vincular cámara", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * PROCESADOR DE FOTOGRAMAS.
 */
private val barcodeScanner by lazy {
    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8
        )
        .build()
    BarcodeScanning.getClient(options)
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onSuccess(value)
                    }
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}