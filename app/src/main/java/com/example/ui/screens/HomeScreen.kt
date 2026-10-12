package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.anim.FaceAnimator
import com.example.model.Palette
import com.example.network.ConnectionStatus
import com.example.ui.components.FaceCanvas
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.IBMPlexMonoFontFamily
import com.example.ui.theme.IBMPlexSansFontFamily
import com.example.viewmodel.BlueyViewModel
import com.example.viewmodel.LiveState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: BlueyViewModel,
    onOpenDashboard: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val liveState by viewModel.liveState.collectAsState()
    val chirpVolume by viewModel.chirpVolume.collectAsState()
    val speechBubble by viewModel.speechBubble.collectAsState()
    val isCursorSimActive by viewModel.isCursorSimActive.collectAsState()
    val connectionStatus by viewModel.wsClient.status.collectAsState()

    var showSoundSettings by remember { mutableStateOf(false) }

    // Haptic feedback helper
    fun vibrate(durationMs: Long = 40) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    // Animation frame step loop
    val frame by produceState(
        initialValue = viewModel.animator.step(System.currentTimeMillis() / 1000.0)
    ) {
        while (true) {
            withFrameNanos { nanos ->
                val nowSec = nanos / 1_000_000_000.0
                value = viewModel.animator.step(nowSec)
            }
        }
    }

    // Border pulse animation for asking state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("home_screen_root")
    ) {
        // Face canvas and gestures
        var holdJob by remember { mutableStateOf<Job?>(null) }
        var isHolding by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            vibrate(60)
                            viewModel.toggleWakeSleep()
                        },
                        onTap = { offset ->
                            // Look at tap point
                            viewModel.animator.touchGazeX = (offset.x / size.width) * 2f - 1f
                            viewModel.animator.touchGazeY = (offset.y / size.height) * 2f - 1f
                            scope.launch {
                                delay(600)
                                viewModel.animator.touchGazeX = null
                                viewModel.animator.touchGazeY = null
                            }
                            if (liveState == LiveState.ASLEEP) {
                                viewModel.wakeUp()
                            }
                        },
                        onPress = { offset ->
                            viewModel.animator.touchGazeX = (offset.x / size.width) * 2f - 1f
                            viewModel.animator.touchGazeY = (offset.y / size.height) * 2f - 1f

                            isHolding = false
                            holdJob = scope.launch {
                                delay(250)
                                isHolding = true
                                vibrate(45)
                                viewModel.startAsking()
                            }

                            try {
                                tryAwaitRelease()
                                holdJob?.cancel()
                                if (isHolding) {
                                    vibrate(35)
                                    viewModel.endAsking("What's on the screen above?")
                                }
                            } finally {
                                holdJob?.cancel()
                                isHolding = false
                                viewModel.animator.touchGazeX = null
                                viewModel.animator.touchGazeY = null
                            }
                        }
                    )
                }
        ) {
            FaceCanvas(frame = frame)
        }

        // Active Glowing Border
        val borderColor = when (liveState) {
            LiveState.ASLEEP -> Color.Transparent
            LiveState.WAKING -> Palette.Yellow
            LiveState.LISTENING -> Palette.Blue
            LiveState.ASKING -> Palette.Blue.copy(alpha = pulseAlpha)
            LiveState.THINKING -> Palette.Purple
            LiveState.SPEAKING -> Palette.Pink
        }
        val borderWidth = when (liveState) {
            LiveState.ASKING -> 10.dp
            LiveState.LISTENING -> 5.dp
            LiveState.WAKING, LiveState.THINKING, LiveState.SPEAKING -> 4.dp
            LiveState.ASLEEP -> 0.dp
        }

        if (borderWidth > 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(24.dp)
                    )
            )
        }

        // Top-Left: Mode Indicator Capsule
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 20.dp)
        ) {
            ModeIndicatorBadge(liveState = liveState)
        }

        // Top-Right: Quick Toolbar (Wake/Sleep, Cursor Sim, Sound Settings, Dashboard)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Wake / Sleep toggle button
            IconButton(
                onClick = {
                    vibrate(40)
                    viewModel.toggleWakeSleep()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (liveState == LiveState.ASLEEP) Color.White.copy(alpha = 0.08f) else Palette.Berry3,
                        CircleShape
                    )
                    .testTag("wake_sleep_button")
            ) {
                Icon(
                    imageVector = if (liveState == LiveState.ASLEEP) Icons.Default.WbSunny else Icons.Default.Bedtime,
                    contentDescription = if (liveState == LiveState.ASLEEP) "Wake Up" else "Sleep",
                    tint = if (liveState == LiveState.ASLEEP) Palette.Yellow else Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Windows Cursor Simulator toggle
            IconButton(
                onClick = {
                    vibrate(30)
                    viewModel.toggleCursorSimulation()
                },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isCursorSimActive) Palette.Green.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .testTag("cursor_sim_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Mouse,
                    contentDescription = "Simulate Windows Cursor Eye Tracking",
                    tint = if (isCursorSimActive) Color.Black else Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Sound Settings button
            IconButton(
                onClick = { showSoundSettings = !showSoundSettings },
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (showSoundSettings) Palette.Berry2 else Color.White.copy(alpha = 0.08f),
                        CircleShape
                    )
                    .testTag("sound_settings_button")
            ) {
                Icon(
                    imageVector = if (showSoundSettings) Icons.Default.Close else when {
                        chirpVolume < 0.05f -> Icons.AutoMirrored.Filled.VolumeMute
                        chirpVolume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                        else -> Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = "Sound Settings",
                    tint = Color.White.copy(alpha = if (showSoundSettings) 1f else 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Dashboard / Meeting Notes button
            IconButton(
                onClick = onOpenDashboard,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    .testTag("open_dashboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Dashboard,
                    contentDescription = "Open Bluey Dashboard",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sound Settings Dropdown
        AnimatedVisibility(
            visible = showSoundSettings,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut() + scaleOut(targetScale = 0.9f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 68.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Palette.Panel.copy(alpha = 0.95f),
                tonalElevation = 8.dp,
                modifier = Modifier.width(280.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CHIRP VOLUME",
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = 11.sp,
                            color = Palette.InkSoft,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${(chirpVolume * 100).toInt()}%",
                            fontFamily = IBMPlexMonoFontFamily,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }

                    Slider(
                        value = chirpVolume,
                        onValueChange = { viewModel.setChirpVolume(it) },
                        colors = SliderDefaults.colors(
                            thumbColor = Palette.Berry1,
                            activeTrackColor = Palette.Berry2,
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (liveState == LiveState.ASLEEP) viewModel.wakeUp() else viewModel.sayHi()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Palette.Berry3),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    ) {
                        Text(
                            text = if (liveState == LiveState.ASLEEP) "Wake Him Up" else "Say Hi Chirp",
                            fontFamily = IBMPlexSansFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Windows Companion Connection Info
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (connectionStatus == ConnectionStatus.CONNECTED) Palette.Green else Palette.InkSoft.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (connectionStatus) {
                                ConnectionStatus.CONNECTED -> "Windows PC Linked"
                                ConnectionStatus.CONNECTING -> "Connecting to PC..."
                                ConnectionStatus.DISCONNECTED -> "Standalone Mode"
                            },
                            fontFamily = IBMPlexSansFontFamily,
                            fontSize = 12.sp,
                            color = Palette.InkSoft
                        )
                    }
                }
            }
        }

        // Center-Top: Speech Bubble Popup
        AnimatedVisibility(
            visible = !speechBubble.isNullOrBlank(),
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 380.dp)
                    .padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Palette.Berry2, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = speechBubble ?: "",
                        fontFamily = FredokaFontFamily,
                        fontSize = 16.sp,
                        color = Palette.Ink,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Bottom Controls Pill: Direct Touch & Voice Interactions
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Primary Action: Hold or Tap to Ask
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = if (liveState == LiveState.ASKING) Palette.Rose else Palette.Berry3,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .height(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    vibrate(40)
                                    viewModel.askQuestion("What's on the screen above?")
                                },
                                onPress = {
                                    vibrate(40)
                                    viewModel.startAsking()
                                    try {
                                        tryAwaitRelease()
                                        vibrate(40)
                                        viewModel.endAsking("What's on the screen above?")
                                    } catch (_: Exception) {
                                        viewModel.endAsking("What's on the screen above?")
                                    }
                                }
                            )
                        }
                        .testTag("talk_action_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (liveState == LiveState.ASKING) "Listening... Release" else "Ask Bluey",
                            fontFamily = FredokaFontFamily,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Say Hi Chirp pill
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier
                        .height(48.dp)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                vibrate(30)
                                viewModel.sayHi()
                            }
                        }
                        .testTag("say_hi_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Palette.Berry1,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Say Hi",
                            fontFamily = IBMPlexSansFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hold face or button to talk · Double-tap face to sleep",
                fontFamily = IBMPlexSansFontFamily,
                fontSize = 11.sp,
                color = Palette.InkSoft.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ModeIndicatorBadge(liveState: LiveState) {
    val (label, icon, color) = when (liveState) {
        LiveState.ASLEEP -> Triple("Asleep", Icons.Default.PowerSettingsNew, Palette.InkSoft)
        LiveState.WAKING -> Triple("Waking up", Icons.Default.PlayArrow, Palette.Yellow)
        LiveState.LISTENING -> Triple("Listening", Icons.AutoMirrored.Filled.VolumeUp, Palette.Blue)
        LiveState.ASKING -> Triple("I'm all ears", Icons.Default.Mic, Palette.Blue)
        LiveState.THINKING -> Triple("Thinking", Icons.Default.Settings, Palette.Purple)
        LiveState.SPEAKING -> Triple("Replying", Icons.AutoMirrored.Filled.VolumeUp, Palette.Pink)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.9f),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontFamily = FredokaFontFamily,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}
