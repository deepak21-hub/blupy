package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.anim.FaceAnimator
import com.example.audio.ChirpSynthesizer
import com.example.model.BlueySession
import com.example.model.EntryKind
import com.example.model.Mood
import com.example.model.TranscriptEntry
import com.example.network.BlueyWebSocketClient
import com.example.network.ConnectionStatus
import com.example.network.PhonePlacement
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class LiveState {
    ASLEEP,
    WAKING,
    LISTENING,
    ASKING,
    THINKING,
    SPEAKING
}

class BlueyViewModel(application: Application) : AndroidViewModel(application) {

    val animator = FaceAnimator()
    val chirpSynth = ChirpSynthesizer()
    val wsClient = BlueyWebSocketClient(viewModelScope)

    private val _liveState = MutableStateFlow(LiveState.LISTENING)
    val liveState = _liveState.asStateFlow()

    private val _chirpVolume = MutableStateFlow(0.85f)
    val chirpVolume = _chirpVolume.asStateFlow()

    private val _speechBubble = MutableStateFlow<String?>("Hi! I'm Bluey. Place me under your monitor!")
    val speechBubble = _speechBubble.asStateFlow()

    private val _sessions = MutableStateFlow<List<BlueySession>>(emptyList())
    val sessions = _sessions.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _isCursorSimActive = MutableStateFlow(false)
    val isCursorSimActive = _isCursorSimActive.asStateFlow()

    private var simCursorJob: Job? = null
    private var speechBubbleDismissJob: Job? = null

    init {
        animator.isAwake = true
        loadInitialSessions()
        startNewSession()

        // Auto dismiss welcome bubble after 6s
        viewModelScope.launch {
            delay(6000)
            if (_speechBubble.value == "Hi! I'm Bluey. Place me under your monitor!") {
                _speechBubble.value = null
            }
        }

        // Listen for cursor updates from Windows PC
        viewModelScope.launch {
            wsClient.cursorFlow.collect { (gazeX, gazeY) ->
                animator.receiveRemote(
                    com.example.model.FaceState(
                        gazeX = gazeX,
                        gazeY = gazeY,
                        mood = animator.localMood ?: com.example.model.Mood.LISTENING
                    ),
                    System.currentTimeMillis() / 1000.0
                )
            }
        }

        // Listen for actions/responses from Windows PC
        viewModelScope.launch {
            wsClient.actionFlow.collect { packet ->
                if (packet.action == "chirp" || packet.action == "speak" || packet.type == "action") {
                    val emotion = packet.emotion?.lowercase() ?: "happy"
                    val mood = when (emotion) {
                        "happy" -> Mood.HAPPY
                        "thinking" -> Mood.THINKING
                        "pointing" -> Mood.POINTING
                        "sleepy" -> Mood.SLEEPY
                        else -> Mood.HAPPY
                    }
                    animator.localMood = mood

                    viewModelScope.launch {
                        chirpSynth.playChirp(_chirpVolume.value)
                    }

                    if (!packet.text.isNullOrBlank()) {
                        showBubble(packet.text)
                        addReplyToCurrentSession(packet.text)
                    }
                }
            }
        }

        // Try auto-connecting to desktop
        wsClient.connect()
    }

