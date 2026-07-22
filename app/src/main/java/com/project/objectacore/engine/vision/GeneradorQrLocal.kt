package com.project.objectacore.engine.vision

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object GeneradorQrLocal {

    fun exportarQrADescargas(context: Context, id: String) {
        try {
            // 1. Crear la matriz del código QR
            val size = 512
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(id, BarcodeFormat.QR_CODE, size, size)

            // 2. Convertir la matriz a una imagen (Bitmap)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            // 3. Preparar la inyección en la carpeta "Descargas" del teléfono
            val nombreArchivo = "QR_${id}.png"
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, nombreArchivo)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Crea una subcarpeta llamada "ObjectaCORE" dentro de Descargas
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ObjectaCORE")
                }
            }

            // 4. Guardar físicamente el archivo
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                Toast.makeText(context, "QR Guardado en Descargas: $nombreArchivo", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Fallo al crear archivo en Descargas", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error al forjar QR: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}