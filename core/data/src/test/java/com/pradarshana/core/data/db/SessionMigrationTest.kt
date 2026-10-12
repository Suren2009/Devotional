package com.pradarshana.core.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PradarshanaDatabase::class.java,
    )

    @Test
    fun version1SessionsGainABlankTitleAndNoLocation() {
        helper.createDatabase(DB_NAME, 1).apply {
            execSQL(
                """INSERT INTO sessions (id, target_count, completed_count, start_time, end_time, status,
                   status_updated_at, status_updated_by, last_updated_time, origin_device_id, local_version, synced_version)
                   VALUES ('s1', 108, 27, 1, NULL, 'ACTIVE', 1, 'phone', 1, 'phone', 1, 0)""",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate(DB_NAME, 2, true, PradarshanaDatabase.MIGRATION_1_2)
        migrated.query("SELECT title, latitude, longitude, completed_count FROM sessions WHERE id = 's1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("", cursor.getString(0))
            assertTrue(cursor.isNull(1))
            assertTrue(cursor.isNull(2))
            assertEquals(27, cursor.getInt(3))
        }
        migrated.close()
    }

    private companion object {
        const val DB_NAME = "migrate-v1.db"
    }
}
