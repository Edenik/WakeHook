package ai.wakehook.app.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.domain.dayBit
import kotlinx.coroutines.launch
import java.time.DayOfWeek

class AlarmEditViewModel(
    private val repo: AlarmRepository,
    private val scheduler: AlarmScheduler,
    /** Fired after a save, so a two-way sync can pick it up promptly. No-op by default. */
    private val onMutated: () -> Unit = {},
) : ViewModel() {
    suspend fun load(id: String?): Alarm = id?.let { repo.get(it) } ?: Alarm()

    fun toggleDay(alarm: Alarm, day: DayOfWeek): Alarm =
        alarm.copy(repeatDays = alarm.repeatDays xor dayBit(day))

    fun save(alarm: Alarm) = viewModelScope.launch {
        repo.upsert(alarm)
        scheduler.cancel(alarm.id)
        if (alarm.enabled) scheduler.schedule(alarm)
        onMutated()
    }
}
