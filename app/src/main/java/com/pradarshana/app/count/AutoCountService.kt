package com.pradarshana.app.count

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pradarshana.app.MainActivity
import com.pradarshana.app.R
import com.pradarshana.core.data.CompletionVibration
import com.pradarshana.core.data.CountFeedbackPreferences
import com.pradarshana.core.data.IncrementResult
import com.pradarshana.core.data.PradarshanaContainerOwner
import com.pradarshana.core.data.SessionRepository
import com.pradarshana.core.data.location.hasLocationPermission
import com.pradarshana.core.model.CounterRules
import com.pradarshana.core.model.PradarshanaSession
import com.pradarshana.core.model.ReturnToPointTracker
import com.pradarshana.core.model.ReturnVisit
import com.pradarshana.core.model.SessionKind
import com.pradarshana.core.model.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Counts a Pradarshana round when the walker leaves the start and comes back.
 * The counting screen stays in the foreground; this keeps counting if the phone is covered.
 */
class AutoCountService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client by lazy { LocationServices.getFusedLocationProviderClient(this) }
    private var listening = false
    private var tracker: ReturnToPointTracker? = null
    private var trackedSessionId: String? = null
    private lateinit var repository: SessionRepository
    private lateinit var feedback: CountFeedbackPreferences

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val sessionId = trackedSessionId ?: return
            val active = tracker ?: return
            val time = location.time.takeIf { it > 0L } ?: System.currentTimeMillis()
            when (val visit = active.onFix(location.latitude, location.longitude, location.accuracy, time)) {
                is ReturnVisit.AnchorChosen -> {
                    progress("At the start. Walk away, then come back.")
                    scope.launch { repository.rememberStartLocation(sessionId, visit.latitude, visit.longitude) }
                }
                ReturnVisit.WaitingForStart -> progress("Stand at the starting place")
                is ReturnVisit.Watching -> progress(
                    if (visit.leftStart) {
                        "Come back to the start. ${visit.distanceMeters} m away."
                    } else {
                        "${visit.distanceMeters} m from the start. Walk farther away."
                    },
                )
                ReturnVisit.Counted -> {
                    progress("Round counted. Walk away again.")
                    scope.launch { countRound(sessionId) }
                }
                ReturnVisit.Ignored -> Unit
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val container = (application as PradarshanaContainerOwner).container
        repository = container.repository
        feedback = container.countFeedback
        ensureChannel()
        if (!runCatching { promote("Waiting for the walk to begin") }.isSuccess) {
            stopSelf()
            return
        }
        scope.launch { watchSession() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        promote("Waiting for the walk to begin")
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopLocation()
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun watchSession() {
        combine(
            repository.observeOngoingSession(),
            feedback.autoCount,
            feedback.awayMeters,
            feedback.returnMeters,
        ) { session, enabled, away, back -> Target(session, enabled, away, back) }
            .collect { target ->
                val session = target.session
                val counting = target.enabled &&
                    session != null &&
                    session.kind == SessionKind.PRADARSHANA &&
                    session.status == SessionStatus.ACTIVE
                if (session == null || !target.enabled || session.kind != SessionKind.PRADARSHANA || session.status.isTerminal) {
                    stopLocation()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@collect
                }
                trackerFor(session, target.awayMeters, target.returnMeters)
                if (counting && hasLocationPermission()) {
                    startLocation()
                    promote("${session.completedCount} rounds")
                } else if (session.status == SessionStatus.PAUSED) {
                    stopLocation()
                    promote("Paused at ${session.completedCount} rounds")
                } else {
                    stopLocation()
                    promote("Allow location to count rounds automatically")
                }
            }
    }

    private fun trackerFor(session: PradarshanaSession, awayMeters: Int, returnMeters: Int): ReturnToPointTracker {
        val current = tracker
        if (current != null && trackedSessionId == session.id) {
            current.updateDistances(awayMeters, returnMeters)
            return current
        }
        trackedSessionId = session.id
        val temple = feedback.matchingTemple(session.latitude, session.longitude)
        progress(if (temple == null) "Stand at the starting place" else "Using ${temple.name}. Walk away, then come back.")
        return ReturnToPointTracker(
            awayMeters = awayMeters,
            returnMeters = returnMeters,
            anchorLatitude = temple?.latitude,
            anchorLongitude = temple?.longitude,
        ).also { tracker = it }
    }

    private suspend fun countRound(sessionId: String) {
        when (val result = repository.increment(sessionId)) {
            is IncrementResult.Counted -> when {
                feedback.guruBell.value && CounterRules.isHalfway(result.session.targetCount, result.session.completedCount) ->
                    CompletionVibration.guru(this)
                feedback.vibrateOnCount.value -> CompletionVibration.tap(this)
            }
            is IncrementResult.Completed -> if (feedback.guruBell.value) CompletionVibration.guru(this) else CompletionVibration.start(this)
            is IncrementResult.Rejected -> Unit
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocation() {
        if (listening || !hasLocationPermission()) return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
            .setMinUpdateIntervalMillis(1_000L)
            .setMinUpdateDistanceMeters(0f)
            .build()
        client.requestLocationUpdates(request, callback, Looper.getMainLooper())
        listening = true
    }

    private fun stopLocation() {
        if (!listening) return
        client.removeLocationUpdates(callback)
        listening = false
    }

    private fun ensureChannel() {
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(CHANNEL_ID, "Pradarshana counting", NotificationManager.IMPORTANCE_LOW)
        channel.setShowBadge(false)
        manager.createNotificationChannel(channel)
    }

    private fun promote(text: String) {
        runCatching { showForeground(text) }.onFailure { stopSelf() }
    }

    private fun showForeground(text: String) {
        val notification = buildNotification(text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_count)
            .setContentTitle("Counting pradakshina")
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private data class Target(
        val session: PradarshanaSession?,
        val enabled: Boolean,
        val awayMeters: Int,
        val returnMeters: Int,
    )

    companion object {
        private const val CHANNEL_ID = "pradarshana_auto_count"
        private const val NOTIFICATION_ID = 42
        private const val UPDATE_INTERVAL_MILLIS = 2_000L

        private val _progress = MutableStateFlow("Stand at the starting place")
        val progress: StateFlow<String> = _progress.asStateFlow()

        fun progress(message: String) {
            _progress.value = message
        }

        fun start(context: Context) {
            val intent = Intent(context.applicationContext, AutoCountService::class.java)
            runCatching { ContextCompat.startForegroundService(context.applicationContext, intent) }
        }
    }
}
