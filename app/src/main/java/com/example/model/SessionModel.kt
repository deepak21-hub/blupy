package com.example.model

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Serializable
enum class EntryKind {
    TRANSCRIPT,
    HEARD,
    ASKED,
    REPLY,
    REPORT
}

@Serializable
data class TranscriptEntry(
    val id: String = UUID.randomUUID().toString(),
    val kind: EntryKind,
    val text: String,
    val time: Long = System.currentTimeMillis(),
    val speaker: String? = null,
    val itemID: String? = null
)

@Serializable
data class BlueySession(
    val id: String = UUID.randomUUID().toString(),
    val started: Long = System.currentTimeMillis(),
    val ended: Long? = null,
    val entries: List<TranscriptEntry> = emptyList(),
    val notesPath: String? = null
) {
    val questionCount: Int
        get() = entries.count { it.kind == EntryKind.ASKED }

    val durationSeconds: Long
        get() = ((ended ?: System.currentTimeMillis()) - started) / 1000

    val summary: String
        get() {
            val firstQuestion = entries.firstOrNull { it.kind == EntryKind.ASKED && it.text.isNotBlank() }
            if (firstQuestion != null) return firstQuestion.text
            val firstSpoken = entries.firstOrNull { (it.kind == EntryKind.TRANSCRIPT || it.kind == EntryKind.HEARD) && it.text.isNotBlank() }
            return firstSpoken?.text ?: "Session started"
        }

    val displayEntries: List<TranscriptEntry>
        get() = entries.filter { it.text.isNotBlank() }.sortedBy { it.time }

    fun formattedDuration(): String {
        val s = durationSeconds
        return if (s >= 3600) {
            String.format(Locale.US, "%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
        } else {
            String.format(Locale.US, "%d:%02d", s / 60, s % 60)
        }
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return sdf.format(Date(started))
    }

    fun exportText(): String {
        val timeFmt = SimpleDateFormat("h:mm:ss a", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("Bluey session, ${formattedDate()}\n\n")
        for (entry in displayEntries) {
            val who = when (entry.kind) {
                EntryKind.TRANSCRIPT -> "Speaker ${entry.speaker ?: "?"}"
                EntryKind.HEARD -> "You"
                EntryKind.ASKED -> "You asked"
                EntryKind.REPLY -> "Bluey"
                EntryKind.REPORT -> "Research"
            }
            sb.append("[${timeFmt.format(Date(entry.time))}] $who: ${entry.text}\n")
        }
        return sb.toString()
    }
}
