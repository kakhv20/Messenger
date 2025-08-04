package ge.kakhvlediani.messenger.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtils {

    fun formatMessageTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60 * 1000 -> "${diff / 1000} sec"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)} min"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)} hour"
            else -> {
                val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}