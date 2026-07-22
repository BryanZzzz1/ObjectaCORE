package com.project.objectacore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.project.objectacore.data.local.AppDatabase
import com.project.objectacore.data.local.ObjetoLocal
import com.project.objectacore.data.local.ObjetoOidLocal
import com.project.objectacore.data.remote.SupabaseManager
import com.project.objectacore.domain.models.ObjetoActivo
import com.project.objectacore.domain.models.ObjetoActivoOid
import com.project.objectacore.engine.vision.GeneradorOidLocal

@Composable
fun InventariadoVista() {
    // 1. CONTROL DE PESTAÑAS (Clásico vs OID)
    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosPestañas = listOf("CLÁSICO (QR)", "MARCADOR OID")

    // 2. SELECTOR TÁCTICO DE DESTINO (Web vs Local)
    var destinoWebActivado by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- CABECERA Y PESTAÑAS ---
        Text(
            text = "FORJA DE INVENTARIO",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        )

        TabRow(
            selectedTabIndex = tabSeleccionada,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]),
                    color = if (tabSeleccionada == 1) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        ) {
            titulosPestañas.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = {
                        Text(
                            text = titulo,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (tabSeleccionada == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (tabSeleccionada == index && index == 1) Color(0xFF8B5CF6)
                            else if (tabSeleccionada == index) MaterialTheme.colorScheme.primary
                            else Color.Gray
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SELECTOR DE DESTINO DE CARGA ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Destino Operativo",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (destinoWebActivado) "Carga hacia Supabase (Nube)" else "Carga hacia SQLite3 (Local)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Switch(
                    checked = destinoWebActivado,
                    onCheckedChange = { destinoWebActivado = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (tabSeleccionada == 1) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary,
                        checkedTrackColor = if (tabSeleccionada == 1) Color(0xFF8B5CF6).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- ADVERTENCIA DE TELEMETRÍA OID ---
        if (tabSeleccionada == 1 && !destinoWebActivado) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4A3E00)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ Telemetría web inoperativa, pero el Marcador Visual SVG se forjará físicamente en el dispositivo local.",
                        color = Color(0xFFFFD700),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // --- ÁREA DE TRABAJO ---
        Box(modifier = Modifier.fillMaxSize()) {
            when (tabSeleccionada) {
                0 -> FormularioClasico(esDestinoWeb = destinoWebActivado)
                1 -> FormularioOid(esDestinoWeb = destinoWebActivado)
            }
        }
    }
}

// =========================================================
// 1. FORMULARIO CLÁSICO (QR)
// =========================================================
@Composable
fun FormularioClasico(esDestinoWeb: Boolean) {
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OBJ-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = serialId,
            onValueChange = { serialId = it.uppercase().replace(Regex("[^A-Z0-9-]"), "") },
            label = { Text("ID de Etiqueta (Ej: OBJ-X1)", fontFamily = FontFamily.Monospace) },
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { generarId() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Generar ID", tint = MaterialTheme.colorScheme.primary)
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
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 4
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (serialId.isNotBlank() && nombre.isNotBlank()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            if (esDestinoWeb) {
                                val nuevoObjeto = ObjetoActivo(serial_id = serialId, nombre = nombre, notas = notas.ifBlank { null })
                                SupabaseManager.registrarObjeto(nuevoObjeto)
                                Toast.makeText(context, "Activo Sincronizado en Nube", Toast.LENGTH_SHORT).show()
                            } else {
                                val baseDatos = AppDatabase.obtenerBaseDatos(context)
                                val dao = baseDatos.inventarioDao()
                                dao.guardarObjeto(ObjetoLocal(serial_id = serialId, nombre = nombre))
                                Toast.makeText(context, "Activo Guardado en SQLite Local", Toast.LENGTH_SHORT).show()
                            }
                            serialId = ""
                            nombre = ""
                            notas = ""
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fallo: ${e.message}", Toast.LENGTH_LONG).show()
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
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text(text = if (esDestinoWeb) "ENCRIPTAR EN NUBE" else "ENCRIPTAR LOCAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// =========================================================
// 2. FORMULARIO OID (Marcadores Matemáticos)
// =========================================================
@Composable
fun FormularioOid(esDestinoWeb: Boolean) {
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OID-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                            val codigoOidNumerico = Random.nextInt(1, 33554431)

                            if (esDestinoWeb) {
                                // MODO NUBE
                                val nuevoOid = ObjetoActivoOid(serial_id = serialId, codigo_numerico = codigoOidNumerico, nombre = nombre, notas = notas.ifBlank { null })
                                SupabaseManager.registrarObjetoOid(nuevoOid)
                                val svgCreado = SupabaseManager.forjarMarcadorSvg(codigoOidNumerico, serialId)
                                SupabaseManager.registrarTelemetria(codigoOidNumerico, "ACTIVO_FORJADO")

                                if (svgCreado) Toast.makeText(context, "OID Web Forjado", Toast.LENGTH_LONG).show()
                                else Toast.makeText(context, "Fallo generación de SVG Web", Toast.LENGTH_LONG).show()
                            } else {
                                // MODO LOCAL - Inyectando Motor de Forja Local
                                val baseDatos = AppDatabase.obtenerBaseDatos(context)
                                val dao = baseDatos.inventarioDao()

                                // 1. Tejer SVG local
                                val svgString = GeneradorOidLocal.generarOidSvg(codigoOidNumerico.toLong())
                                val rutaFisica = GeneradorOidLocal.guardarSvgEnDispositivo(context, serialId, svgString)

                                // 2. Guardar en SQLite con la ruta
                                dao.guardarObjetoOid(
                                    ObjetoOidLocal(
                                        serial_id = serialId,
                                        codigo_numerico = codigoOidNumerico,
                                        nombre = nombre,
                                        ruta_archivo_local = rutaFisica ?: "" // Guardamos la ruta
                                    )
                                )
                                Toast.makeText(context, "OID Forjado en Bóveda Local", Toast.LENGTH_LONG).show()
                            }

                            codigoGenerado = codigoOidNumerico
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
                Text(text = if (esDestinoWeb) "FORJAR OID WEB" else "FORJAR OID LOCAL", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
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
                    Text("IDENTIFICADOR MATEMÁTICO:", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("$codigoGenerado", color = Color(0xFF8B5CF6), fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}