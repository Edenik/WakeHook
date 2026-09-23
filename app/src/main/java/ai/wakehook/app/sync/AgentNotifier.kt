package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm

/**
 * Notifies the user of alarms an agent added/changed remotely. The real Android implementation
 * (posting on the `agent_activity` notification channel) is added in Task 7.
 */
interface AgentNotifier {
    fun notifyAgentActivity(added: List<Alarm>, changed: List<Alarm>)
}
