package ai.wakehook.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.domain.Alarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repo: AlarmRepository,
    private val scheduler: AlarmScheduler,
) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = repo.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(alarm: Alarm) = viewModelScope.launch {
        val updated = alarm.copy(enabled = !alarm.enabled)
        repo.upsert(updated)
        scheduler.cancel(updated.id)
        if (updated.enabled) scheduler.schedule(updated)
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id)
        scheduler.cancel(id)
    }
}
