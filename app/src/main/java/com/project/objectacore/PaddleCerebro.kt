package com.project.objectacore

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig


object PaddleCerebro {
    private var ocr: OCR? = null
    var estaListo = false

    fun inicializar(context: Context) {
        if (ocr != null) return

        try {
            ocr = OCR(context)
            val config = OcrConfig().apply {
                modelPath = "models" // Busca automáticamente en assets/models/
                isRunDet = true
                isRunCls = true
                isRunRec = true
            }

            // Inicialización síncrona
            val result = ocr?.initModelSync(config)
            if (result?.isSuccess == true) {
                estaListo = true
                Log.d("PADDLE_TACTICO", "Motor PaddleOCR cargado con éxito.")
            } else {
                Log.e("PADDLE_TACTICO", "Fallo al inicializar Paddle.")
            }
        } catch (e: Exception) {
            Log.e("PADDLE_TACTICO", "Error crítico en inicialización", e)
        }
    }

    fun reconocerTexto(bitmap: Bitmap): String {
        if (!estaListo || ocr == null) return ""

        return try {
            // Disparo del OCR (nos devuelve una caja fuerte tipo 'Result')
            val resultado = ocr!!.runSync(bitmap)

            // Abrimos la caja con getOrNull() y extraemos el texto simple
            resultado.getOrNull()?.simpleText ?: ""
        } catch (e: Exception) {
            Log.e("PADDLE_TACTICO", "Error procesando imagen", e)
            ""
        }
    }
}