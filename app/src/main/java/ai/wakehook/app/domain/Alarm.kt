package ai.wakehook.app.domain

import java.util.UUID

data class Alarm(
    val id: String = UUID.randomUUID().toString(),
    val label: String = "",
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: Int = 0,   // bitmask; 0 = one-time
    val enabled: Boolean = true,
) {
    val isRecurring: Boolean get() = repeatDays != 0
}
