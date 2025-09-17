package digital.tonima.mydiary.utils

import android.util.Log
import digital.tonima.mydiary.database.entities.DiaryEntryEntity
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun getLocalDateFromEntity(entity: DiaryEntryEntity): LocalDate {
    return Instant.ofEpochMilli(entity.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
}

fun formatTimestampToHourAndMinute(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        Log.e("DateFormat", "Error formatting timestamp: $timestamp", e)
        "--:--"
    }
}
