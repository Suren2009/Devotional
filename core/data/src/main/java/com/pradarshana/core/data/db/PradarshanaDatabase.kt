package com.pradarshana.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import com.pradarshana.core.model.SessionStatus

@Database(
    entities = [
        SessionEntity::class,
        DeviceCountEntity::class,
        CounterOperationEntity::class,
        MetadataEntity::class,
    ],
    version = PradarshanaDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PradarshanaDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        const val VERSION = 1
        const val NAME = "pradarshana.db"

        /**
         * Explicit migrations for every schema change. History must never be lost on upgrade, so
         * destructive fallback is deliberately not enabled.
         */
        val MIGRATIONS: Array<Migration> = arrayOf()

        fun create(context: Context, name: String? = NAME): PradarshanaDatabase {
            val builder = if (name == null) {
                Room.inMemoryDatabaseBuilder(context, PradarshanaDatabase::class.java)
            } else {
                Room.databaseBuilder(context, PradarshanaDatabase::class.java, name)
            }
            return builder.addMigrations(*MIGRATIONS).build()
        }
    }
}

class Converters {
    @TypeConverter
    fun statusToString(status: SessionStatus): String = status.name

    @TypeConverter
    fun stringToStatus(value: String): SessionStatus = SessionStatus.valueOf(value)
}
