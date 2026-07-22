package com.project.objectacore.data.local

import androidx.room.TypeConverter

class Convertidores {

    @TypeConverter
    fun deListaAString(lista: List<String>): String {
        // Empaqueta la lista uniendo los textos con un separador raro
        return lista.joinToString(separator = "|~|")
    }

    @TypeConverter
    fun deStringALista(datos: String): List<String> {
        // Si no hay datos, devuelve una lista vacía
        if (datos.isEmpty()) return emptyList()
        // Desempaqueta el texto de vuelta a una lista
        return datos.split("|~|")
    }
}