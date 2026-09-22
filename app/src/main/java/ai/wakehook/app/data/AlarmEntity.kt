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
)

fun AlarmEntity.toDomain() = Alarm(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays,
    dates = if (dates.isBlank()) emptyList() else dates.split(",").map(LocalDate::parse),
    enabled = enabled,
)

fun Alarm.toEntity() = AlarmEntity(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays,
    dates = dates.joinToString(",") { it.toString() },
    enabled = enabled,
)
