package digital.tonima.mydiary.utils

import android.util.Log
import java.io.File
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

fun getLocalDateFromFile(file: File): LocalDate? {
    return try {
        val timestamp = file.name.removePrefix("entry_").removeSuffix(".txt").toLong()
        Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (e: Exception) {
        Log.e("DateFormat", "Error parsing date from filename: ${file.name}", e)
        null
    }
}

fun formatTimestamp(filename: String): String {
    return try {
        val timestamp = filename.removePrefix("entry_").removeSuffix(".txt").toLong()
        val dateFormat = DateFormat.getDateTimeInstance()
        dateFormat.format(Date(timestamp))
    } catch (e: Exception) {
        Log.e("DateFormat", "Error formatting timestamp from filename: $filename", e)
        "Data Inválida"
    }
}
