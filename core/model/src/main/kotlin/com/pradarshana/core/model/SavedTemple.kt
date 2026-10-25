package com.pradarshana.core.model

/** A named starting place for automatic Pradarshana counting. */
data class SavedTemple(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
) {
    init {
        require(id.isNotBlank()) { "id must not be blank" }
        require(name == SessionDetails.normalizeTitle(name) && name.isNotEmpty()) { "name must be normalized" }
        val (lat, lng) = SessionDetails.normalizeLocation(latitude, longitude)
        require(lat == latitude && lng == longitude) { "location must be valid" }
    }
}

object SavedTemples {
    const val MAX = 12

    fun encode(temples: List<SavedTemple>): String =
        temples.joinToString("\n") { "${it.id}\t${it.name}\t${it.latitude}\t${it.longitude}" }

    fun decode(raw: String?): List<SavedTemple> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence().mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size != 4) return@mapNotNull null
            val name = SessionDetails.normalizeTitle(parts[1])
            val lat = parts[2].toDoubleOrNull()
            val lng = parts[3].toDoubleOrNull()
            val (latitude, longitude) = SessionDetails.normalizeLocation(lat, lng)
            if (parts[0].isBlank() || name.isEmpty() || latitude == null || longitude == null) return@mapNotNull null
            SavedTemple(parts[0], name, latitude, longitude)
        }.take(MAX).toList()
    }
}