    private fun loadInitialSessions() {
        val now = System.currentTimeMillis()
        val sample1 = BlueySession(
            id = UUID.randomUUID().toString(),
            started = now - 1800000,
            ended = now - 600000,
            notesPath = "~/Documents/Bluey Notes/2026-10-12-design-review",
            entries = listOf(
                TranscriptEntry(
                    kind = EntryKind.TRANSCRIPT,
                    text = "Welcome everyone, let's review the new Bluey desktop companion architecture.",
                    time = now - 1750000,
                    speaker = "A"
                ),
                TranscriptEntry(
                    kind = EntryKind.TRANSCRIPT,
                    text = "The spatial eye-tracking runs at 30fps over WebSocket, which feels super natural.",
                    time = now - 1650000,
                    speaker = "B"
                ),
                TranscriptEntry(
                    kind = EntryKind.ASKED,
                    text = "What is this download button doing?",
                    time = now - 1500000
                ),
                TranscriptEntry(
                    kind = EntryKind.REPLY,
                    text = "That's the build artifact exporter! It packages the Windows system tray companion and installer.",
                    time = now - 1490000
                ),
                TranscriptEntry(
                    kind = EntryKind.REPORT,
                    text = "Desktop Companion Research\n\nFastAPI WebSocket server on port 8765 handles sub-10ms latency. PyAutoGUI controls mouse and keyboard with safety guardrails on destructive actions.",
                    time = now - 1400000
                )
            )
        )

        val sample2 = BlueySession(
            id = UUID.randomUUID().toString(),
            started = now - 86400000,
            ended = now - 84000000,
            notesPath = "~/Documents/Bluey Notes/2026-10-11-pairing-test",
            entries = listOf(
                TranscriptEntry(
                    kind = EntryKind.TRANSCRIPT,
                    text = "Testing mDNS auto-discovery and calibration between the Android phone and PC monitor.",
                    time = now - 86300000,
                    speaker = "A"
                ),
                TranscriptEntry(
                    kind = EntryKind.ASKED,
                    text = "Where are you looking right now?",
                    time = now - 85500000
                ),
                TranscriptEntry(
                    kind = EntryKind.REPLY,
                    text = "I'm looking directly up at your code editor on the main screen above me!",
                    time = now - 85400000
                )
            )
        )

        _sessions.value = listOf(sample1, sample2)
    }

    fun toggleWakeSleep() {
        if (_liveState.value == LiveState.ASLEEP) {
            wakeUp()
        } else {
            putToSleep()
        }
    }

    fun wakeUp() {
        if (_liveState.value != LiveState.ASLEEP) return
        _liveState.value = LiveState.WAKING
        animator.isAwake = true
        animator.localMood = Mood.HAPPY

        viewModelScope.launch {
            chirpSynth.playChirp(_chirpVolume.value)
            delay(600)
            _liveState.value = LiveState.LISTENING
            animator.localMood = null
            startNewSession()
            wsClient.sendStatus("awake")
        }
    }

    fun putToSleep() {
        _liveState.value = LiveState.ASLEEP
        animator.isAwake = false
        animator.localMood = Mood.SLEEPY
        endCurrentSession()
        wsClient.sendStatus("asleep")

        viewModelScope.launch {
            delay(1200)
            if (_liveState.value == LiveState.ASLEEP) {
                animator.localMood = Mood.RESTING
            }
        }
    }

    fun startAsking() {
        if (_liveState.value == LiveState.ASLEEP) {
            wakeUp()
            return
        }
        _liveState.value = LiveState.ASKING
        animator.localMood = Mood.LISTENING
        _speechBubble.value = null
        wsClient.sendStatus("listening")
    }

    fun askQuestion(prompt: String = "What's on the screen above?") {
        _liveState.value = LiveState.THINKING
        animator.localMood = Mood.THINKING
        addAskedToCurrentSession(prompt)

        viewModelScope.launch {
            if (wsClient.status.value == ConnectionStatus.CONNECTED) {
                wsClient.sendQuery(prompt)
            } else {
                delay(900)
                generateStandaloneReply(prompt)
            }
        }
    }

    fun endAsking(questionPrompt: String? = null) {
        val askedText = questionPrompt ?: "What's on the screen above?"
        askQuestion(askedText)
    }

    private fun generateStandaloneReply(query: String) {
        val replies = listOf(
            "I'm tracking your screen above! Place me right under your monitor for natural eye-contact.",
            "I see your cursor moving! Bluey is watching and ready to point at things.",
            "Looking at your active window above! Everything is running smoothly.",
            "I'm on it! Your Windows PC companion is listening and ready for commands.",
            "Chirp chirp! Bluey is here and tracking your mouse cursor in real time."
        )
        val reply = when {
            query.contains("look", ignoreCase = true) || query.contains("cursor", ignoreCase = true) ->
                "I'm tracking your screen above! Place me right under your monitor for natural eye-contact."
            query.contains("who", ignoreCase = true) ->
                "I'm Bluey! Your blueberry AI companion living on your phone and helping you on your PC."
            query.contains("what", ignoreCase = true) ->
                "Looking at your active window above! It looks clean and ready for work."
            else ->
                replies.random()
        }

        viewModelScope.launch {
            _liveState.value = LiveState.SPEAKING
            animator.localMood = Mood.TALKING
            chirpSynth.playChirp(_chirpVolume.value)
            showBubble(reply)
            addReplyToCurrentSession(reply)

            delay(2800)
            _liveState.value = LiveState.LISTENING
            animator.localMood = Mood.HAPPY
            delay(1200)
            animator.localMood = null
        }
    }

