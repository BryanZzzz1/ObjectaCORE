package com.project.objectacore

import kotlinx.serialization.Serializable

@Serializable
data class ObjetoActivo(
    val serial_id: String,
    val nombre: String,
    val notas: String? = null
)