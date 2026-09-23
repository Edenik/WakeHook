package ai.wakehook.app.sync

import com.google.common.truth.Truth.assertThat
import java.io.IOException
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

        val newEtag = provider.writeJson("{\"alarms\":[{\"id\":\"a1\"}]}", null)

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

    @Test fun readJson_withFailReadSet_throwsInsteadOfReturningNull() = runTest {
        val provider = FakeSyncProvider()
        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")
        provider.failRead = true

        try {
            provider.readJson()
            org.junit.Assert.fail("expected IOException")
        } catch (e: IOException) {
            // expected: a genuine read failure must NOT look like "file absent" (null).
        }
    }

    @Test fun writeJson_withStaleExpectedEtag_throwsConflict_andDoesNotOverwrite() = runTest {
        val provider = FakeSyncProvider()
        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")
        val e1 = provider.readJson()!!.etag

        // Simulate an external write (e.g. another device) landing after our read.
        provider.simulateExternalWrite("{\"alarms\":[{\"id\":\"external\"}]}")

        try {
            provider.writeJson("{\"alarms\":[{\"id\":\"stale\"}]}", e1)
            org.junit.Assert.fail("expected ConflictException")
        } catch (e: ConflictException) {
            // expected
        }

        val after = provider.readJson()!!
        assertThat(after.content).contains("external")
        assertThat(after.content).doesNotContain("stale")
    }

    @Test fun writeJson_withMatchingExpectedEtag_succeeds() = runTest {
        val provider = FakeSyncProvider()
        provider.ensureFolderAndFiles("{\"alarms\":[]}", "# WakeHook")
        val e1 = provider.readJson()!!.etag

        provider.writeJson("{\"alarms\":[{\"id\":\"a1\"}]}", e1)

        assertThat(provider.readJson()!!.content).contains("a1")
    }
}