    fun sayHi() {
        viewModelScope.launch {
            chirpSynth.playChirp(_chirpVolume.value)
            animator.localMood = Mood.HAPPY
            showBubble("Hi there! I'm Bluey!")
            delay(2000)
            if (_liveState.value == LiveState.ASLEEP) {
                animator.localMood = Mood.RESTING
            } else {
                animator.localMood = null
            }
        }
    }

    fun showBubble(text: String) {
        _speechBubble.value = text
        speechBubbleDismissJob?.cancel()
        speechBubbleDismissJob = viewModelScope.launch {
            delay(5000)
            _speechBubble.value = null
        }
    }

    fun setChirpVolume(vol: Float) {
        _chirpVolume.value = vol.coerceIn(0f, 1f)
    }

    fun setPlacement(placement: PhonePlacement) {
        wsClient.setPlacement(placement)
    }

    fun setManualMood(mood: Mood) {
        animator.localMood = mood
        viewModelScope.launch {
            chirpSynth.playChirp(_chirpVolume.value * 0.6f)
            delay(3000)
            animator.localMood = null
        }
    }

    fun toggleCursorSimulation() {
        val next = !_isCursorSimActive.value
        _isCursorSimActive.value = next
        if (next) {
            simCursorJob = viewModelScope.launch {
                var angle = 0.0
                while (_isCursorSimActive.value) {
                    angle += 0.08
                    val x = (kotlin.math.sin(angle) * 0.75).toFloat()
                    val y = (-0.6 + kotlin.math.cos(angle * 0.7) * 0.35).toFloat()
                    animator.receiveRemote(
                        com.example.model.FaceState(
                            gazeX = x,
                            gazeY = y,
                            mood = animator.localMood ?: Mood.LISTENING
                        ),
                        System.currentTimeMillis() / 1000.0
                    )
                    delay(33) // ~30 fps
                }
            }
        } else {
            simCursorJob?.cancel()
            simCursorJob = null
        }
    }

    private fun startNewSession() {
        val newSession = BlueySession(
            id = UUID.randomUUID().toString(),
            started = System.currentTimeMillis()
        )
        _sessions.value = listOf(newSession) + _sessions.value
        _currentSessionId.value = newSession.id
    }

    private fun endCurrentSession() {
        val currentId = _currentSessionId.value ?: return
        val now = System.currentTimeMillis()
        _sessions.value = _sessions.value.map { session ->
            if (session.id == currentId) session.copy(ended = now) else session
        }
        _currentSessionId.value = null
    }

    private fun addAskedToCurrentSession(text: String) {
        val entry = TranscriptEntry(
            kind = EntryKind.ASKED,
            text = text,
            time = System.currentTimeMillis()
        )
        appendEntry(entry)
    }

    private fun addReplyToCurrentSession(text: String) {
        val entry = TranscriptEntry(
            kind = EntryKind.REPLY,
            text = text,
            time = System.currentTimeMillis()
        )
        appendEntry(entry)
    }

    private fun appendEntry(entry: TranscriptEntry) {
        val currentId = _currentSessionId.value ?: run {
            startNewSession()
            _currentSessionId.value
        }
        _sessions.value = _sessions.value.map { session ->
            if (session.id == currentId) {
                session.copy(entries = session.entries + entry)
            } else session
        }
    }

    fun deleteSession(id: String) {
        _sessions.value = _sessions.value.filter { it.id != id }
    }
}
