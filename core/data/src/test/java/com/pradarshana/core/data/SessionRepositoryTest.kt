package com.pradarshana.core.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.CounterRules
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SessionRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: PradarshanaDatabase
    private lateinit var repo: SessionRepository
    private val clock = TestClock()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = PradarshanaDatabase.create(context, name = null)
        repo = SessionRepository(db, "phone-1", clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun start(
        target: Int,
        title: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
        kind: SessionKind = SessionKind.PRADARSHANA,
    ): String = (repo.startSession(target, title, latitude, longitude, kind) as StartResult.Started).sessionId

    @Test
    fun titleAndLocationAreStoredAndABlankTitleStaysBlank() = runTest {
        val named = start(21, title = "  Kapaleeshwarar  ", latitude = 13.033, longitude = 80.270)
        val session = repo.getSession(named)!!
        assertEquals("Kapaleeshwarar", session.title)
        assertEquals("Kapaleeshwarar", session.displayTitle)
        assertEquals(13.033, session.latitude!!, 0.0)
        assertEquals(80.270, session.longitude!!, 0.0)

        val plain = start(3, latitude = 95.0, longitude = 0.0)
        val untitled = repo.getSession(plain)!!
        assertEquals("", untitled.title)
        assertEquals("Untitled", untitled.displayTitle)
        assertEquals(false, untitled.hasLocation)
    }

    @Test
    fun startingWithValidTargetInitialisesSession() = runTest {
        val id = start(108)
        val session = repo.getSession(id)!!
        assertEquals(108, session.targetCount)
        assertEquals(0, session.completedCount)
        assertEquals(108, session.remainingCount)
        assertEquals(SessionStatus.ACTIVE, session.status)
        assertEquals(clock.now(), session.startTime)
        assertEquals(id, repo.observeOngoingSession().first()?.id)
    }

    @Test
    fun zeroOrNegativeTargetsAreRejected() = runTest {
        assertEquals(StartResult.Invalid(CounterRules.TargetValidation.NotPositive), repo.startSession(0))
        assertEquals(StartResult.Invalid(CounterRules.TargetValidation.NotPositive), repo.startSession(-3))
        assertNull(repo.getOngoingSession())
    }

    @Test
    fun eachIncrementAddsExactlyOne() = runTest {
        val id = start(108)
        repeat(27) { repo.increment(id) }
        val session = repo.getSession(id)!!
        assertEquals(27, session.completedCount)
        assertEquals(81, session.remainingCount)
        assertEquals(25, session.progressPercent)
        assertEquals(27, db.sessionDao().countOperations(id))
    }

    @Test
    fun concurrentTapsAreAllCountedOnce() = runTest {
        val id = start(500)
        (1..200).map { async { repo.increment(id) } }.awaitAll()
        assertEquals(200, repo.getSession(id)!!.completedCount)
    }

    @Test
    fun reachingTargetCompletesExactlyOnceAndBlocksFurtherIncrements() = runTest {
        val id = start(3)
        assertTrue(repo.increment(id) is IncrementResult.Counted)
        assertTrue(repo.increment(id) is IncrementResult.Counted)
        clock.advance(60_000)
        val completion = repo.increment(id)
        assertTrue(completion is IncrementResult.Completed)

        val after = repo.increment(id)
        assertEquals(IncrementResult.Rejected(IncrementResult.Reason.ALREADY_FINISHED), after)

        val session = repo.getSession(id)!!
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(3, session.completedCount)
        assertEquals(0, session.remainingCount)
        assertEquals(100, session.progressPercent)
        assertEquals(clock.now(), session.endTime)
        assertNull(repo.getOngoingSession())
        assertEquals(listOf(id), repo.observeHistory().first().map { it.id })
    }

    @Test
    fun pausedSessionDoesNotCount() = runTest {
        val id = start(10)
        repo.increment(id)
        assertTrue(repo.pause(id))
        assertEquals(IncrementResult.Rejected(IncrementResult.Reason.PAUSED), repo.increment(id))
        assertTrue(repo.resume(id))
        repo.increment(id)
        assertEquals(2, repo.getSession(id)!!.completedCount)
    }

    @Test
    fun endingSessionKeepsItAsIncompleteInHistory() = runTest {
        val id = start(108)
        repeat(5) { repo.increment(id) }
        assertTrue(repo.endSession(id))
        val history = repo.observeHistory().first()
        assertEquals(1, history.size)
        assertEquals(SessionStatus.CANCELLED, history.single().status)
        assertEquals(5, history.single().completedCount)
        assertNotNull(history.single().endTime)
    }

    @Test
    fun startingNewSessionEndsThePreviousOne() = runTest {
        val first = start(10)
        clock.advance(1000)
        val second = start(20)
        assertEquals(SessionStatus.CANCELLED, repo.getSession(first)!!.status)
        assertEquals(second, repo.getOngoingSession()!!.id)
    }

    @Test
    fun deletingOneFinishedSessionKeepsTheOthersAndBlocksReimport() = runTest {
        val keep = start(1)
        repo.increment(keep)
        clock.advance(1_000)
        val drop = start(2)
        repo.increment(drop)
        repo.increment(drop)
        val remote = repo.snapshot(drop)!!.first

        assertTrue(repo.deleteSession(drop))
        assertNull(repo.getSession(drop))
        assertEquals(listOf(keep), repo.observeHistory().first().map { it.id })

        val reimport = repo.applyRemote(remote)
        assertTrue(reimport.ignored)
        assertNull(repo.getSession(drop))

        val ongoing = start(5)
        assertEquals(false, repo.deleteSession(ongoing))
        assertEquals(ongoing, repo.getOngoingSession()!!.id)
    }

    @Test
    fun historyIsMostRecentFirstAndCanBeCleared() = runTest {
        val ids = (1..3).map {
            clock.advance(10_000)
            val id = start(1)
            repo.increment(id)
            id
        }
        assertEquals(ids.reversed(), repo.observeHistory().first().map { it.id })

        val ongoing = start(5)
        repo.clearHistory()
        assertTrue(repo.observeHistory().first().isEmpty())
        assertEquals(ongoing, repo.getOngoingSession()!!.id)
    }

    @Test
    fun activeSessionSurvivesProcessDeath() = runTest {
        val name = "restore-test.db"
        context.deleteDatabase(name)
        val firstDb = PradarshanaDatabase.create(context, name)
        val firstRepo = SessionRepository(firstDb, "phone-1", clock)
        val id = (firstRepo.startSession(108) as StartResult.Started).sessionId
        repeat(27) { firstRepo.increment(id) }
        firstRepo.pause(id)
        firstDb.close()

        val secondDb = PradarshanaDatabase.create(context, name)
        val restoredRepo = SessionRepository(secondDb, "phone-1", clock)
        val restored = restoredRepo.observeOngoingSession().first()!!
        assertEquals(id, restored.id)
        assertEquals(27, restored.completedCount)
        assertEquals(SessionStatus.PAUSED, restored.status)

        restoredRepo.resume(id)
        restoredRepo.increment(id)
        assertEquals(28, restoredRepo.getSession(id)!!.completedCount)
        assertEquals(28, secondDb.sessionDao().countOperations(id))
        secondDb.close()
        context.deleteDatabase(name)
    }

    @Test
    fun completedHistoryIsPersistedAcrossRestarts() = runTest {
        val name = "history-test.db"
        context.deleteDatabase(name)
        val firstDb = PradarshanaDatabase.create(context, name)
        val firstRepo = SessionRepository(firstDb, "phone-1", clock)
        val id = (firstRepo.startSession(2) as StartResult.Started).sessionId
        firstRepo.increment(id)
        firstRepo.increment(id)
        firstDb.close()

        val secondDb = PradarshanaDatabase.create(context, name)
        val history = SessionRepository(secondDb, "phone-1", clock).observeHistory().first()
        assertEquals(1, history.size)
        assertEquals(SessionStatus.COMPLETED, history.single().status)
        assertEquals(2, history.single().completedCount)
        secondDb.close()
        context.deleteDatabase(name)
    }

    @Test
    fun suggestionsUseTheLatestTitleThenFrequentCounts() = runTest {
        start(108, title = "First")
        clock.advance(1_000)
        start(108, title = "  ")
        clock.advance(1_000)
        start(21, title = "Meenakshi")
        assertEquals("Meenakshi", repo.latestSessionTitle())
        assertEquals(listOf(21, 108, 3, 9, 18, 54), repo.suggestedCounts())
    }

    @Test
    fun chantingIsStoredAndBecomesTheLatestKind() = runTest {
        start(108)
        clock.advance(1_000)
        val id = start(11, title = "Sahasranama", kind = SessionKind.CHANTING)
        assertEquals(SessionKind.CHANTING, repo.getSession(id)!!.kind)
        assertEquals(SessionKind.CHANTING, repo.latestSessionKind())
        assertEquals(SessionKind.PRADARSHANA, repo.getSession(repo.observeHistory().first().last().id)!!.kind)
    }

    @Test
    fun latestWalkTargetAndTitleIgnoreChanting() = runTest {
        assertNull(repo.latestTargetFor(SessionKind.PRADARSHANA))
        assertEquals("", repo.latestTitleFor(SessionKind.PRADARSHANA))
        start(54, title = "Kapaleeshwarar")
        clock.advance(1_000)
        start(11, title = "Sahasranama", kind = SessionKind.CHANTING)
        assertEquals(54, repo.latestTargetFor(SessionKind.PRADARSHANA))
        assertEquals("Kapaleeshwarar", repo.latestTitleFor(SessionKind.PRADARSHANA))
        assertEquals(11, repo.latestTargetFor(SessionKind.CHANTING))
    }

    @Test
    fun totalTimeIgnoresTheGapBetweenPauseAndResume() = runTest {
        val id = start(3)
        clock.advance(5_000)
        assertTrue(repo.pause(id))
        clock.advance(20_000)
        assertTrue(repo.resume(id))
        clock.advance(5_000)
        repeat(3) { repo.increment(id) }
        val session = repo.getSession(id)!!
        assertEquals(SessionStatus.COMPLETED, session.status)
        assertEquals(10_000, session.durationMillis())
        assertEquals(session.startTime + 30_000, session.endTime)
    }

    @Test
    fun rememberStartLocationFillsAMissingPlaceAndDoesNotReplaceOne() = runTest {
        val id = start(3)
        repo.rememberStartLocation(id, 13.0, 80.0)
        val saved = repo.getSession(id)!!
        assertEquals(13.0, saved.latitude!!, 0.0)
        assertEquals(80.0, saved.longitude!!, 0.0)
        repo.rememberStartLocation(id, 1.0, 2.0)
        assertEquals(13.0, repo.getSession(id)!!.latitude!!, 0.0)
        assertEquals(80.0, repo.getSession(id)!!.longitude!!, 0.0)
    }

    @Test
    fun undoRemovesOneLocalCountAndARepeatedSnapshotDoesNotPutItBack() = runTest {
        val id = (repo.startSession(5, kind = SessionKind.CHANTING, mantra = "Om") as StartResult.Started).sessionId
        assertEquals("Om", repo.getSession(id)!!.mantra)
        repeat(3) { repo.increment(id) }
        val undone = repo.undo(id)
        assertTrue(undone is UndoResult.Undone)
        assertEquals(2, repo.getSession(id)!!.completedCount)
        val snapshot = repo.snapshot(id)!!.first
        val watch = SessionRepository(db, "watch-1", clock)
        val again = watch.applyRemote(snapshot)
        assertEquals(2, watch.getSession(id)!!.completedCount)
        watch.applyRemote(snapshot)
        assertEquals(2, repo.getSession(id)!!.completedCount)
        assertTrue(again.changedLocally || !again.ignored)
        assertEquals(UndoResult.Rejected(UndoResult.Reason.NOTHING_TO_UNDO), SessionRepository(db, "watch-1", clock).undo(id))
        repeat(3) { repo.increment(id) }
        assertEquals(SessionStatus.COMPLETED, repo.getSession(id)!!.status)
        assertEquals(UndoResult.Rejected(UndoResult.Reason.ALREADY_FINISHED), repo.undo(id))
    }

    @Test
    fun historyFileRoundTripsAndDoesNotRestoreADeletedSession() = runTest {
        val kept = start(4, title = "Kept")
        repo.increment(kept)
        val dropped = start(4, title = "Dropped")
        repo.endSession(dropped)
        repo.deleteSession(dropped)
        val json = repo.exportHistory()
        val freshDb = PradarshanaDatabase.create(context, name = null)
        val fresh = SessionRepository(freshDb, "phone-2", clock)
        val imported = fresh.importHistory(json)
        assertTrue(imported >= 1)
        assertEquals(1, fresh.getSession(kept)!!.completedCount)
        assertEquals(null, fresh.getSession(dropped))
        assertEquals(-1, fresh.importHistory("not json"))
        freshDb.close()
    }
}
