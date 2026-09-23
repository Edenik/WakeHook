package ai.wakehook.app.domain

import java.time.LocalDate
import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: Int = 0,   // bitmask; 0 = one-time
    val dates: List<LocalDate> = emptyList(),
    val enabled: Boolean = true,
    val source: String = "local",
    val version: Long = 0,
    val ackState: String = "",
    val ackAt: String = "",
    val ackError: String = "",
    val snoozedUntil: Long? = null,
) {
    val isRecurring: Boolean get() = repeatDays != 0
    val isDateBased: Boolean get() = dates.isNotEmpty()
}
