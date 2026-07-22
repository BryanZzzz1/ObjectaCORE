package com.project.objectacore.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class ObjetoActivo(
    // Quitamos el @SerialName("serial_id") porque tu base de datos usa exactamente "id"
    val id: String,

    val nombre: String,

    val tipoEtiqueta: String = "QR",

    val camposDinamicos: List<String> = emptyList(),

    val estado: String = "ACTIVO",

    // El ? es crucial para que no crashee cuando en Supabase diga NULL
    val notas: String? = null,

    // Cambiamos a Long? (nullable) para absorber los NULL de la base de datos sin explotar
    val timestamp: Long? = null
)