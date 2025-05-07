data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun isValid(): Boolean {
        return (startHour in 0..23 && startMinute in 0..59 &&
                endHour in 0..23 && endMinute in 0..59 &&
                (startHour < endHour || (startHour == endHour && startMinute < endMinute)))
    }
}