package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm

/** In-memory [AgentNotifier] test double that records every call. */
class FakeAgentNotifier : AgentNotifier {
    var added: List<Alarm> = emptyList()
        private set
    var changed: List<Alarm> = emptyList()
        private set
    var callCount: Int = 0
        private set

    override fun notifyAgentActivity(added: List<Alarm>, changed: List<Alarm>) {
        this.added = added
        this.changed = changed
        callCount++
    }
}
