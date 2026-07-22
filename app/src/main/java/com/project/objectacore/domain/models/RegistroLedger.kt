package com.project.objectacore.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RegistroLedger(
    val codigo_numerico: Int,
    val evento: String,
    val metadatos: Map<String, String>? = null // NUEVO: Carga útil flexible
)