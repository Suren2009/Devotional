package com.pradarshana.core.data.sync

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.core.data.IncrementResult
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.StartResult
import com.pradarshana.core.data.TestClock
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PhoneWatchSyncTest {

    private class Device(
        val db: PradarshanaDatabase,
        val repo: SessionRepository,
        val sync: SyncManager,
        val transport: FakeDataLayer.Node,
    ) {
        suspend fun session(id: String): PradarshanaSession = repo.getSession(id)!!
    }

    private val clock = TestClock()
    private val layer = FakeDataLayer()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private lateinit var phone: Device
    private lateinit var watch: Device

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        phone = device(context, "phone-1", layer.a)
        watch = device(context, "watch-1", layer.b)
    }

    @After
    fun tearDown() {
        scope.cancel()
        phone.db.close()
        watch.db.close()
    }

    private fun device(context: Context, id: String, node: FakeDataLayer.Node): Device {
        val db = PradarshanaDatabase.create(context, name = null)
        val repo = SessionRepository(db, id, clock)
        val sync = SyncManager(repo, node, scope, logger = { _, _ -> })
        node.deviceId = id
        node.receiver = { sync.onRemoteSnapshots(it) }
        return Device(db, repo, sync, node)
    }

    private suspend fun Device.start(target: Int) = (repo.startSession(target) as StartResult.Started).sessionId

    private suspend fun Device.tap(id: String, times: Int = 1) = repeat(times) {
        clock.advance(1_000)
        repo.increment(id)
    }

    private suspend fun pump() = layer.pump(phone.sync, watch.sync)

    @Test
    fun phoneIncrementsAppearOnWatch() = runTest {
        val id = phone.start(108)
        phone.tap(id, 27)
        pump()
        val onWatch = watch.session(id)
        assertEquals(27, onWatch.completedCount)
        assertEquals(81, onWatch.remainingCount)
        assertEquals(108, onWatch.targetCount)
        assertEquals(SessionStatus.ACTIVE, onWatch.status)
        assertEquals(phone.session(id).startTime, onWatch.startTime)
    }

    @Test
    fun watchIncrementsAppearOnPhone() = runTest {
        val id = phone.start(108)
        pump()
        watch.tap(id, 10)
        pump()
        assertEquals(10, phone.session(id).completedCount)
    }

    @Test
    fun receivingSyncMessagesNeverIncrementsTheCounter() = runTest {
        layer.duplicateDeliveries = true
        val id = phone.start(108)
        phone.tap(id, 5)
        pump()
        repeat(3) {
            watch.sync.onRemoteSnapshots(watch.transport.fetchAll())
            phone.sync.onRemoteSnapshots(phone.transport.fetchAll())
            watch.sync.reconcile()
            phone.sync.reconcile()
        }
        pump()
        assertEquals(5, phone.session(id).completedCount)
        assertEquals(5, watch.session(id).completedCount)
    }

    @Test
    fun alternatingTapsOnBothDevicesAreCountedOnce() = runTest {
        val id = phone.start(108)
        pump()
        repeat(10) {
            phone.tap(id)
            pump()
            watch.tap(id)
            pump()
        }
        assertEquals(20, phone.session(id).completedCount)
        assertEquals(20, watch.session(id).completedCount)
    }

    @Test
    fun watchKeepsCountingWhileDisconnectedAndCatchesUp() = runTest {
        val id = phone.start(108)
        phone.tap(id, 4)
        pump()

        layer.connected = false
        watch.tap(id, 30)
        phone.tap(id, 2)
        pump()
        assertEquals(6, phone.session(id).completedCount)
        assertEquals(34, watch.session(id).completedCount)

        layer.connected = true
        pump()
        assertEquals(36, phone.session(id).completedCount)
        assertEquals(36, watch.session(id).completedCount)
    }

    @Test
    fun failedPublishesAreRetriedLater() = runTest {
        val id = phone.start(50)
        phone.transport.failPublishes = true
        phone.tap(id, 7)
        pump()
        assertNull(watch.repo.getSession(id))

        phone.transport.failPublishes = false
        pump()
        assertEquals(7, watch.session(id).completedCount)
    }

    @Test
    fun concurrentOfflineUpdatesResolveIdenticallyOnBothDevices() = runTest {
        val id = phone.start(108)
        pump()
        layer.connected = false
        phone.tap(id, 3)
        watch.tap(id, 5)
        clock.advance(10)
        phone.repo.pause(id)
        clock.advance(10)
        watch.repo.pause(id)
        clock.advance(10)
        watch.repo.resume(id)

        layer.connected = true
        pump()
        val p = phone.session(id)
        val w = watch.session(id)
        assertEquals(8, p.completedCount)
        assertEquals(p, w)
        assertEquals(SessionStatus.ACTIVE, p.status)
    }

    @Test
    fun overshootWhileDisconnectedIsCappedAndCompletesOnce() = runTest {
        val id = phone.start(10)
        phone.tap(id, 9)
        pump()
        layer.connected = false
        assertTrue(phone.repo.increment(id) is IncrementResult.Completed)
        assertTrue(watch.repo.increment(id) is IncrementResult.Completed)
        layer.connected = true
        pump()
        for (device in listOf(phone, watch)) {
            val s = device.session(id)
            assertEquals(10, s.completedCount)
            assertEquals(0, s.remainingCount)
            assertEquals(SessionStatus.COMPLETED, s.status)
        }
        assertEquals(phone.session(id).endTime, watch.session(id).endTime)
        assertEquals(1, phone.repo.observeHistory().first().size)
    }

    @Test
    fun sessionCountedEntirelyOnWatchCompletesAndReachesPhoneHistory() = runTest {
        layer.connected = false
        val id = watch.start(21)
        watch.tap(id, 20)
        val last = watch.repo.increment(id)
        assertTrue(last is IncrementResult.Completed)
        assertEquals(SessionStatus.COMPLETED, watch.session(id).status)
        assertNull(phone.repo.getSession(id))

        layer.connected = true
        pump()
        val onPhone = phone.repo.observeHistory().first().single()
        assertEquals(id, onPhone.id)
        assertEquals(SessionStatus.COMPLETED, onPhone.status)
        assertEquals(21, onPhone.completedCount)
        assertEquals(watch.session(id).endTime, onPhone.endTime)
        assertNull(phone.repo.getOngoingSession())
    }

    @Test
    fun completionOnPhoneStopsWatchFromCounting() = runTest {
        val id = phone.start(2)
        pump()
        phone.tap(id, 2)
        pump()
        assertEquals(SessionStatus.COMPLETED, watch.session(id).status)
        assertEquals(IncrementResult.Rejected(IncrementResult.Reason.ALREADY_FINISHED), watch.repo.increment(id))
    }

    @Test
    fun endingOnOneDeviceEndsOnTheOther() = runTest {
        val id = phone.start(108)
        phone.tap(id, 3)
        pump()
        watch.repo.endSession(id)
        pump()
        assertEquals(SessionStatus.CANCELLED, phone.session(id).status)
        assertEquals(3, phone.session(id).completedCount)
    }

    @Test
    fun sessionsStartedOnBothDevicesWhileOfflineLeaveOnlyNewestInProgress() = runTest {
        layer.connected = false
        val older = phone.start(10)
        clock.advance(5_000)
        val newer = watch.start(20)
        layer.connected = true
        pump()
        assertEquals(newer, phone.repo.getOngoingSession()!!.id)
        assertEquals(newer, watch.repo.getOngoingSession()!!.id)
        assertEquals(SessionStatus.CANCELLED, phone.session(older).status)
        assertEquals(SessionStatus.CANCELLED, watch.session(older).status)
    }

    @Test
    fun clearedHistoryIsNotReimportedFromThePeer() = runTest {
        val id = phone.start(1)
        phone.tap(id)
        pump()
        clock.advance(1_000)
        phone.sync.clearHistory()
        phone.sync.reconcile()
        assertTrue(phone.repo.observeHistory().first().isEmpty())
        assertEquals(SessionStatus.COMPLETED, watch.session(id).status)
    }
}
