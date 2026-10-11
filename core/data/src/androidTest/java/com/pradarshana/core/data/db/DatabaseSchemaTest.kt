package com.pradarshana.core.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the exported schema. When the version is bumped, add a test here that creates the
 * previous version, inserts data and runs `helper.runMigrationsAndValidate` to prove no history is lost.
 */
@RunWith(AndroidJUnit4::class)
class DatabaseSchemaTest {

    private val dbName = "schema-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PradarshanaDatabase::class.java,
    )

    @Test
    fun currentSchemaOpensWithExportedDefinition() {
        helper.createDatabase(dbName, PradarshanaDatabase.VERSION).apply {
            execSQL(
                """INSERT INTO sessions (id, target_count, completed_count, start_time, end_time, status,
                   status_updated_at, status_updated_by, last_updated_time, origin_device_id, local_version, synced_version)
                   VALUES ('s1', 108, 108, 1, 2, 'COMPLETED', 2, 'phone', 2, 'phone', 1, 1)""",
            )
            close()
        }
        val db = Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PradarshanaDatabase::class.java,
            dbName,
        ).addMigrations(*PradarshanaDatabase.MIGRATIONS).build()
        val session = runBlocking { db.sessionDao().getSession("s1") }
        assertEquals(108, session?.completedCount)
        db.close()
    }
}
