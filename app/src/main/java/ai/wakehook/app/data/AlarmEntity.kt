package ai.wakehook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import ai.wakehook.app.domain.Alarm
import java.time.LocalDate

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,
    val dates: String,   // ISO yyyy-MM-dd joined by ","; "" = none
    val enabled: Boolean,
    val source: String,
    val version: Long,
    val ackState: String,
    val ackAt: String,
    val ackError: String,
    val snoozedUntil: Long?,
)

fun AlarmEntity.toDomain() = Alarm(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays,
    dates = if (dates.isBlank()) emptyList() else dates.split(",").map(LocalDate::parse),
    enabled = enabled,
    source = source,
    version = version,
    ackState = ackState,
    ackAt = ackAt,
    ackError = ackError,
    snoozedUntil = snoozedUntil,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays,
    dates = dates.joinToString(",") { it.toString() },
    enabled = enabled,
    source = source,
    version = version,
    ackState = ackState,
    ackAt = ackAt,
    ackError = ackError,
    snoozedUntil = snoozedUntil,
)
