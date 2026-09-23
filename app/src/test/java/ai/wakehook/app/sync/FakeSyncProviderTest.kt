package ai.wakehook.app.sync

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FakeSyncProviderTest {

    @Test fun ensureThenRead_returnsSeed() = runTest {
        val provider = FakeSyncProvider()

        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")

        val file = provider.readJson()
        assertThat(file).isNotNull()
        assertThat(file!!.content).isEqualTo("{\"alarms\":[]}")
        assertThat(provider.md).isEqualTo("# WakeHook")
    }

    @Test fun secondEnsure_doesNotOverwrite() = runTest {
        val provider = FakeSyncProvider()
        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")

        provider.ensureFolderAndFiles("{\"alarms\":[{\"id\":\"x\"}]}", "# Different")

        val file = provider.readJson()
        assertThat(file!!.content).isEqualTo("{\"alarms\":[]}")
        assertThat(provider.md).isEqualTo("# WakeHook")
    }

    @Test fun writeJson_changesEtag_andReadReturnsNewContent() = runTest {
        val provider = FakeSyncProvider()
        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")
        val before = provider.readJson()!!

        val newEtag = provider.writeJson("{\"alarms\":[{\"id\":\"a1\"}]}")

        assertThat(newEtag).isNotEqualTo(before.etag)
        val after = provider.readJson()!!
        assertThat(after.content).isEqualTo("{\"alarms\":[{\"id\":\"a1\"}]}")
        assertThat(after.etag).isEqualTo(newEtag)
    }

    @Test fun readJson_beforeEnsure_returnsNull() = runTest {
        val provider = FakeSyncProvider()

        assertThat(provider.readJson()).isNull()
    }

    @Test fun isConnected_reflectsConnectedFlag() = runTest {
        val provider = FakeSyncProvider()
        assertThat(provider.isConnected()).isTrue()

        provider.connected = false

        assertThat(provider.isConnected()).isFalse()
    }
}
