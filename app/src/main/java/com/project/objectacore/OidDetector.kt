package com.project.objectacore

import android.util.Log
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters

object OidDetector {

    init {
        try {
            System.loadLibrary("opencv_java4")
        } catch (e: UnsatisfiedLinkError) {
            try {
                System.loadLibrary("opencv_java")
            } catch (e2: UnsatisfiedLinkError) {
                Log.e("OidDetector", "[SISTEMA] OpenCV no disponible.")
            }
        }
    }

    // Adaptado al nuevo Telar 6x6
    private const val MARKER_SIZE = 160 // Ajustado para 6 celdas de 20px + bordes
    private const val GRID_SIZE = 6
    private const val CELL_SIZE = 20

    fun procesarFrame(rgbaMat: Mat): Int? {
        val gray = Mat()
        val thresh = Mat()

        Imgproc.cvtColor(rgbaMat, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.adaptiveThreshold(
            gray, thresh, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY_INV, 11, 2.0
        )

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var idEncontrado: Int? = null

        for (contour in contours) {
            val contour2f = MatOfPoint2f(*contour.toArray())
            val peri = Imgproc.arcLength(contour2f, true)
            val approx = MatOfPoint2f()

            Imgproc.approxPolyDP(contour2f, approx, 0.04 * peri, true)

            // Buscar cuadrados
            if (approx.total() == 4L && Imgproc.isContourConvex(MatOfPoint(*approx.toArray()))) {
                val area = Imgproc.contourArea(approx)

                // Ignorar basuritas muy pequeñas
                if (area > 2000) {
                    val dstPoints = listOf(
                        Point(0.0, 0.0),
                        Point(MARKER_SIZE.toDouble(), 0.0),
                        Point(MARKER_SIZE.toDouble(), MARKER_SIZE.toDouble()),
                        Point(0.0, MARKER_SIZE.toDouble())
                    )

                    val srcPoints = ordenarEsquinas(approx.toList())
                    val srcMat = Converters.vector_Point2f_to_Mat(srcPoints)
                    val dstMat = Converters.vector_Point2f_to_Mat(dstPoints)

                    val perspectiveTransform = Imgproc.getPerspectiveTransform(srcMat, dstMat)
                    val correctedMarker = Mat()

                    // INTER_NEAREST preserva los bordes duros de los "hilos"
                    Imgproc.warpPerspective(gray, correctedMarker, perspectiveTransform, Size(MARKER_SIZE.toDouble(), MARKER_SIZE.toDouble()), Imgproc.INTER_NEAREST)

                    // Enviamos al decodificador del Telar
                    idEncontrado = desenredarTelar(correctedMarker)

                    correctedMarker.release()
                    srcMat.release()
                    dstMat.release()

                    if (idEncontrado != null) break
                }
            }
        }

        gray.release()
        thresh.release()
        hierarchy.release()
        return idEncontrado
    }

    private fun ordenarEsquinas(puntos: List<Point>): List<Point> {
        val ordenados = puntos.sortedBy { it.x + it.y }
        val tl = ordenados[0]
        val br = ordenados[3]
        val restantes = puntos.filter { it != tl && it != br }.sortedBy { it.y - it.x }
        return listOf(tl, restantes[0], br, restantes[1])
    }

    private fun desenredarTelar(markerMat: Mat): Int? {
        val binaryMarker = Mat()
        Imgproc.threshold(markerMat, binaryMarker, 0.0, 255.0, Imgproc.THRESH_BINARY or Imgproc.THRESH_OTSU)

        // Verificación rápida: El marco exterior debe existir y ser oscuro
        val checkMarco = Core.mean(binaryMarker.submat(0, 5, 10, MARKER_SIZE - 10)).`val`[0]
        if (checkMarco > 100) {
            binaryMarker.release()
            return null
        }

        var tramaCompleta = ""
        val offset = 20 // Desplazamiento por el marco exterior

        // Leer los 36 hilos del telar
        for (row in 0 until GRID_SIZE) {
            for (col in 0 until GRID_SIZE) {
                val startY = offset + (row * CELL_SIZE) + 5
                val endY = offset + (row * CELL_SIZE) + 15
                val startX = offset + (col * CELL_SIZE) + 5
                val endX = offset + (col * CELL_SIZE) + 15

                val cellMat = binaryMarker.submat(startY, endY, startX, endX)
                val valorPixel = Core.mean(cellMat).`val`[0]
                cellMat.release()

                // Oscuro (< 128) es 1, Claro es 0
                tramaCompleta += if (valorPixel < 128) "1" else "0"
            }
        }
        binaryMarker.release()

        // ==========================================
        // 🛡️ VALIDACIÓN CRIPTOGRÁFICA DEL TELAR
        // ==========================================

        // 1. Verificamos el Ancla Fija (Los primeros 3 bits DEBEN ser '101')
        val ancla = tramaCompleta.substring(0, 3)
        if (ancla != "101") {
            return null // Es una sombra, una ventana, o el marcador está chueco
        }

        // 2. Extraemos las partes
        val strIdBinario = tramaCompleta.substring(3, 28) // 25 bits
        val strChecksumBinario = tramaCompleta.substring(28, 36) // 8 bits finales

        return try {
            val idExtraido = strIdBinario.toInt(2)
            val checksumExtraido = strChecksumBinario.toInt(2)

            // 3. LA PRUEBA DE FUEGO: Calculamos el checksum localmente
            val checksumCalculadoLocamente = idExtraido % 255

            if (checksumExtraido == checksumCalculadoLocamente) {
                Log.i("OidDetector", "🛡️ TELAR VALIDADO CIENTÍFICAMENTE. ID: $idExtraido")
                idExtraido // ¡Éxito absoluto! Devolvemos el ID.
            } else {
                // Falso positivo. OpenCV leyó un cuadrado que no es nuestro telar.
                // Lo ignoramos silenciosamente.
                null
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}