package com.pradarshana.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus

@Database(
    entities = [
        SessionEntity::class,
        DeviceCountEntity::class,
        CounterOperationEntity::class,
        MetadataEntity::class,
        DeletedSessionEntity::class,
    ],
    version = PradarshanaDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PradarshanaDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao

    companion object {
        const val VERSION = 5
        const val NAME = "pradarshana.db"

        /**
         * Adds the optional title and the place where the session was started.
         * Existing rows stay in history with a blank title and no coordinates.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN title TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN longitude REAL")
            }
        }

        /**
         * Explicit migrations for every schema change. History must never be lost on upgrade, so
         * destructive fallback is deliberately not enabled.
         */
        /** Records which finished sessions the user removed, without touching the ones they kept. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `deleted_sessions` (`id` TEXT NOT NULL, `deleted_at` INTEGER NOT NULL, PRIMARY KEY(`id`))",
                )
            }
        }

        /** Existing walks stay Pradarshana. New sessions can also be chanting. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN kind TEXT NOT NULL DEFAULT 'PRADARSHANA'")
            }
        }

        /** Older sessions have no recorded pauses, so their total time stays start to end. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN paused_millis INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)

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

    @TypeConverter
    fun kindToString(kind: SessionKind): String = kind.name

    @TypeConverter
    fun stringToKind(value: String): SessionKind = SessionKind.fromStored(value)
}
