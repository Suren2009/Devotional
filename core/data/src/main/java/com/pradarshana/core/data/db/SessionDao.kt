package com.pradarshana.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: String): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY start_time DESC, id DESC LIMIT 1")
    fun observeOngoingSession(): Flow<SessionEntity?>

    @Query("SELECT * FROM sessions WHERE status IN ('COMPLETED', 'CANCELLED') ORDER BY start_time DESC, id DESC")
    fun observeHistory(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE', 'PAUSED') ORDER BY start_time DESC, id DESC")
    suspend fun getOngoingSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE local_version > synced_version")
    suspend fun getUnsyncedSessions(): List<SessionEntity>

    @Query("SELECT * FROM sessions WHERE status IN ('ACTIVE', 'PAUSED') OR last_updated_time >= :since")
    suspend fun getSessionsForResync(since: Long): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: SessionEntity)

    @Upsert
    suspend fun upsertSession(session: SessionEntity)

    @Query("UPDATE sessions SET synced_version = :version WHERE id = :id AND synced_version < :version")
    suspend fun markSynced(id: String, version: Long)

    @Query("SELECT id FROM sessions WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun getTerminalSessionIds(): List<String>

    @Query("DELETE FROM sessions WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun deleteTerminalSessions(): Int

    @Query("DELETE FROM sessions WHERE id = :id AND status IN ('COMPLETED', 'CANCELLED')")
    suspend fun deleteTerminalSession(id: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeleted(entry: DeletedSessionEntity)

    @Query("SELECT EXISTS(SELECT 1 FROM deleted_sessions WHERE id = :id)")
    suspend fun isDeleted(id: String): Boolean

    @Query("SELECT * FROM session_device_counts WHERE session_id = :sessionId")
    suspend fun getDeviceCounts(sessionId: String): List<DeviceCountEntity>

    @Upsert
    suspend fun upsertDeviceCounts(counts: List<DeviceCountEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOperation(operation: CounterOperationEntity)

    @Query("SELECT COUNT(*) FROM counter_operations WHERE session_id = :sessionId")
    suspend fun countOperations(sessionId: String): Int

    @Query("SELECT value FROM metadata WHERE `key` = :key")
    suspend fun getMetadata(key: String): Long?

    @Upsert
    suspend fun putMetadata(entry: MetadataEntity)
}
