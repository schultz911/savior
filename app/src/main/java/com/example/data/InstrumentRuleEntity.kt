package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Locale

@Entity(
    tableName = "instrument_rules",
    indices = [Index(value = ["last4"])]
)
data class InstrumentRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String = "",
    val cardOrAccountName: String = "",
    val last4: String = "",
    val markers: String = "", // Stored newline-separated to support complex regex patterns
    val createdAt: Long = System.currentTimeMillis()
) {
    @androidx.room.Ignore
    @Volatile
    private var cachedMarkers: List<String>? = null

    fun getMarkerList(): List<String> {
        if (markers.isBlank()) return emptyList()
        val cached = cachedMarkers
        if (cached != null) return cached
        val parsed = markers.split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        cachedMarkers = parsed
        return parsed
    }

    val canonicalName: String
        get() {
            val parts = mutableListOf<String>()
            val cleanBank = bankName.trim()
            val cleanCard = cardOrAccountName.trim()
            if (cleanBank.isNotBlank()) parts.add(cleanBank)
            if (cleanCard.isNotBlank()) parts.add(cleanCard)
            val prefix = parts.joinToString(" ").trim()
            val cleanLast4 = last4.trim().uppercase(Locale.US)
            return when {
                prefix.isNotBlank() && cleanLast4.isNotBlank() -> "$prefix ••$cleanLast4"
                prefix.isNotBlank() -> prefix
                cleanLast4.isNotBlank() -> "A/c ••$cleanLast4"
                else -> "Unidentified Instrument"
            }
        }

    companion object {
        fun markersToString(list: List<String>): String {
            return list.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
        }
    }
}
