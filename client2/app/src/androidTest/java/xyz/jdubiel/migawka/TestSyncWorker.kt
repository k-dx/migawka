package xyz.jdubiel.migawka

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.jdubiel.migawka.data.SyncWorker

@RunWith(AndroidJUnit4::class)
class SyncWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
//        Unfortunately, this does not work - DataStore value is not set in testSyncWorker()
//        val userSettingsRepository = (context as MigawkaApplication).userSettingsRepository
//        runBlocking {
//            userSettingsRepository.setServerAddress("192.168.5.158")
//        }
    }

    @Test
    fun testSyncWorker() {
        val worker = TestListenableWorkerBuilder<SyncWorker>(context).build()
        runBlocking {
            val result = worker.doWork()
            assertThat(result, `is`(Result.success()))
        }
    }
}
