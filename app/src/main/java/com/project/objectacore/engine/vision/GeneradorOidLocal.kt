package com.project.objectacore.engine.vision

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

object GeneradorOidLocal {

    // ==========================================
    // 1. MOTOR DE TEJIDO DEL TELAR (Offline)
    // ==========================================
    fun generarOidSvg(id: Long): String {
        val gridSize = 6
        val cellSize = 20
        val borderSize = 20
        val totalSize = (gridSize * cellSize) + (borderSize * 2)

        val checksum = id % 255
        val binId = id.toString(2).padStart(25, '0')
        val binCheck = checksum.toString(2).padStart(8, '0')
        val binaryString = "101$binId$binCheck"

        val svgBuilder = StringBuilder()
        svgBuilder.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $totalSize $totalSize" width="100%" height="100%">""")
        svgBuilder.append("""<rect width="$totalSize" height="$totalSize" rx="18" fill="#0A0A0A" />""")

        val halfBorder = borderSize / 2
        val innerSize = totalSize - borderSize
        svgBuilder.append("""<rect x="$halfBorder" y="$halfBorder" width="$innerSize" height="$innerSize" rx="8" fill="#FFFFFF" />""")

        var bitIndex = 0
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val bit = binaryString[bitIndex]
                if (bit == '1') {
                    val x = borderSize + (col * cellSize)
                    val y = borderSize + (row * cellSize)

                    if (row == 0 && col == 0) {
                        svgBuilder.append("""<rect x="${x + 2}" y="${y + 2}" width="16" height="16" rx="4" fill="#0A0A0A" />""")
                        svgBuilder.append("""<circle cx="${x + 10}" cy="${y + 10}" r="3" fill="#FFFFFF" />""")
                    } else {
                        svgBuilder.append("""<rect x="${x + 2}" y="${y + 2}" width="16" height="16" rx="5" fill="#0A0A0A" />""")
                    }
                }
                bitIndex++
            }
        }
        svgBuilder.append("</svg>")
        return svgBuilder.toString()
    }

    // ==========================================
    // 2. GUARDADO EN ALMACENAMIENTO INTERNO SECRETO
    // ==========================================
    fun guardarSvgEnDispositivo(context: Context, serialId: String, svgContent: String): String? {
        return try {
            val directorio = File(context.filesDir, "MarcadoresOID_Locales")
            if (!directorio.exists()) directorio.mkdirs()

            val archivo = File(directorio, "$serialId.svg")
            FileOutputStream(archivo).use { out ->
                out.write(svgContent.toByteArray())
            }
            archivo.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // 3. EXPORTACIÓN A CARPETA PÚBLICA DE DESCARGAS
    // ==========================================
    fun exportarSvgADescargasPublicas(context: Context, rutaInterna: String, serialId: String) {
        try {
            val archivoInterno = File(rutaInterna)
            if (!archivoInterno.exists()) {
                Toast.makeText(context, "Error: El archivo original no existe.", Toast.LENGTH_SHORT).show()
                return
            }

            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "OID_$serialId.svg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/svg+xml")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ObjectaCORE")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri).use { outputStream ->
                    archivoInterno.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream!!)
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                }
                Toast.makeText(context, "Descargado en: Descargas/ObjectaCORE", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Fallo al exportar: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}