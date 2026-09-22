package ai.wakehook.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import ai.wakehook.app.domain.Alarm

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey val id: String,
    val label: String,
    val hour: Int,
    val minute: Int,
    val repeatDays: Int,
    val enabled: Boolean,
)

fun AlarmEntity.toDomain() = Alarm(id, label, hour, minute, repeatDays, enabled)
fun Alarm.toEntity() = AlarmEntity(id, label, hour, minute, repeatDays, enabled)
