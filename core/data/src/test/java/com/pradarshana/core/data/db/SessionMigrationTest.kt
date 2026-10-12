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

    @Test
    fun version2KeepsSessionsWhenDeletedTableIsAdded() {
        helper.createDatabase("migrate-v2.db", 2).apply {
            execSQL(
                """INSERT INTO sessions (id, target_count, completed_count, start_time, end_time, status,
                   status_updated_at, status_updated_by, last_updated_time, origin_device_id, local_version,
                   synced_version, title)
                   VALUES ('s1', 11, 11, 1, 2, 'COMPLETED', 2, 'phone', 2, 'phone', 1, 1, 'Temple')""",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate("migrate-v2.db", 3, true, PradarshanaDatabase.MIGRATION_2_3)
        migrated.query("SELECT title, completed_count FROM sessions WHERE id = 's1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Temple", cursor.getString(0))
            assertEquals(11, cursor.getInt(1))
        }
        migrated.query("SELECT COUNT(*) FROM deleted_sessions").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(0, cursor.getInt(0))
        }
        migrated.close()
    }

    @Test
    fun version3SessionsStayPradarshanaWhenKindIsAdded() {
        helper.createDatabase("migrate-v3.db", 3).apply {
            execSQL(
                """INSERT INTO sessions (id, target_count, completed_count, start_time, end_time, status,
                   status_updated_at, status_updated_by, last_updated_time, origin_device_id, local_version,
                   synced_version, title)
                   VALUES ('s1', 108, 108, 1, 2, 'COMPLETED', 2, 'phone', 2, 'phone', 1, 1, 'Temple')""",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate("migrate-v3.db", 4, true, PradarshanaDatabase.MIGRATION_3_4)
        migrated.query("SELECT kind, title, completed_count FROM sessions WHERE id = 's1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("PRADARSHANA", cursor.getString(0))
            assertEquals("Temple", cursor.getString(1))
            assertEquals(108, cursor.getInt(2))
        }
        migrated.close()
    }

    @Test
    fun version4SessionsKeepTheirHistoryWhenPauseTimeIsAdded() {
        helper.createDatabase("migrate-v4.db", 4).apply {
            execSQL(
                """INSERT INTO sessions (id, target_count, completed_count, start_time, end_time, status,
                   status_updated_at, status_updated_by, last_updated_time, origin_device_id, local_version,
                   synced_version, title, kind)
                   VALUES ('s1', 108, 108, 1, 61, 'COMPLETED', 61, 'phone', 61, 'phone', 1, 1, 'Temple', 'PRADARSHANA')""",
            )
            close()
        }
        val migrated = helper.runMigrationsAndValidate("migrate-v4.db", 5, true, PradarshanaDatabase.MIGRATION_4_5)
        migrated.query("SELECT title, completed_count, paused_millis FROM sessions WHERE id = 's1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Temple", cursor.getString(0))
            assertEquals(108, cursor.getInt(1))
            assertEquals(0, cursor.getLong(2))
        }
        migrated.close()
    }

    private companion object {
        const val DB_NAME = "migrate-v1.db"
    }
}
