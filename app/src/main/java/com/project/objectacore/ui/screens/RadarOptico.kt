package com.project.objectacore.ui.screens

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import com.project.objectacore.domain.models.ObjetoActivo
import com.project.objectacore.domain.models.ObjetoActivoOid
import com.project.objectacore.data.remote.SupabaseManager
import com.project.objectacore.data.local.AppDatabase
import com.project.objectacore.engine.vision.PaddleCerebro
import com.project.objectacore.engine.vision.OidDetector

@androidx.camera.core.ExperimentalGetImage
@Composable
fun RadarOptico() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Estados de Producción
    var codigoDetectado by remember { mutableStateOf<String?>(null) }
    var objetoCompleto by remember { mutableStateOf<ObjetoActivo?>(null) }
    var estadoSistema by remember { mutableStateOf("Rastreando área...") }

    // Arsenal Táctico: "OID" (OpenCV), "QR" (ML Kit), "OCR" (Paddle)
    var modoArma by remember { mutableStateOf("OID") }

    // Banderas Tácticas
    var buscandoEnBD by remember { mutableStateOf(false) }
    var congelarEscaneo by remember { mutableStateOf(false) }

    // Inicialización silenciosa del motor OCR
    LaunchedEffect(Unit) {
        PaddleCerebro.inicializar(context)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val barcodeScanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
                    val executor = Executors.newSingleThreadExecutor()

                    imageAnalysis.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {

                            if (congelarEscaneo || buscandoEnBD) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            when (modoArma) {
                                "QR" -> {
                                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty()) {
                                                val idRaw = barcodes[0].displayValue
                                                if (idRaw != null) {
                                                    buscandoEnBD = true
                                                    estadoSistema = "Verificando BD..."
                                                    realizarConsultaHibridaQr(idRaw, context, coroutineScope) { datos ->
                                                        if (datos != null) {
                                                            estadoSistema = "¡OBJETO ENCONTRADO!"
                                                            codigoDetectado = idRaw
                                                            objetoCompleto = datos
                                                            congelarEscaneo = true
                                                        } else {
                                                            estadoSistema = "Rastreando área..."
                                                        }
                                                        buscandoEnBD = false
                                                    }
                                                }
                                            }
                                        }.addOnCompleteListener { imageProxy.close() }
                                }
                                "OCR" -> {
                                    val croppedBitmap = imageProxy.toCroppedBitmap()
                                    if (croppedBitmap != null && PaddleCerebro.estaListo) {
                                        val idLimpio = PaddleCerebro.reconocerTexto(croppedBitmap).trim().uppercase()
                                        if (idLimpio.length > 2) {
                                            buscandoEnBD = true
                                            realizarConsultaHibridaQr(idLimpio, context, coroutineScope) { datos ->
                                                if (datos != null) {
                                                    estadoSistema = "¡OBJETO ENCONTRADO!"
                                                    codigoDetectado = idLimpio
                                                    objetoCompleto = datos
                                                    congelarEscaneo = true
                                                } else {
                                                    estadoSistema = "Rastreando área..."
                                                }
                                                buscandoEnBD = false
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }
                                "OID" -> {
                                    val originalBitmap = imageProxy.toBitmap()
                                    if (originalBitmap != null) {
                                        val matrix = Matrix()
                                        matrix.postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())
                                        val rotated = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                                        val mat = org.opencv.core.Mat()
                                        org.opencv.android.Utils.bitmapToMat(rotated, mat)
                                        val idDetectado = OidDetector.procesarFrame(mat)
                                        mat.release()

                                        if (idDetectado != null) {
                                            buscandoEnBD = true
                                            coroutineScope.launch {
                                                // 1. INTENTAR PRIMERO EN SUPABASE
                                                var datosOid: ObjetoActivoOid? = null
                                                try {
                                                    datosOid = SupabaseManager.obtenerObjetoOidPorCodigo(idDetectado)
                                                } catch (e: Exception) {
                                                    Log.e("RadarOptico", "Fallo web buscando OID")
                                                }

                                                if (datosOid != null) {
                                                    Log.i("RadarOptico", "✅ OID Encontrado en Supabase. Enviando Telemetría.")
                                                    val payloadEscaneo = mapOf(
                                                        "dispositivo_escaner" to android.os.Build.MODEL,
                                                        "motor_vision" to "OpenCV Telar 6x6",
                                                        "timestamp_local" to System.currentTimeMillis().toString()
                                                    )
                                                    SupabaseManager.registrarTelemetria(idDetectado, "ESCANEADO_EN_TERRENO", payloadEscaneo)
                                                } else {
                                                    // 2. RESPALDO LOCAL
                                                    Log.w("RadarOptico", "❌ OID no encontrado en Nube. Intentando Local...")
                                                    try {
                                                        val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
                                                        val local = dao.buscarOidPorCodigo(idDetectado)
                                                        if (local != null) {
                                                            datosOid = ObjetoActivoOid(
                                                                serial_id = local.serial_id,
                                                                codigo_numerico = local.codigo_numerico,
                                                                nombre = local.nombre,
                                                                notas = "[MODO OFFLINE] Activo Local"
                                                            )
                                                        }
                                                    } catch (e: Exception) {
                                                        Log.e("RadarOptico", "Error SQLite OID", e)
                                                    }
                                                }

                                                // 3. RESOLUCIÓN FINAL CORREGIDA
                                                if (datosOid != null) {
                                                    estadoSistema = "¡OBJETO ENCONTRADO!"
                                                    codigoDetectado = idDetectado.toString()

                                                    // Empaquetamos las notas antiguas en la nueva lista de camposDinamicos
                                                    val listaDinamica = if (!datosOid.notas.isNullOrBlank()) listOf(datosOid.notas!!) else emptyList()

                                                    objetoCompleto = ObjetoActivo(
                                                        id = datosOid.serial_id,
                                                        nombre = datosOid.nombre,
                                                        tipoEtiqueta = "OID",
                                                        camposDinamicos = listaDinamica
                                                    )

                                                    congelarEscaneo = true
                                                } else {
                                                    estadoSistema = "Rastreando área..."
                                                }
                                                buscandoEnBD = false
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }
                            }
                        } else { imageProxy.close() }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Retícula central dinámica
        Box(
            modifier = Modifier
                .size(if (modoArma == "QR") 220.dp else 140.dp)
                .align(Alignment.Center)
                .border(
                    width = 2.dp,
                    color = if (congelarEscaneo) Color.Green else {
                        when (modoArma) {
                            "QR" -> Color(0xFF0EA5E9)   // Azul
                            "OCR" -> Color(0xFFF59E0B)  // Naranja
                            else -> Color(0xFF8B5CF6)   // Morado para OID
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                )
        )

        // Selector de Modo (Botón Flotante Cíclico)
        FloatingActionButton(
            onClick = {
                modoArma = when (modoArma) {
                    "OID" -> "QR"
                    "QR" -> "OCR"
                    else -> "OID"
                }
                congelarEscaneo = false
                codigoDetectado = null
                objetoCompleto = null
                estadoSistema = "Rastreando área..."
            },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp).padding(top = 16.dp),
            containerColor = when (modoArma) {
                "QR" -> Color(0xFF0EA5E9)
                "OCR" -> Color(0xFFF59E0B)
                else -> Color(0xFF8B5CF6)
            }
        ) {
            Icon(
                imageVector = when (modoArma) {
                    "QR" -> Icons.Default.QrCode
                    "OCR" -> Icons.Default.TextFields
                    else -> Icons.Default.GridView
                },
                contentDescription = "Foco",
                tint = Color.White
            )
        }

        // Banner Superior de Estado Simple
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
                .background(Color(0x99000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = estadoSistema,
                color = if (congelarEscaneo) Color.Green else {
                    when (modoArma) {
                        "QR" -> Color(0xFF0EA5E9)
                        "OCR" -> Color(0xFFF59E0B)
                        else -> Color(0xFF8B5CF6)
                    }
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        // Bóveda Inferior CORREGIDA (Aparece SOLO cuando encuentra un resultado exitoso)
        if (congelarEscaneo && objetoCompleto != null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp, start = 16.dp, end = 16.dp).fillMaxWidth().background(Color(0xEE0F172A), RoundedCornerShape(16.dp)).border(1.dp, Color.Green, RoundedCornerShape(16.dp)))
            {
                // BOTÓN CERRAR
                IconButton(
                    onClick = {
                        congelarEscaneo = false
                        codigoDetectado = null
                        objetoCompleto = null
                        estadoSistema = "Rastreando área..."
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray) }

                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("ID CONFIRMADO: $codigoDetectado", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = objetoCompleto!!.nombre, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)

                    // Extraemos los datos de la nueva lista dinámica en lugar de "notas"
                    if (objetoCompleto!!.camposDinamicos.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "INFORMACIÓN: ${objetoCompleto!!.camposDinamicos.joinToString(" • ")}",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

// EXTRACCIÓN Y RECORTE DE COORDENADAS CON ESCUDO ANTI-CRASH
@androidx.camera.core.ExperimentalGetImage
fun ImageProxy.toCroppedBitmap(): Bitmap? {
    try {
        val bitmapOriginal = this.toBitmap() ?: return null
        val matrix = Matrix()
        matrix.postRotate(this.imageInfo.rotationDegrees.toFloat())
        val bitmapRotated = Bitmap.createBitmap(bitmapOriginal, 0, 0, bitmapOriginal.width, bitmapOriginal.height, matrix, true)

        val width = bitmapRotated.width
        val height = bitmapRotated.height

        val cropWidth = (width * 0.45).toInt()
        val cropHeight = (height * 0.15).toInt()
        val startX = (width / 2) - (cropWidth / 2)
        val startY = (height / 2) - (cropHeight / 2)

        if (cropWidth > 0 && cropHeight > 0) {
            return Bitmap.createBitmap(bitmapRotated, startX, startY, cropWidth, cropHeight)
        }
    } catch (e: Exception) {
        Log.e("RadarOptico", "Error en segmentación de bitmap", e)
    }
    return null
}

fun realizarConsultaHibridaQr(id: String, context: Context, scope: CoroutineScope, onResult: (ObjetoActivo?) -> Unit) {
    scope.launch {
        val resultado = SupabaseManager.buscarObjetoHibrido(context, id)
        onResult(resultado)
    }
}

// FUNCIÓN ANTIGUA PRESERVADA PARA NO ROMPER NADA
private fun dispararConsulta(idConsultado: String, scope: CoroutineScope, onResult: (ObjetoActivo?) -> Unit) {
    scope.launch {
        val datos = SupabaseManager.obtenerObjetoPorSerial(idConsultado)
        onResult(datos)
    }
}