package com.project.objectacore.engine.vision

import android.content.Context
import android.util.Log
import com.project.objectacore.data.remote.SupabaseManager
import com.project.objectacore.data.local.AppDatabase

object IdentificadorHibrido {

    /**
     * Busca un OID (código numérico escaneado con OpenCV) en Nube y luego en Local.
     */
    suspend fun procesarEscaneoOid(codigoEscaneado: Int, context: Context) {
        try {
            // 1. INTENTAR BUSCAR EN SUPABASE PRIMERO
            val objetoWeb = SupabaseManager.obtenerObjetoOidPorCodigo(codigoEscaneado)
            if (objetoWeb != null) {
                Log.d("Identificador", "✅ OID encontrado en la Nube: ${objetoWeb.nombre}")
                return
            }
        } catch (e: Exception) {
            Log.e("Identificador", "⚠️ Fallo la conexión web o no existe, buscando OID en local...")
        }

        // 2. SI FALLA LA WEB O NO SE ENCUENTRA, BUSCAR EN SQLITE
        try {
            val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
            val objetoLocal = dao.buscarOidPorCodigo(codigoEscaneado)

            if (objetoLocal != null) {
                Log.d("Identificador", "✅ OID encontrado LOCALMENTE: ${objetoLocal.nombre}")
            } else {
                Log.w("Identificador", "❌ OID no registrado en ninguna base de datos.")
            }
        } catch (e: Exception) {
            Log.e("Identificador", "Error grave consultando SQLite OID", e)
        }
    }

    /**
     * Busca un QR o texto clásico en Nube y luego en Local.
     */
    suspend fun procesarEscaneoQr(textoEscaneado: String, context: Context) {
        try {
            // 1. INTENTAR BUSCAR EN SUPABASE
            val objetoWeb = SupabaseManager.obtenerObjetoPorSerial(textoEscaneado)
            if (objetoWeb != null) {
                Log.d("Identificador", "✅ QR encontrado en la Nube: ${objetoWeb.nombre}")
                return
            }
        } catch (e: Exception) {
            Log.e("Identificador", "⚠️ Fallo la conexión web o no existe, buscando QR en local...")
        }

        // 2. BUSCAR EN SQLITE
        try {
            val dao = AppDatabase.obtenerBaseDatos(context).inventarioDao()
            val objetoLocal = dao.buscarClasicoPorId(textoEscaneado)

            if (objetoLocal != null) {
                Log.d("Identificador", "✅ QR encontrado LOCALMENTE: ${objetoLocal.nombre}")
            } else {
                Log.w("Identificador", "❌ QR no registrado en ninguna base de datos.")
            }
        } catch (e: Exception) {
            Log.e("Identificador", "Error grave consultando SQLite QR", e)
        }
    }
}