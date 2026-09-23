package ai.wakehook.app.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ai.wakehook.app.alarm.AlarmScheduler
import ai.wakehook.app.data.AlarmRepository
import ai.wakehook.app.domain.Alarm
import ai.wakehook.app.sync.TombstoneStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val repo: AlarmRepository,
    private val scheduler: AlarmScheduler,
    /** Records local deletes so a two-way sync doesn't resurrect them from a stale remote copy. */
    private val tombstones: TombstoneStore,
    /** Fired after any local mutation, so a two-way sync can pick it up promptly. No-op by default. */
    private val onMutated: () -> Unit = {},
) : ViewModel() {
    val alarms: StateFlow<List<Alarm>> = repo.observeAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggle(alarm: Alarm) = viewModelScope.launch {
        val updated = alarm.copy(enabled = !alarm.enabled)
        repo.upsert(updated)
        scheduler.cancel(updated.id)
        if (updated.enabled) scheduler.schedule(updated)
        onMutated()
    }

    fun delete(id: String) = viewModelScope.launch {
        repo.delete(id)
        scheduler.cancel(id)
        tombstones.add(id)
        onMutated()
    }
}
