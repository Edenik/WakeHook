package ai.wakehook.app.sync

import ai.wakehook.app.domain.Alarm
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AlarmMergeTest {

    @Test fun remoteOnlyNewAgentAlarm_isMergedAndAdded() {
        val remoteAlarm = Alarm(
            id = "r1",
            label = "Standup",
            hour = 9,
            minute = 0,
            source = "claude",
            version = 1,
        )

        val result = AlarmMerge.merge(
            local = emptyList(),
            remote = listOf(remoteAlarm),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(remoteAlarm)
        assertThat(result.added).containsExactly(remoteAlarm)
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isFalse()
    }

    @Test fun localOnlyAlarm_isMergedAndWritesRemote() {
        val localAlarm = Alarm(id = "l1", label = "Gym", hour = 6, minute = 45, source = "local", version = 0)

        val result = AlarmMerge.merge(
            local = listOf(localAlarm),
            remote = emptyList(),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(localAlarm)
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isTrue()
    }

    @Test fun localNewerVersion_wins_andDoesNotAppearInAddedOrChanged() {
        val local = Alarm(id = "a1", label = "Local edit", hour = 7, minute = 30, source = "local", version = 3)
        val remote = Alarm(id = "a1", label = "Remote stale", hour = 7, minute = 0, source = "claude", version = 2)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = listOf(remote),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(local)
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isTrue()
    }

    @Test fun tieOnVersion_localWins() {
        val local = Alarm(id = "a1", label = "Local", hour = 7, minute = 30, source = "local", version = 2)
        val remote = Alarm(id = "a1", label = "Remote", hour = 7, minute = 0, source = "claude", version = 2)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = listOf(remote),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(local)
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isTrue()
    }

    @Test fun remoteNewerVersion_wins_andIsChanged() {
        val local = Alarm(id = "a1", label = "Local stale", hour = 7, minute = 0, source = "local", version = 1)
        val remote = Alarm(id = "a1", label = "Remote edit", hour = 7, minute = 30, source = "claude", version = 4)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = listOf(remote),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(remote)
        assertThat(result.added).isEmpty()
        assertThat(result.changed).containsExactly(remote)
        assertThat(result.toWriteRemote).isFalse()
    }

    @Test fun remoteNewerVersion_butSourceLocal_isNotChanged() {
        // Defensive: even if a remote-sourced record somehow carries source="local",
        // added/changed must respect the source!="local" rule, not just "won from remote".
        val local = Alarm(id = "a1", label = "Local stale", hour = 7, minute = 0, source = "local", version = 1)
        val remote = Alarm(id = "a1", label = "Remote edit", hour = 7, minute = 30, source = "local", version = 4)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = listOf(remote),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(remote)
        assertThat(result.changed).isEmpty()
    }

    @Test fun tombstonedId_staysDeleted_evenIfPresentOnOtherSideWithHigherVersion() {
        val local = Alarm(id = "a1", label = "Deleted locally", hour = 7, minute = 0, source = "local", version = 1)
        val remote = Alarm(id = "a1", label = "Still on remote", hour = 8, minute = 0, source = "claude", version = 99)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = listOf(remote),
            localTombstones = setOf("a1"),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).isEmpty()
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isTrue() // remote still has it, must be removed remotely
    }

    @Test fun remoteTombstone_removesLocalAlarm_notResurrected() {
        val local = Alarm(id = "a1", label = "Local copy", hour = 7, minute = 0, source = "local", version = 5)

        val result = AlarmMerge.merge(
            local = listOf(local),
            remote = emptyList(),
            localTombstones = emptySet(),
            remoteTombstones = setOf("a1"),
        )

        assertThat(result.merged).isEmpty()
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isFalse() // remote already lacks it
    }

    @Test fun identicalSets_toWriteRemoteIsFalse() {
        val shared = Alarm(id = "a1", label = "Same", hour = 7, minute = 0, source = "local", version = 2)

        val result = AlarmMerge.merge(
            local = listOf(shared),
            remote = listOf(shared),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).containsExactly(shared)
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isFalse()
    }

    @Test fun emptyBothSides_returnsEmptyNoWrite() {
        val result = AlarmMerge.merge(
            local = emptyList(),
            remote = emptyList(),
            localTombstones = emptySet(),
            remoteTombstones = emptySet(),
        )

        assertThat(result.merged).isEmpty()
        assertThat(result.added).isEmpty()
        assertThat(result.changed).isEmpty()
        assertThat(result.toWriteRemote).isFalse()
    }
}
