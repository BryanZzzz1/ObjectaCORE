package com.project.objectacore

import android.util.Log
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import android.content.Context
// Molde de datos para enviar a la Edge Function
@Serializable
data class OidPayload(val codigoNumerico: Int, val serialId: String)

object SupabaseManager {

    // Inicialización del cliente
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest)
        install(Functions) // OBLIGATORIO: Motor de funciones habilitado
    }

    // ====================================================================
    // 🛡️ SISTEMA CLÁSICO (QR / ML Kit) - INTOCABLE
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

    // OPERACIÓN 3: Buscar objeto clásico por ID
    suspend fun obtenerObjetoPorSerial(serialId: String): ObjetoActivo? {
        return withContext(Dispatchers.IO) {
            try {
                client.from("objetos").select {
                    filter { eq("serial_id", serialId) }
                }.decodeSingleOrNull<ObjetoActivo>()
            } catch (e: Exception) {
                null
            }
        }
    }

    // ====================================================================
    // 🚀 NUEVO SISTEMA OID (OpenCV Híbrido) - TOTALMENTE AISLADO
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

    // OPERACIÓN 8: Obtener todo el inventario OID (Para la Bóveda Unificada)
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

    suspend fun buscarObjetoHibrido(context: Context, serialId: String): ObjetoActivo? {
        // 1. Intenta buscar en Supabase
        val webResult = try { obtenerObjetoPorSerial(serialId) } catch (e: Exception) { null }
        if (webResult != null) return webResult

        // 2. Si no encontró nada, busca en SQLite local
        val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
        val localResult = dao.buscarClasicoPorId(serialId)
        return localResult?.let { ObjetoActivo(it.serial_id, it.nombre, "Modo Offline") }
    }

    suspend fun buscarOidHibrido(context: Context, codigo: Int): ObjetoActivoOid? {
        // 1. Intenta buscar en Supabase
        val webResult = try { obtenerObjetoOidPorCodigo(codigo) } catch (e: Exception) { null }
        if (webResult != null) return webResult

        // 2. Si no encontró nada, busca en SQLite local
        val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
        val localResult = dao.buscarOidPorCodigo(codigo)
        return localResult?.let { ObjetoActivoOid(it.serial_id, it.codigo_numerico, it.nombre, "Modo Offline") }
    }






    // ====================================================================
    // 🔗 BLOCKCHAIN SIMULADO (Ledger de Telemetría)
    // ====================================================================

    // OPERACIÓN 7: Inyectar telemetría en el Ledger Inmutable
    suspend fun registrarTelemetria(
        codigoNumerico: Int,
        tipoEvento: String,
        datosExtra: Map<String, String>? = null // NUEVO PARÁMETRO
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