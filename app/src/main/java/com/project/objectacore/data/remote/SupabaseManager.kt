package com.project.objectacore.data.remote

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import android.content.Context
import com.project.objectacore.domain.models.ObjetoActivo
import com.project.objectacore.domain.models.ObjetoActivoOid
import com.project.objectacore.domain.models.RegistroLedger
import com.project.objectacore.data.local.AppDatabase
import com.project.objectacore.BuildConfig

// Molde de datos para enviar a la Edge Function
@Serializable
data class OidPayload(val codigoNumerico: Int, val serialId: String)

object SupabaseManager {

    // Inicialización del cliente blindado contra errores JSON
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest) {
            serializer = KotlinXSerializer(
                Json {
                    ignoreUnknownKeys = true // Evita crasheos por columnas adicionales
                    isLenient = true         // Tolerante a formatos JSON flexibles
                    explicitNulls = false    // Permite mapear valores nulos de forma transparente
                }
            )
        }
        install(Functions) // Motor de Edge Functions habilitado
    }

    // ====================================================================
    // 🛡️ SISTEMA CLÁSICO (QR / ML Kit)
    // ====================================================================

    // OPERACIÓN 1: Insertar nuevo activo clásico
    suspend fun registrarObjeto(objeto: ObjetoActivo) {
        withContext(Dispatchers.IO) {
            client.from("objetos").insert(objeto)
        }
    }

    // OPERACIÓN 2: Obtener todo el inventario clásico
    suspend fun obtenerTodosLosObjetos(): List<ObjetoActivo> {
        return withContext(Dispatchers.IO) {
            client.from("objetos")
                .select()
                .decodeList<ObjetoActivo>()
        }
    }

    // OPERACIÓN 3: Buscar objeto clásico por ID (CORREGIDO DE "serial_id" A "id")
    suspend fun obtenerObjetoPorSerial(serialId: String): ObjetoActivo? {
        return withContext(Dispatchers.IO) {
            try {
                client.from("objetos").select {
                    filter { eq("id", serialId) } // ✅ Campo corregido acorde al esquema de Supabase
                }.decodeSingleOrNull<ObjetoActivo>()
            } catch (e: Exception) {
                Log.e("SupabaseManager", "Error buscando QR clásico: ${e.message}")
                null
            }
        }
    }

    // ====================================================================
    // 🚀 NUEVO SISTEMA OID (OpenCV Híbrido)
    // ====================================================================

    // OPERACIÓN 4: Insertar en la tabla OID
    suspend fun registrarObjetoOid(objetoOid: ObjetoActivoOid) {
        withContext(Dispatchers.IO) {
            client.from("objetos_oid").insert(objetoOid)
        }
    }

    // OPERACIÓN 5: Buscar específicamente por el código numérico de 25 bits
    suspend fun obtenerObjetoOidPorCodigo(codigoNumerico: Int): ObjetoActivoOid? {
        return withContext(Dispatchers.IO) {
            try {
                client.from("objetos_oid").select {
                    filter { eq("codigo_numerico", codigoNumerico) }
                }.decodeSingleOrNull<ObjetoActivoOid>()
            } catch (e: Exception) {
                Log.e("SupabaseManager", "Error buscando OID: ${e.message}")
                null
            }
        }
    }

    // OPERACIÓN 6: Disparar la forja visual del SVG en la nube
    suspend fun forjarMarcadorSvg(codigoNumerico: Int, serialId: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                client.functions.invoke(
                    function = "generar-oid",
                    body = OidPayload(codigoNumerico, serialId)
                )
                true
            } catch (e: Exception) {
                Log.e("SupabaseManager", "Error al ejecutar Edge Function", e)
                false
            }
        }
    }

    // OPERACIÓN 8: Obtener todo el inventario OID
    suspend fun obtenerTodosLosObjetosOid(): List<ObjetoActivoOid> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("objetos_oid")
                    .select()
                    .decodeList<ObjetoActivoOid>()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Búsqueda Híbrida QR (Red -> SQLite)
    suspend fun buscarObjetoHibrido(context: Context, serialId: String): ObjetoActivo? {
        // 1. Intenta buscar en Supabase
        val webResult = try { obtenerObjetoPorSerial(serialId) } catch (e: Exception) { null }
        if (webResult != null) return webResult

        // 2. Si no encontró nada en la nube, busca en la base local (SQLite)
        val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
        val localResult = dao.buscarClasicoPorId(serialId)
        return localResult?.let { ObjetoActivo(id = it.serial_id, nombre = it.nombre, notas = "Modo Offline") }
    }

    // Búsqueda Híbrida OID (Red -> SQLite)
    suspend fun buscarOidHibrido(context: Context, codigo: Int): ObjetoActivoOid? {
        // 1. Intenta buscar en Supabase
        val webResult = try { obtenerObjetoOidPorCodigo(codigo) } catch (e: Exception) { null }
        if (webResult != null) return webResult

        // 2. Si no encontró nada en la nube, busca en la base local (SQLite)
        val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
        val localResult = dao.buscarOidPorCodigo(codigo)
        return localResult?.let { ObjetoActivoOid(it.serial_id, it.codigo_numerico, it.nombre, "Modo Offline") }
    }

    // ====================================================================
    // 🔗 BLOCKCHAIN SIMULADO (Ledger de Telemetría)
    // ====================================================================

    suspend fun registrarTelemetria(
        codigoNumerico: Int,
        tipoEvento: String,
        datosExtra: Map<String, String>? = null
    ) {
        withContext(Dispatchers.IO) {
            try {
                val registro = RegistroLedger(
                    codigo_numerico = codigoNumerico,
                    evento = tipoEvento,
                    metadatos = datosExtra
                )
                client.from("ledger_oid").insert(registro)
                Log.d("SupabaseManager", "🔗 Telemetría inyectada en Ledger: $tipoEvento para OID $codigoNumerico")
            } catch (e: Exception) {
                Log.e("SupabaseManager", "Fallo al registrar telemetría", e)
            }
        }
    }
}