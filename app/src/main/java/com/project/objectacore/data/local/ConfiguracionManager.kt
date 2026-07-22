package com.project.objectacore.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Inicializamos el almacén de preferencias
private val Context.dataStore by preferencesDataStore(name = "configuracion_tactica")

class ConfiguracionManager(private val context: Context) {

    // La llave maestra de nuestro interruptor
    private val MODO_NUBE = booleanPreferencesKey("modo_nube_activado")

    // Leer el estado en tiempo real (Por defecto: true / Web)
    val modoNubeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[MODO_NUBE] ?: true
    }

    // Cambiar la configuración
    suspend fun establecerModoNube(activado: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[MODO_NUBE] = activado
        }
    }
}