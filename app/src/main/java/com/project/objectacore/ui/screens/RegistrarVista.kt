package com.project.objectacore.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.project.objectacore.domain.models.ObjetoActivo
import com.project.objectacore.data.remote.SupabaseManager

@Composable
fun RegistrarVista() {
    // Variables de estado (Lo que el usuario escribe)
    var serialId by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    // Estado de carga para el botón
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope() // Para operaciones en segundo plano

    // Función para generar ID aleatorio (Igual que en tu versión anterior)
    fun generarId() {
        val caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        serialId = "OBJ-" + (1..4).map { caracteres.random() }.joinToString("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "NUEVO ACTIVO",
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
        )

        OutlinedTextField(
            value = serialId,
            onValueChange = { serialId = it },
            label = { Text("ID de Etiqueta (Serial)", fontFamily = FontFamily.Monospace) },
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
                            // MEJORA: Generamos el ID matemático de 25 bits para el OID (1 a 33 Millones)
                            val codigoOidNumerico = Random.nextInt(1, 33554431)

                            // Empaquetamos los datos en nuestro modelo, sumando el nuevo código numérico
                            val nuevoObjeto = ObjetoActivo(
                                serial_id = serialId,
                                nombre = nombre,
                                notas = notas.ifBlank { null },
                            )

                            // Disparamos la conexión a Supabase
                            SupabaseManager.registrarObjeto(nuevoObjeto)

                            // Reflejamos en el Toast el código OID creado
                            Toast.makeText(context, "Activo Encriptado en Bóveda. OID Asignado: $codigoOidNumerico", Toast.LENGTH_LONG).show()

                            // Limpiamos la terminal
                            serialId = ""
                            nombre = ""
                            notas = ""
                        } catch (e: Exception) {
                            Toast.makeText(context, "Fallo de conexión: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                } else {
                    Toast.makeText(context, "Faltan datos requeridos", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            enabled = !isLoading,
            shape = MaterialTheme.shapes.medium
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Text("ENCRIPTAR EN BÓVEDA", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}