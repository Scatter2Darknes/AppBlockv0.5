import java.util.Calendar

data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun isValid(): Boolean = (startHour in 0..23 && startMinute in 0..59 &&
            endHour   in 0..23 && endMinute   in 0..59 &&
            (startHour < endHour ||
                    (startHour == endHour && startMinute < endMinute)))

    /** Returns true if `cal` falls between start and end (inclusive of start, exclusive of end). */
    fun isWithinRange(cal: Calendar): Boolean {
        val nowMinutes  = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes   = endHour * 60 + endMinute
        return nowMinutes in startMinutes until endMinutes
    }
}
