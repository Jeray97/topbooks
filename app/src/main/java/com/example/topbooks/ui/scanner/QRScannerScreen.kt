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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
 *
 * @param onBackClick Acción para regresar a la pantalla anterior.
 * @param onBookFound Acción ejecutada al confirmar la selección de un libro encontrado.
 * @param viewModel Gestiona el estado de búsqueda del libro a partir del código escaneado.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScannerScreen(
    onBackClick: () -> Unit,
    onBookFound: (String) -> Unit,
    viewModel: ScannerViewModel = viewModel()
) {
    // Gestión de permisos de cámara en tiempo de ejecución
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val state by viewModel.uiState.collectAsState()

    // Solicitud automática de permisos al entrar en la pantalla
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Capa de la cámara
            CameraPreview(
                onBarcodeScanned = { barcode ->
                    viewModel.onIsbnDetected(barcode)
                }
            )

            // Interfaz de control: Botón de cierre
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

            // Consola de estado: Muestra mensajes informativos sobre el proceso de escaneo
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = state.uiLog,
                    color = Color.Green,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            // Diálogo de éxito: Se muestra cuando el ISBN coincide con un libro en la API
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
                            Text(book.title, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Text(book.authors.joinToString(", "), color = Color.Gray, fontSize = 14.sp)
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.dismissBookInfo()
                                onBookFound(book.id)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorArcDarkBrown)
                        ) {
                            Text(stringResource(R.string.scanner_action_view_details))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissBookInfo() }) {
                            Text(stringResource(R.string.scanner_action_keep_scanning), color = ColorArcMediumBrown)
                        }
                    }
                )
            }

            // Diálogo de error: Se muestra si el código no pertenece a ningún libro conocido
            state.notFoundIsbn?.let { isbn ->
                AlertDialog(
                    onDismissRequest = { viewModel.dismissError() },
                    title = { Text(stringResource(R.string.scanner_error_title)) },
                    text = { Text(stringResource(R.string.scanner_error_body, isbn)) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.dismissError() },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorArcDarkBrown)
                        ) {
                            Text(stringResource(R.string.scanner_action_accept))
                        }
                    }
                )
            }

            // Overlay de carga durante la consulta a la API
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorArcMediumBrown)
                }
            }

        } else {
            // Vista informativa en caso de que el permiso de cámara sea denegado
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
 * COMPONENTE DE VISTA PREVIA DE CÁMARA.
 * Integra CameraX mediante una AndroidView para mostrar el flujo de video y procesar los fotogramas.
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

                // Configuración del flujo de visualización
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                // Configuración del analizador de imágenes para detección de códigos
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
                    // Vinculación de la cámara al ciclo de vida del componente
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
 * Convierte el proxy de imagen de la cámara en un formato compatible con ML Kit Barcode Scanning.
 */
@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    onSuccess: (String) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        // OPTIMIZACIÓN: Configuramos el escáner para buscar exclusivamente formatos de ISBN
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8
            )
            .build()

        val scanner = BarcodeScanning.getClient(options)

        // Procesamiento asíncrono del fotograma
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        onSuccess(value) // Código detectado exitosamente
                    }
                }
            }
            .addOnCompleteListener {
                // Es crítico cerrar el proxy para liberar la cámara para el siguiente fotograma
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}