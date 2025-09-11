package digital.tonima.mydiary.utils

import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

fun getLocalDateFromFile(file: File): LocalDate? {
    return try {
        val timestamp = file.name.removePrefix("entry_").removeSuffix(".txt").toLong()
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        Log.e("DateFormat", "Error parsing date from filename: ${file.name}", e)
        null
    }
}

fun formatTimestampToHourAndMinute(filename: String): String {
    return try {
        val timestamp = filename.removePrefix("entry_").removeSuffix(".txt").toLong()
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (e: Exception) {
        "--:--"
    }
}
