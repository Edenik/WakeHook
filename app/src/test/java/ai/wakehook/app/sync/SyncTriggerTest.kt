package ai.wakehook.app.sync

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncTriggerTest {
    private val app: Application = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, config)
    }

    @Test
    fun schedulePeriodic_enqueuesUniquePeriodicWork() {
        SyncTrigger.schedulePeriodic(app)

        val workInfos = WorkManager.getInstance(app)
            .getWorkInfosForUniqueWork(SyncTrigger.PERIODIC_WORK_NAME)
            .get()

        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.map { it.state }).doesNotContain(WorkInfo.State.CANCELLED)
    }

    @Test
    fun now_enqueuesUniqueOneTimeWork() {
        SyncTrigger.now(app)

        val workInfos = WorkManager.getInstance(app)
            .getWorkInfosForUniqueWork(SyncTrigger.NOW_WORK_NAME)
            .get()

        assertThat(workInfos).isNotEmpty()
        assertThat(workInfos.map { it.state }).doesNotContain(WorkInfo.State.CANCELLED)
    }
}
