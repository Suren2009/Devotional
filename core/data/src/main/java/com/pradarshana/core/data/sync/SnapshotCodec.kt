package com.pradarshana.core.data.sync

import com.google.android.gms.wearable.DataMap
import com.pradarshana.core.model.SessionDetails
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionSnapshot
import com.pradarshana.core.model.SessionStatus

/** (De)serializes a [SessionSnapshot] to a Data Layer [DataMap]. */
object SnapshotCodec {
    const val SCHEMA_VERSION = 1

    private const val KEY_SCHEMA = "schema"
    private const val KEY_SOURCE = "source_device"
    private const val KEY_ID = "session_id"
    private const val KEY_TARGET = "target_count"
    private const val KEY_COMPLETED = "completed_count"
    private const val KEY_REMAINING = "remaining_count"
    private const val KEY_START = "start_time"
    private const val KEY_END = "end_time"
    private const val KEY_STATUS = "status"
    private const val KEY_STATUS_AT = "status_updated_at"
    private const val KEY_STATUS_BY = "status_updated_by"
    private const val KEY_UPDATED = "last_updated_time"
    private const val KEY_ORIGIN = "origin_device"
    private const val KEY_COUNT_DEVICES = "count_devices"
    private const val KEY_COUNT_VALUES = "count_values"
    private const val KEY_TITLE = "title"
    private const val KEY_HAS_LOCATION = "has_location"
    private const val KEY_LATITUDE = "latitude"
    private const val KEY_LONGITUDE = "longitude"
    private const val KEY_KIND = "kind"
    private const val KEY_PAUSED = "paused_millis"

    fun write(map: DataMap, snapshot: SessionSnapshot, sourceDeviceId: String) {
        map.putInt(KEY_SCHEMA, SCHEMA_VERSION)
        map.putString(KEY_SOURCE, sourceDeviceId)
        map.putString(KEY_ID, snapshot.sessionId)
        map.putInt(KEY_TARGET, snapshot.targetCount)
        // Derived values are included for readability/debugging; receivers recompute them.
        map.putInt(KEY_COMPLETED, snapshot.completedCount)
        map.putInt(KEY_REMAINING, snapshot.remainingCount)
        map.putLong(KEY_START, snapshot.startTime)
        map.putLong(KEY_END, snapshot.endTime ?: -1L)
        map.putString(KEY_STATUS, snapshot.status.name)
        map.putLong(KEY_STATUS_AT, snapshot.statusUpdatedAt)
        map.putString(KEY_STATUS_BY, snapshot.statusUpdatedBy)
        map.putLong(KEY_UPDATED, snapshot.lastUpdatedTime)
        map.putString(KEY_ORIGIN, snapshot.originDeviceId)
        val entries = snapshot.deviceCounts.entries.sortedBy { it.key }
        map.putStringArray(KEY_COUNT_DEVICES, entries.map { it.key }.toTypedArray())
        map.putIntegerArrayList(KEY_COUNT_VALUES, ArrayList(entries.map { it.value }))
        map.putString(KEY_TITLE, snapshot.title)
        val latitude = snapshot.latitude
        val longitude = snapshot.longitude
        map.putBoolean(KEY_HAS_LOCATION, latitude != null && longitude != null)
        if (latitude != null && longitude != null) {
            map.putDouble(KEY_LATITUDE, latitude)
            map.putDouble(KEY_LONGITUDE, longitude)
        }
        map.putString(KEY_KIND, snapshot.kind.name)
        map.putLong(KEY_PAUSED, snapshot.pausedMillis)
    }

    /** Returns null for malformed or newer-schema payloads instead of throwing. */
    fun read(map: DataMap): RemoteSnapshot? = runCatching {
        if (map.getInt(KEY_SCHEMA, 0) != SCHEMA_VERSION) return null
        val devices = map.getStringArray(KEY_COUNT_DEVICES) ?: return null
        val values = map.getIntegerArrayList(KEY_COUNT_VALUES) ?: return null
        if (devices.size != values.size) return null
        val endTime = map.getLong(KEY_END, -1L).takeIf { it >= 0 }
        val (latitude, longitude) = if (map.getBoolean(KEY_HAS_LOCATION, false)) {
            SessionDetails.normalizeLocation(map.getDouble(KEY_LATITUDE), map.getDouble(KEY_LONGITUDE))
        } else {
            null to null
        }
        RemoteSnapshot(
            sourceDeviceId = map.getString(KEY_SOURCE) ?: return null,
            snapshot = SessionSnapshot(
                sessionId = map.getString(KEY_ID) ?: return null,
                targetCount = map.getInt(KEY_TARGET),
                startTime = map.getLong(KEY_START),
                endTime = endTime,
                status = SessionStatus.valueOf(map.getString(KEY_STATUS) ?: return null),
                statusUpdatedAt = map.getLong(KEY_STATUS_AT),
                statusUpdatedBy = map.getString(KEY_STATUS_BY) ?: return null,
                lastUpdatedTime = map.getLong(KEY_UPDATED),
                originDeviceId = map.getString(KEY_ORIGIN) ?: return null,
                deviceCounts = devices.zip(values).toMap(),
                title = SessionDetails.normalizeTitle(map.getString(KEY_TITLE) ?: ""),
                latitude = latitude,
                longitude = longitude,
                kind = SessionKind.fromStored(map.getString(KEY_KIND)),
                pausedMillis = if (map.containsKey(KEY_PAUSED)) map.getLong(KEY_PAUSED) else 0L,
            ),
        )
    }.getOrNull()
}
