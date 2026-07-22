package com.project.objectacore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosPestañas = listOf("CLÁSICO (QR)", "MARCADOR OID")
    var destinoWebActivado by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // CABECERA
        Text(
            text = "FORJA DE INVENTARIO",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        )

        // PESTAÑAS
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

        // SELECTOR DE DESTINO
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

        Box(modifier = Modifier.fillMaxSize()) {
            when (tabSeleccionada) {
                0 -> FormularioClasico(esDestinoWeb = destinoWebActivado)
                1 -> FormularioOid(esDestinoWeb = destinoWebActivado)
            }
        }
    }
}

// =========================================================
// 1. FORMULARIO CLÁSICO (QR) - CON CAMPOS DINÁMICOS
// =========================================================
@Composable
fun FormularioClasico(esDestinoWeb: Boolean) {
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Estado dinámico de atributos
    val camposDinamicos = remember { mutableStateListOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OBJ-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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

        Spacer(modifier = Modifier.height(24.dp))

        // SECCIÓN CAMPOS DINÁMICOS
        Text(
            text = "ATRIBUTOS PERSONALIZADOS (${camposDinamicos.size}/5)",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        camposDinamicos.forEachIndexed { index, valor ->
            OutlinedTextField(
                value = valor,
                onValueChange = { nuevoValor -> camposDinamicos[index] = nuevoValor },
                label = { Text("Atributo ${index + 1}", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                // CORRECCIÓN AQUÍ: removeAt(lastIndex)
                onClick = { if (camposDinamicos.isNotEmpty()) camposDinamicos.removeAt(camposDinamicos.lastIndex) },
                enabled = camposDinamicos.isNotEmpty()
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quitar", color = if (camposDinamicos.isNotEmpty()) Color.Red.copy(alpha = 0.8f) else Color.Gray)
            }

            TextButton(
                onClick = { if (camposDinamicos.size < 5) camposDinamicos.add("") },
                enabled = camposDinamicos.size < 5
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir Campo", color = if (camposDinamicos.size < 5) MaterialTheme.colorScheme.primary else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (serialId.isNotBlank() && nombre.isNotBlank()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val listaLimpia = camposDinamicos.filter { it.isNotBlank() }

                            if (esDestinoWeb) {
                                val nuevoObjeto = ObjetoActivo(
                                    id = serialId,
                                    nombre = nombre,
                                    tipoEtiqueta = "QR",
                                    camposDinamicos = listaLimpia
                                )
                                SupabaseManager.registrarObjeto(nuevoObjeto)
                                Toast.makeText(context, "Activo Sincronizado en Nube", Toast.LENGTH_SHORT).show()
                            } else {
                                val baseDatos = AppDatabase.obtenerBaseDatos(context)
                                val dao = baseDatos.inventarioDao()
                                dao.guardarObjeto(ObjetoLocal(
                                    serial_id = serialId,
                                    nombre = nombre,
                                    campos_dinamicos = listaLimpia
                                ))
                                Toast.makeText(context, "Activo Guardado en SQLite Local", Toast.LENGTH_SHORT).show()
                            }
                            serialId = ""
                            nombre = ""
                            camposDinamicos.clear()
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
// 2. FORMULARIO OID (Marcadores Matemáticos) - CON CAMPOS DINÁMICOS
// =========================================================
@Composable
fun FormularioOid(esDestinoWeb: Boolean) {
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var codigoGenerado by remember { mutableStateOf<Int?>(null) }

    // Estado dinámico de atributos
    val camposDinamicos = remember { mutableStateListOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OID-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
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

        Spacer(modifier = Modifier.height(24.dp))

        // SECCIÓN CAMPOS DINÁMICOS OID
        Text(
            text = "ATRIBUTOS PERSONALIZADOS (${camposDinamicos.size}/5)",
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFF8B5CF6),
            fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(8.dp))

        camposDinamicos.forEachIndexed { index, valor ->
            OutlinedTextField(
                value = valor,
                onValueChange = { nuevoValor -> camposDinamicos[index] = nuevoValor },
                label = { Text("Atributo ${index + 1}", fontFamily = FontFamily.Monospace) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                // CORRECCIÓN AQUÍ: removeAt(lastIndex)
                onClick = { if (camposDinamicos.isNotEmpty()) camposDinamicos.removeAt(camposDinamicos.lastIndex) },
                enabled = camposDinamicos.isNotEmpty()
            ) {
                Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quitar", color = if (camposDinamicos.isNotEmpty()) Color.Red.copy(alpha = 0.8f) else Color.Gray)
            }

            TextButton(
                onClick = { if (camposDinamicos.size < 5) camposDinamicos.add("") },
                enabled = camposDinamicos.size < 5
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Añadir Campo", color = if (camposDinamicos.size < 5) Color(0xFF8B5CF6) else Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (serialId.isNotBlank() && nombre.isNotBlank()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val codigoOidNumerico = Random.nextInt(1, 33554431)
                            val listaLimpia = camposDinamicos.filter { it.isNotBlank() }

                            if (esDestinoWeb) {
                                val nuevoOid = ObjetoActivoOid(
                                    serial_id = serialId,
                                    codigo_numerico = codigoOidNumerico,
                                    nombre = nombre,
                                    notas = if (listaLimpia.isNotEmpty()) listaLimpia.joinToString(" • ") else null
                                )
                                SupabaseManager.registrarObjetoOid(nuevoOid)
                                SupabaseManager.forjarMarcadorSvg(codigoOidNumerico, serialId)
                                SupabaseManager.registrarTelemetria(codigoOidNumerico, "ACTIVO_FORJADO")

                                Toast.makeText(context, "OID Web Forjado con Atributos", Toast.LENGTH_LONG).show()
                            } else {
                                val baseDatos = AppDatabase.obtenerBaseDatos(context)
                                val dao = baseDatos.inventarioDao()

                                val svgString = GeneradorOidLocal.generarOidSvg(codigoOidNumerico.toLong())
                                val rutaFisica = GeneradorOidLocal.guardarSvgEnDispositivo(context, serialId, svgString)

                                dao.guardarObjetoOid(
                                    ObjetoOidLocal(
                                        serial_id = serialId,
                                        codigo_numerico = codigoOidNumerico,
                                        nombre = nombre,
                                        ruta_archivo_local = rutaFisica ?: "",
                                        campos_dinamicos = listaLimpia
                                    )
                                )
                                Toast.makeText(context, "OID Forjado en Bóveda Local", Toast.LENGTH_LONG).show()
                            }

                            codigoGenerado = codigoOidNumerico
                            serialId = ""
                            nombre = ""
                            camposDinamicos.clear()
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