package com.project.objectacore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.project.objectacore.domain.models.ObjetoActivoOid
import com.project.objectacore.data.remote.SupabaseManager

@Composable
fun RegistrarVistaOid() {
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Prefijo "OID-" para distinguirlos visualmente de los "OBJ-"
    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OID-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)) {
            Icon(Icons.Default.Memory, contentDescription = "OID", tint = Color(0xFF8B5CF6), modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "FORJAR OID",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color(0xFF8B5CF6)
            )
        }

        OutlinedTextField(
            value = serialId,
            onValueChange = { serialId = it.uppercase().replace(Regex("[^A-Z0-9-]"), "") },
            label = { Text("Serial OID (Ej: OID-X1)", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { generarId() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Generar ID", tint = Color(0xFF8B5CF6))
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Designación del Objeto", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = notas,
            onValueChange = { notas = it },
            label = { Text("Telemetría / Notas", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth().height(100.dp),
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (serialId.isNotBlank() && nombre.isNotBlank()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            // 1. Generamos el ID matemático de 25 bits
                            val codigoOidNumerico = Random.nextInt(1, 33554431)

                            // 2. Empaquetamos usando el NUEVO modelo
                            val nuevoOid = ObjetoActivoOid(
                                serial_id = serialId,
                                codigo_numerico = codigoOidNumerico,
                                nombre = nombre,
                                notas = notas.ifBlank { null }
                            )

                            // 3. Disparamos a la tabla nueva aislada
                            SupabaseManager.registrarObjetoOid(nuevoOid)

                            // 4. Disparamos el SVG a la Edge Function
                            val svgCreado = SupabaseManager.forjarMarcadorSvg(codigoOidNumerico, serialId)

                            // 5. BLOCKCHAIN SIMULADO: Registramos el evento en el Ledger
                            SupabaseManager.registrarTelemetria(codigoOidNumerico, "ACTIVO_FORJADO")

                            if (svgCreado) {
                                codigoGenerado = codigoOidNumerico
                                Toast.makeText(context, "OID Forjado y SVG Guardado en Bucket", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "OID Guardado, pero falló la generación del SVG", Toast.LENGTH_LONG).show()
                            }

                            serialId = ""
                            nombre = ""
                            notas = ""
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error en forja: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    Toast.makeText(context, "Faltan datos requeridos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6))
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("GENERAR MARCADOR DIGITAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (codigoGenerado != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x228B5CF6), RoundedCornerShape(8.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IDENTIFICADOR DE 25 BITS:", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("$codigoGenerado", color = Color(0xFF8B5CF6), fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}