package com.project.objectacore

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Storage
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

@Composable
fun BovedaVista() {
    val context = LocalContext.current
    var tabSeleccionada by remember { mutableStateOf(0) }
    val titulosPestañas = listOf("CLÁSICOS", "MARCADORES OID")

    // Switch de origen de datos
    var modoNubeActivado by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Listas Locales
    var listaLocalesClasicos by remember { mutableStateOf<List<ObjetoLocal>>(emptyList()) }
    var listaLocalesOid by remember { mutableStateOf<List<ObjetoOidLocal>>(emptyList()) }

    // Listas de la Nube (Supabase)
    var listaNubeClasicos by remember { mutableStateOf<List<ObjetoActivo>>(emptyList()) }
    var listaNubeOid by remember { mutableStateOf<List<ObjetoActivoOid>>(emptyList()) }

    LaunchedEffect(modoNubeActivado, tabSeleccionada) {
        isLoading = true
        try {
            if (!modoNubeActivado) {
                // MODO LOCAL (SQLite)
                val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
                if (tabSeleccionada == 0) {
                    listaLocalesClasicos = dao.obtenerObjetosLocales()
                } else {
                    listaLocalesOid = dao.obtenerOidsLocales()
                }
            } else {
                // MODO NUBE (Supabase)
                if (tabSeleccionada == 0) {
                    listaNubeClasicos = SupabaseManager.obtenerTodosLosObjetos()
                } else {
                    listaNubeOid = SupabaseManager.obtenerTodosLosObjetosOid()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error de lectura: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)) {
            Icon(
                imageVector = if (modoNubeActivado) Icons.Default.Cloud else Icons.Default.Storage,
                contentDescription = null,
                tint = if (modoNubeActivado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "BÓVEDA DE ACTIVOS", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Origen de Datos", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text(text = if (modoNubeActivado) "Supabase (Red)" else "SQLite (Local)", style = MaterialTheme.typography.bodySmall, color = if (modoNubeActivado) Color(0xFF4CAF50) else Color.Gray)
                }
                Switch(checked = modoNubeActivado, onCheckedChange = { modoNubeActivado = it })
            }
        }

        TabRow(
            selectedTabIndex = tabSeleccionada,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(modifier = Modifier.tabIndicatorOffset(tabPositions[tabSeleccionada]), color = if (tabSeleccionada == 1) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary)
            },
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).padding(bottom = 16.dp)
        ) {
            titulosPestañas.forEachIndexed { index, titulo ->
                Tab(
                    selected = tabSeleccionada == index,
                    onClick = { tabSeleccionada = index },
                    text = { Text(text = titulo, fontFamily = FontFamily.Monospace, fontWeight = if (tabSeleccionada == index) FontWeight.Bold else FontWeight.Normal, color = if (tabSeleccionada == index && index == 1) Color(0xFF8B5CF6) else if (tabSeleccionada == index) MaterialTheme.colorScheme.primary else Color.Gray) }
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (tabSeleccionada == 1) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.primary)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 80.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!modoNubeActivado) {
                    if (tabSeleccionada == 0) {
                        if (listaLocalesClasicos.isEmpty()) item { MensajeVacio("Bóveda Local QR Vacía") }
                        items(listaLocalesClasicos) { objeto -> TarjetaClasicaLocal(objeto) }
                    } else {
                        if (listaLocalesOid.isEmpty()) item { MensajeVacio("Bóveda Local OID Vacía") }
                        items(listaLocalesOid) { oid -> TarjetaOidLocal(oid) }
                    }
                } else {
                    if (tabSeleccionada == 0) {
                        if (listaNubeClasicos.isEmpty()) item { MensajeVacio("Nube QR Vacía") }
                        items(listaNubeClasicos) { objeto -> TarjetaClasicaNube(objeto) }
                    } else {
                        if (listaNubeOid.isEmpty()) item { MensajeVacio("Nube OID Vacía") }
                        items(listaNubeOid) { oid -> TarjetaOidNube(oid) }
                    }
                }
            }
        }
    }
}

// --- COMPONENTES VISUALES ---

@Composable
fun TarjetaClasicaLocal(objeto: ObjetoLocal) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = objeto.serial_id, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                Badge(containerColor = Color.LightGray) { Text("LOCAL", color = Color.Black, fontSize = 10.sp) }
            }
            Text(text = "Desig: ${objeto.nombre}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun TarjetaOidLocal(oid: ObjetoOidLocal) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E8FF))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = oid.serial_id, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color(0xFF8B5CF6))
                Badge(containerColor = Color(0xFF8B5CF6)) { Text("OID LOCAL", color = Color.White, fontSize = 10.sp) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Desig: ${oid.nombre}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(Color(0x228B5CF6), RoundedCornerShape(4.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text(text = "ID: ${oid.codigo_numerico}", color = Color(0xFF6D28D9), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                IconButton(
                    onClick = {
                        if (!oid.ruta_archivo_local.isNullOrBlank()) {
                            GeneradorOidLocal.exportarSvgADescargasPublicas(context, oid.ruta_archivo_local, oid.serial_id)
                        } else {
                            Toast.makeText(context, "Error: Archivo no forjado físicamente.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.background(Color(0xFF8B5CF6), RoundedCornerShape(50))
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = "Descargar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun TarjetaClasicaNube(objeto: ObjetoActivo) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = objeto.serial_id, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color(0xFF2E7D32))
                Badge(containerColor = Color(0xFF4CAF50)) { Text("NUBE WEB", color = Color.White, fontSize = 10.sp) }
            }
            Text(text = "Desig: ${objeto.nombre}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            // Si tu ObjetoActivo tiene "notas", descomenta esto:
            // if (!objeto.notas.isNullOrBlank()) {
            //     Text(text = "Notas: ${objeto.notas}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
            // }
        }
    }
}

@Composable
fun TarjetaOidNube(oid: ObjetoActivoOid) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = oid.serial_id, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace, color = Color(0xFF6200EA))
                Badge(containerColor = Color(0xFF6200EA)) { Text("OID WEB", color = Color.White, fontSize = 10.sp) }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Desig: ${oid.nombre}", style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.background(Color(0x226200EA), RoundedCornerShape(4.dp)).padding(8.dp)) {
                Text(text = "ID: ${oid.codigo_numerico}", color = Color(0xFF311B92), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun MensajeVacio(texto: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text = texto, color = Color.Gray, fontFamily = FontFamily.Monospace)
    }
}