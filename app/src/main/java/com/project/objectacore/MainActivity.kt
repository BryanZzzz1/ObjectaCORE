package com.project.objectacore

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn // <-- IMPORTANTE: Importación para el OptIn
import androidx.camera.core.ExperimentalGetImage // <-- IMPORTANTE: Importación de CameraX
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// Importaciones de tus vistas en sus nuevas carpetas
import com.project.objectacore.ui.screens.RadarOptico
import com.project.objectacore.ui.screens.InventariadoVista
import com.project.objectacore.ui.screens.BovedaVista
import com.project.objectacore.ui.screens.ConfiguracionVista

@OptIn(ExperimentalGetImage::class) // <-- SOLUCIÓN AL ERROR AQUÍ
class MainActivity : ComponentActivity() {

    // 1. SOLICITUD NATIVA DE PERMISOS DE CÁMARA
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            println("Sistema Óptico: Autorizado")
        } else {
            println("Sistema Óptico: Denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 0. CARGA DE LIBRERÍAS NATIVAS (OpenCV)
        if (org.opencv.android.OpenCVLoader.initDebug()) {
            Log.d("SISTEMA_CORE", "OpenCV cargado correctamente.")
        } else {
            Log.e("SISTEMA_CORE", "Error crítico: OpenCV no pudo ser inicializado.")
        }

        // Ejecutamos la validación de hardware al abrir la app
        validarPermisosDeHardware()

        // 2. INYECCIÓN DE LA INTERFAZ (Jetpack Compose)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { // Forzamos Modo Oscuro táctico
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ObjectaCoreApp()
                }
            }
        }
    }

    private fun validarPermisosDeHardware() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                // El permiso ya fue otorgado en ejecuciones anteriores
            }
            else -> {
                // Lanzamos el pop-up del sistema operativo
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}

// 3. ESTRUCTURA DE NAVEGACIÓN ACTUALIZADA (4 MÓDULOS)
@OptIn(ExperimentalGetImage::class) // <-- SOLUCIÓN AL ERROR AQUÍ TAMBIÉN POR SI ACASO
@Composable
fun ObjectaCoreApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // BOTÓN 1: RADAR ÓPTICO
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Escáner") },
                    label = { Text("Escáner", fontFamily = FontFamily.Monospace) },
                    selected = currentRoute == "escanear",
                    onClick = {
                        navController.navigate("escanear") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                // BOTÓN 2: INVENTARIADO (Fusión Clásico + OID)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AddCircle, contentDescription = "Inventariado") },
                    label = { Text("Forja", fontFamily = FontFamily.Monospace) },
                    selected = currentRoute == "inventariado",
                    onClick = {
                        navController.navigate("inventariado") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                // BOTÓN 3: BÓVEDA
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Storage, contentDescription = "Bóveda") },
                    label = { Text("Bóveda", fontFamily = FontFamily.Monospace) },
                    selected = currentRoute == "vault",
                    onClick = {
                        navController.navigate("vault") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )

                // BOTÓN 4: SISTEMA / SETTINGS (Switch Local/Web)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Sistema") },
                    label = { Text("Sistema", fontFamily = FontFamily.Monospace) },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.startDestinationId)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // 4. EL ENRUTADOR
        NavHost(
            navController = navController,
            startDestination = "escanear", // Arrancamos en el escáner directamente
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("escanear") {
                RadarOptico() // El radar intacto
            }
            composable("inventariado") {
                InventariadoVista() // La nueva vista fusionada que crearemos
            }
            composable("vault") {
                BovedaVista() // La bóveda que modificaremos con los badges
            }
            composable("settings") {
                ConfiguracionVista() // El panel de control SQLite3 vs Web
            }
        }
    }
}

// Composable temporal para rellenar vistas en construcción
@Composable
fun VistaTemporal(mensaje: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = mensaje, fontFamily = FontFamily.Monospace)
    }
}