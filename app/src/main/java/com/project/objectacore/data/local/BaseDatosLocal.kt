package com.project.objectacore.data.local

import android.content.Context
import androidx.room.*


// 1. LAS TABLAS (Entidades Espejo)
@Entity(tableName = "objetos_locales")
data class ObjetoLocal(
    @PrimaryKey val serial_id: String,
    val nombre: String,
    val campos_dinamicos: List<String> // <- NUEVO: Lista para el formulario dinámico
)

@Entity(tableName = "objetos_oid_locales")
data class ObjetoOidLocal(
    @PrimaryKey val serial_id: String,
    val codigo_numerico: Int,
    val nombre: String,
    val ruta_archivo_local: String? = null,
    val campos_dinamicos: List<String> // <- NUEVO: Lista para el formulario dinámico
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

    // --- FUNCIONES PARA EL ESCÁNER ---

    // Búsqueda de Clásicos (QR/Texto) por su Serial en SQLite
    @Query("SELECT * FROM objetos_locales WHERE serial_id = :serialId LIMIT 1")
    suspend fun buscarClasicoPorId(serialId: String): ObjetoLocal?

    // Búsqueda de OIDs matemáticos por su código numérico escaneado en SQLite
    @Query("SELECT * FROM objetos_oid_locales WHERE codigo_numerico = :codigo LIMIT 1")
    suspend fun buscarOidPorCodigo(codigo: Int): ObjetoOidLocal?
}

// 3. LA BASE DE DATOS (SQLite)
// CAMBIO 1: Incrementamos a version = 2
// CAMBIO 2: Declaramos el TypeConverter
@Database(entities = [ObjetoLocal::class, ObjetoOidLocal::class], version = 2, exportSchema = false)
@TypeConverters(Convertidores::class)
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
                )
                    // CAMBIO 3: Evita crashes reseteando la tabla al detectar la nueva columna
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}