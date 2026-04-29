package co.edu.unipiloto.fuelcontrol.laboratories

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Encomienda(
    val id: Long = 0,
    val remitente: String,
    val destinatario: String,
    val direccionDestino: String,
    val descripcion: String,
    val peso: Double,
    val fecha: String
)

class EncomendaDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "encomiendas.db"
        const val DATABASE_VERSION = 1
        const val TABLE = "encomiendas"
        const val COL_ID = "id"
        const val COL_REMITENTE = "remitente"
        const val COL_DESTINATARIO = "destinatario"
        const val COL_DIRECCION = "direccion_destino"
        const val COL_DESCRIPCION = "descripcion"
        const val COL_PESO = "peso"
        const val COL_FECHA = "fecha"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_REMITENTE TEXT NOT NULL,
                $COL_DESTINATARIO TEXT NOT NULL,
                $COL_DIRECCION TEXT NOT NULL,
                $COL_DESCRIPCION TEXT NOT NULL,
                $COL_PESO REAL NOT NULL,
                $COL_FECHA TEXT NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    fun insertar(e: Encomienda): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_REMITENTE, e.remitente)
            put(COL_DESTINATARIO, e.destinatario)
            put(COL_DIRECCION, e.direccionDestino)
            put(COL_DESCRIPCION, e.descripcion)
            put(COL_PESO, e.peso)
            put(COL_FECHA, e.fecha)
        }
        return db.insert(TABLE, null, values)
    }

    fun consultarTodas(): List<Encomienda> {
        val lista = mutableListOf<Encomienda>()
        val db = readableDatabase
        val cursor = db.query(TABLE, null, null, null, null, null, "$COL_ID DESC")
        with(cursor) {
            while (moveToNext()) {
                lista.add(
                    Encomienda(
                        id = getLong(getColumnIndexOrThrow(COL_ID)),
                        remitente = getString(getColumnIndexOrThrow(COL_REMITENTE)),
                        destinatario = getString(getColumnIndexOrThrow(COL_DESTINATARIO)),
                        direccionDestino = getString(getColumnIndexOrThrow(COL_DIRECCION)),
                        descripcion = getString(getColumnIndexOrThrow(COL_DESCRIPCION)),
                        peso = getDouble(getColumnIndexOrThrow(COL_PESO)),
                        fecha = getString(getColumnIndexOrThrow(COL_FECHA))
                    )
                )
            }
            close()
        }
        return lista
    }
}