package com.project.objectacore

import android.content.Context
import androidx.room.*

// 1. LAS TABLAS (Entidades Espejo)
@Entity(tableName = "objetos_locales")
data class ObjetoLocal(
    @PrimaryKey val serial_id: String,
    val nombre: String
)

@Entity(tableName = "objetos_oid_locales")
data class ObjetoOidLocal(
    @PrimaryKey val serial_id: String,
    val codigo_numerico: Int,
    val nombre: String,
    val ruta_archivo_local: String? = null // <- ESTA ES LA LÍNEA NUEVA
)

@Dao
interface InventarioDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarObjeto(objeto: ObjetoLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarObjetoOid(objetoOid: ObjetoOidLocal)

    @Query("SELECT * FROM objetos_locales")
    suspend fun obtenerObjetosLocales(): List<ObjetoLocal>

    @Query("SELECT * FROM objetos_oid_locales")
    suspend fun obtenerOidsLocales(): List<ObjetoOidLocal>

    // --- NUEVAS FUNCIONES PARA EL ESCÁNER ---

    // Búsqueda de Clásicos (QR/Texto) por su Serial en SQLite
    @Query("SELECT * FROM objetos_locales WHERE serial_id = :serialId LIMIT 1")
    suspend fun buscarClasicoPorId(serialId: String): ObjetoLocal?

    // Búsqueda de OIDs matemáticos por su código numérico escaneado en SQLite
    @Query("SELECT * FROM objetos_oid_locales WHERE codigo_numerico = :codigo LIMIT 1")
    suspend fun buscarOidPorCodigo(codigo: Int): ObjetoOidLocal?
}

// 3. LA BASE DE DATOS (SQLite)
@Database(entities = [ObjetoLocal::class, ObjetoOidLocal::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun inventarioDao(): InventarioDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun obtenerBaseDatos(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "objectacore_boveda_local.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}