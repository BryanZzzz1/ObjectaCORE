
package com.project.objectacore

import kotlinx.serialization.Serializable

@Serializable
data class ObjetoActivoOid(
    val serial_id: String,
    val codigo_numerico: Int, // El ID de 25 bits
    val nombre: String,
    val notas: String? = null
)