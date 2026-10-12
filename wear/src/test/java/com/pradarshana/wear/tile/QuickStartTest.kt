package com.pradarshana.wear.tile

import androidx.test.core.app.ApplicationProvider
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.db.PradarshanaDatabase
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuickStartTest {

    private lateinit var db: PradarshanaDatabase
    private lateinit var repo: SessionRepository
    private var now = 1_700_000_000_000L

    @Before
    fun setUp() {
        db = PradarshanaDatabase.create(ApplicationProvider.getApplicationContext(), name = null)
        repo = SessionRepository(db, "watch-1", clock = { now.also { now += 1_000 } })
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun tileOffersTheLatestWalkUntilOneIsInProgress() {
        val idle = QuickStart.content(ongoing = null, latestTarget = 54)
        assertEquals("Pradarshana", idle.title)
        assertEquals("54", idle.countText)
        assertEquals("rounds", idle.detail)
        assertEquals("Start", idle.action)

        val active = PradarshanaSession("s", 108, 12, 0, null, SessionStatus.ACTIVE, 0)
        val walking = QuickStart.content(active, latestTarget = 54)
        assertEquals("12", walking.countText)
        assertEquals("of 108", walking.detail)
        assertEquals("Resume", walking.action)

        val paused = active.copy(status = SessionStatus.PAUSED, kind = SessionKind.CHANTING)
        val held = QuickStart.content(paused, latestTarget = 54)
        assertEquals("Chanting", held.title)
        assertEquals("Paused", held.detail)
        assertEquals("Resume", held.action)
    }

    @Test
    fun firstTapStarts108AndTheNextTapResumesThatWalk() = runBlocking {
        val first = QuickStart.startOrResume(repo)
        val started = repo.getSession(first)!!
        assertEquals(QuickStart.DEFAULT_TARGET, started.targetCount)
        assertEquals(SessionKind.PRADARSHANA, started.kind)
        assertEquals("", started.title)
        assertEquals(first, QuickStart.startOrResume(repo))

        repo.endSession(first)
        assertNull(repo.getOngoingSession())
        val second = QuickStart.startOrResume(repo)
        val again = repo.getSession(second)!!
        assertEquals(QuickStart.DEFAULT_TARGET, again.targetCount)
        assertEquals(SessionKind.PRADARSHANA, again.kind)
    }

    @Test
    fun quickStartRepeatsTheLatestWalkTitleAndTarget() = runBlocking {
        val original = QuickStart.startOrResume(repo)
        repo.endSession(original)
        val named = repo.startSession(21, title = "Meenakshi", kind = SessionKind.PRADARSHANA)
        val namedId = (named as com.pradarshana.core.data.StartResult.Started).sessionId
        repo.endSession(namedId)
        repo.startSession(11, title = "Sahasranama", kind = SessionKind.CHANTING).also {
            repo.endSession((it as com.pradarshana.core.data.StartResult.Started).sessionId)
        }

        val id = QuickStart.startOrResume(repo)
        val session = repo.getSession(id)!!
        assertEquals(21, session.targetCount)
        assertEquals("Meenakshi", session.title)
        assertEquals(SessionKind.PRADARSHANA, session.kind)
    }
}
