package com.pradarshana.core.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus

@Entity(
    tableName = "sessions",
    indices = [Index("status"), Index("start_time")],
)
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "target_count") val targetCount: Int,
    /** Cached merged total of [DeviceCountEntity] rows, capped at [targetCount]. */
    @ColumnInfo(name = "completed_count") val completedCount: Int,
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long?,
    val status: SessionStatus,
    @ColumnInfo(name = "status_updated_at") val statusUpdatedAt: Long,
    @ColumnInfo(name = "status_updated_by") val statusUpdatedBy: String,
    @ColumnInfo(name = "last_updated_time") val lastUpdatedTime: Long,
    @ColumnInfo(name = "origin_device_id") val originDeviceId: String,
    /** Incremented on every local state change; compared to [syncedVersion] to find unsent changes. */
    @ColumnInfo(name = "local_version") val localVersion: Long,
    @ColumnInfo(name = "synced_version") val syncedVersion: Long,
    /** Empty string when the session was not named. */
    @ColumnInfo(name = "title", defaultValue = "''") val title: String,
    val latitude: Double?,
    val longitude: Double?,
    /** Pradarshana or chanting. Older sessions are pradakshina walks. */
    @ColumnInfo(name = "kind", defaultValue = "'PRADARSHANA'") val kind: SessionKind,
    /** Closed pauses only. The pause still running is measured from [statusUpdatedAt]. */
    @ColumnInfo(name = "paused_millis", defaultValue = "0") val pausedMillis: Long = 0,
)

/** One G-Counter entry: the number of rounds counted on [deviceId] for a session. */
@Entity(
    tableName = "session_device_counts",
    primaryKeys = ["session_id", "device_id"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class DeviceCountEntity(
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val count: Int,
)

/** Local log of every round counted on this device; [opId] is `sessionId:deviceId:sequence`. */
@Entity(
    tableName = "counter_operations",
    indices = [Index("session_id")],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["session_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class CounterOperationEntity(
    @PrimaryKey @ColumnInfo(name = "op_id") val opId: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "device_id") val deviceId: String,
    val sequence: Int,
    val timestamp: Long,
)

@Entity(tableName = "metadata")
data class MetadataEntity(
    @PrimaryKey val key: String,
    val value: Long,
)

/** A finished session the user removed. Stops a synced copy from coming back. */
@Entity(tableName = "deleted_sessions")
data class DeletedSessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long,
)
