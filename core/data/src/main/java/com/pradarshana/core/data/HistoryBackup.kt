package com.pradarshana.core.data

import com.pradarshana.core.model.SessionDetails
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionSnapshot
import com.pradarshana.core.model.SessionStatus
import org.json.JSONArray
import org.json.JSONObject

/** A history file the devotee can keep and read back after reinstalling. */
object HistoryBackup {
    const val FORMAT = 1

    data class Backup(
        val sessions: List<SessionSnapshot>,
        val deletedIds: List<String>,
        val clearedAt: Long?,
    )

    fun encode(sessions: List<SessionSnapshot>, deletedIds: List<String>, clearedAt: Long?): String {
        val root = JSONObject()
        root.put("format", FORMAT)
        if (clearedAt != null) root.put("clearedAt", clearedAt)
        root.put("deleted", JSONArray(deletedIds))
        val encoded = JSONArray()
        sessions.forEach { encoded.put(sessionJson(it)) }
        root.put("sessions", encoded)
        return root.toString()
    }

    fun decode(json: String): Backup? = runCatching {
        val root = JSONObject(json)
        if (root.optInt("format", 0) != FORMAT) return null
        val deleted = root.optJSONArray("deleted") ?: JSONArray()
        val deletedIds = buildList {
            for (index in 0 until deleted.length()) {
                val id = deleted.optString(index)
                if (id.isNotEmpty()) add(id)
            }
        }
        val sessionsJson = root.optJSONArray("sessions") ?: return null
        val sessions = buildList {
            for (index in 0 until sessionsJson.length()) {
                val session = sessionsJson.optJSONObject(index) ?: return null
                add(sessionFrom(session) ?: return null)
            }
        }
        val clearedAt = if (root.has("clearedAt")) root.getLong("clearedAt") else null
        Backup(sessions, deletedIds, clearedAt)
    }.getOrNull()

    private fun sessionJson(snapshot: SessionSnapshot): JSONObject = JSONObject().apply {
        put("sessionId", snapshot.sessionId)
        put("targetCount", snapshot.targetCount)
        put("startTime", snapshot.startTime)
        if (snapshot.endTime != null) put("endTime", snapshot.endTime)
        put("status", snapshot.status.name)
        put("statusUpdatedAt", snapshot.statusUpdatedAt)
        put("statusUpdatedBy", snapshot.statusUpdatedBy)
        put("lastUpdatedTime", snapshot.lastUpdatedTime)
        put("originDeviceId", snapshot.originDeviceId)
        put("deviceCounts", JSONObject(snapshot.deviceCounts))
        put("deviceUndos", JSONObject(snapshot.deviceUndos))
        put("title", snapshot.title)
        if (snapshot.latitude != null && snapshot.longitude != null) {
            put("latitude", snapshot.latitude)
            put("longitude", snapshot.longitude)
        }
        put("kind", snapshot.kind.name)
        put("pausedMillis", snapshot.pausedMillis)
        put("mantra", snapshot.mantra)
    }

    private fun sessionFrom(json: JSONObject): SessionSnapshot? = runCatching {
        val (latitude, longitude) = if (json.has("latitude") && json.has("longitude")) {
            SessionDetails.normalizeLocation(json.getDouble("latitude"), json.getDouble("longitude"))
        } else {
            null to null
        }
        SessionSnapshot(
            sessionId = json.getString("sessionId"),
            targetCount = json.getInt("targetCount"),
            startTime = json.getLong("startTime"),
            endTime = if (json.has("endTime")) json.getLong("endTime") else null,
            status = SessionStatus.valueOf(json.getString("status")),
            statusUpdatedAt = json.getLong("statusUpdatedAt"),
            statusUpdatedBy = json.getString("statusUpdatedBy"),
            lastUpdatedTime = json.getLong("lastUpdatedTime"),
            originDeviceId = json.getString("originDeviceId"),
            deviceCounts = intMap(json.getJSONObject("deviceCounts")),
            title = SessionDetails.normalizeTitle(json.optString("title")),
            latitude = latitude,
            longitude = longitude,
            kind = SessionKind.fromStored(json.optString("kind")),
            pausedMillis = json.optLong("pausedMillis", 0L),
            deviceUndos = if (json.has("deviceUndos")) intMap(json.getJSONObject("deviceUndos")) else emptyMap(),
            mantra = SessionDetails.normalizeMantra(json.optString("mantra")),
        )
    }.getOrNull()

    private fun intMap(json: JSONObject): Map<String, Int> =
        json.keys().asSequence().associateWith { key -> json.getInt(key) }
}
