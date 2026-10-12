package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlueySession
import com.example.model.EntryKind
import com.example.model.Mood
import com.example.model.Palette
import com.example.model.TranscriptEntry
import com.example.network.ConnectionStatus
import com.example.network.PhonePlacement
import com.example.ui.components.BlobShape
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.IBMPlexMonoFontFamily
import com.example.ui.theme.IBMPlexSansFontFamily
import com.example.viewmodel.BlueyViewModel
import com.example.viewmodel.LiveState

@Composable
fun BlueyAppView(
    viewModel: BlueyViewModel,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()
    val liveState by viewModel.liveState.collectAsState()
    val chirpVolume by viewModel.chirpVolume.collectAsState()
    val connectionStatus by viewModel.wsClient.status.collectAsState()
    val hostAddress by viewModel.wsClient.hostAddress.collectAsState()
    val placement by viewModel.wsClient.placement.collectAsState()

    var selectedSessionId by remember { mutableStateOf<String?>(null) }
    var selectedMobileTab by remember { mutableStateOf(0) } // 0: Sessions, 1: Settings

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("bluey_app_view_root")
    ) {
        val isWideScreen = maxWidth >= 650.dp

        if (isWideScreen) {
            // Wide Screen / Landscape: Side-by-side layout
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight()
                        .background(Palette.Panel.copy(alpha = 0.85f))
                ) {
                    SidebarContent(
                        viewModel = viewModel,
                        liveState = liveState,
                        chirpVolume = chirpVolume,
                        connectionStatus = connectionStatus,
                        hostAddress = hostAddress,
                        placement = placement,
                        onClose = onClose
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.Black)
                ) {
                    val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }
                    if (selectedSession != null) {
                        SessionDetailView(
                            session = selectedSession,
                            onBack = { selectedSessionId = null },
                            onDelete = {
                                viewModel.deleteSession(selectedSession.id)
                                selectedSessionId = null
                            }
                        )
                    } else {
                        SessionListView(
                            sessions = sessions,
                            currentSessionId = currentSessionId,
                            onSelect = { selectedSessionId = it.id }
                        )
                    }
                }
            }
        } else {
            // Mobile Portrait: Tabbed layout
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Palette.Panel)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to face",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                            .padding(3.dp)
                    ) {
                        FilterChip(
                            selected = selectedMobileTab == 0,
                            onClick = { selectedMobileTab = 0 },
                            label = {
                                Text(
                                    text = "Sessions",
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Palette.Berry3,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = Palette.InkSoft
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        FilterChip(
                            selected = selectedMobileTab == 1,
                            onClick = { selectedMobileTab = 1 },
                            label = {
                                Text(
                                    text = "PC Link & Settings",
                                    fontFamily = FredokaFontFamily,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Palette.Berry3,
                                selectedLabelColor = Color.White,
                                containerColor = Color.Transparent,
                                labelColor = Palette.InkSoft
                            )
                        )
                    }
                }

                // Mobile Body
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedMobileTab == 0) {
                        val selectedSession = sessions.firstOrNull { it.id == selectedSessionId }
                        if (selectedSession != null) {
                            SessionDetailView(
                                session = selectedSession,
                                onBack = { selectedSessionId = null },
                                onDelete = {
                                    viewModel.deleteSession(selectedSession.id)
                                    selectedSessionId = null
                                }
                            )
                        } else {
                            SessionListView(
                                sessions = sessions,
                                currentSessionId = currentSessionId,
                                onSelect = { selectedSessionId = it.id }
                            )
                        }
                    } else {
                        SidebarContent(
                            viewModel = viewModel,
                            liveState = liveState,
                            chirpVolume = chirpVolume,
                            connectionStatus = connectionStatus,
                            hostAddress = hostAddress,
                            placement = placement,
                            onClose = onClose
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SidebarContent(
    viewModel: BlueyViewModel,
    liveState: LiveState,
    chirpVolume: Float,
    connectionStatus: ConnectionStatus,
    hostAddress: String,
    placement: PhonePlacement,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    var editingHost by remember { mutableStateOf(hostAddress) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // Header: Mini Bluey & App Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniBlueyAvatar(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Bluey",
                    fontFamily = FredokaFontFamily,
                    fontSize = 26.sp,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (connectionStatus == ConnectionStatus.CONNECTED) Palette.Green else Palette.InkSoft.copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (connectionStatus) {
                            ConnectionStatus.CONNECTED -> "Windows PC Linked"
                            ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionStatus.DISCONNECTED -> "Standalone Mode"
                        },
                        fontFamily = IBMPlexSansFontFamily,
                        fontSize = 12.sp,
                        color = Palette.InkSoft
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Session Button (Start / End)
        Button(
            onClick = {
                viewModel.toggleWakeSleep()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (liveState == LiveState.ASLEEP) Palette.Berry3 else Palette.Rose
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("session_toggle_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (liveState == LiveState.ASLEEP) Icons.Default.PlayArrow else Icons.Default.Stop,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (liveState == LiveState.ASLEEP) "Start Session" else "End Session",
                    fontFamily = FredokaFontFamily,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = if (liveState == LiveState.ASLEEP)
                "Bluey stays awake and records transcripts and questions while the session is running."
            else
                "Session running! Hold his face on the home screen to ask questions.",
            fontFamily = IBMPlexSansFontFamily,
            fontSize = 12.sp,
            color = Palette.InkSoft,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        Spacer(modifier = Modifier.height(18.dp))

        // Chirp Volume Slider
        Text(
            text = "CHIRP VOLUME",
            fontFamily = IBMPlexMonoFontFamily,
            fontSize = 11.sp,
            color = Palette.InkSoft,
            fontWeight = FontWeight.Medium
        )
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

        Spacer(modifier = Modifier.height(14.dp))

        // Physical Phone Placement Calibration
        Text(
            text = "MONITOR CALIBRATION",
            fontFamily = IBMPlexMonoFontFamily,
            fontSize = 11.sp,
            color = Palette.InkSoft,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PhonePlacement.values().forEach { p ->
                FilterChip(
                    selected = placement == p,
                    onClick = { viewModel.setPlacement(p) },
                    label = {
                        Text(
                            text = p.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontFamily = IBMPlexSansFontFamily,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Palette.Berry3,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White.copy(alpha = 0.06f),
                        labelColor = Palette.InkSoft
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Windows Host Address input
        Text(
            text = "WINDOWS PC HOST (WEBSOCKET)",
            fontFamily = IBMPlexMonoFontFamily,
            fontSize = 11.sp,
            color = Palette.InkSoft,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = editingHost,
                onValueChange = { editingHost = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Palette.Berry2,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 12.sp
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Button(
                onClick = { viewModel.wsClient.updateHost(editingHost) },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Berry4),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text("Sync", fontFamily = IBMPlexSansFontFamily, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mood Playground (Testing all 7 expressions)
        Text(
            text = "EXPRESSION PLAYGROUND",
            fontFamily = IBMPlexMonoFontFamily,
            fontSize = 11.sp,
            color = Palette.InkSoft,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Mood.values().forEach { mood ->
                Button(
                    onClick = { viewModel.setManualMood(mood) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = mood.title,
                        fontFamily = IBMPlexSansFontFamily,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Back to Face button
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("back_to_face_button")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Back to His Face",
                    fontFamily = IBMPlexSansFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SessionListView(
    sessions: List<BlueySession>,
    currentSessionId: String?,
    onSelect: (BlueySession) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Meeting Sessions",
                fontFamily = FredokaFontFamily,
                fontSize = 26.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${sessions.size} recorded",
                fontFamily = IBMPlexMonoFontFamily,
                fontSize = 13.sp,
                color = Palette.InkSoft
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = Palette.Berry2,
                        modifier = Modifier.size(42.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No sessions yet",
                        fontFamily = FredokaFontFamily,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start one and your transcripts and Bluey's replies appear here.",
                        fontFamily = IBMPlexSansFontFamily,
                        fontSize = 13.sp,
                        color = Palette.InkSoft
                    )
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(sessions, key = { it.id }) { session ->
                    SessionRowCard(
                        session = session,
                        isLive = session.id == currentSessionId,
                        onClick = { onSelect(session) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRowCard(
    session: BlueySession,
    isLive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Panel),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("session_row_${session.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = session.formattedDate(),
                        fontFamily = IBMPlexSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    if (isLive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(Palette.Green, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "LIVE",
                                fontFamily = IBMPlexMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = session.summary,
                    fontFamily = IBMPlexSansFontFamily,
                    fontSize = 13.sp,
                    color = Palette.InkSoft,
                    maxLines = 1
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = session.formattedDuration(),
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 12.sp,
                    color = Palette.InkSoft
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${session.questionCount} asked",
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 11.sp,
                    color = Palette.Berry1
                )
            }
        }
    }
}

@Composable
private fun SessionDetailView(
    session: BlueySession,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Top Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back to sessions",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.formattedDate(),
                    fontFamily = FredokaFontFamily,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = "${session.formattedDuration()} · ${session.questionCount} questions asked",
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 11.sp,
                    color = Palette.InkSoft
                )
            }

            // Copy Agent Prompt Button
            Button(
                onClick = {
                    val prompt = """
                        I recorded a meeting with Bluey on ${session.formattedDate()}.
                        The notes are on my Windows PC in ${session.notesPath ?: "Documents/Bluey Notes/"}
                        
                        - notes.md: the transcript with timestamps and speaker diarization.
                        - session.json: structured transcript data.
                        - audio/: recording chunks.
                        
                        Read the notes and help me synthesize actionable takeaways and summary tasks.
                    """.trimIndent()
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Bluey Agent Prompt", prompt))
                    copied = true
                    Toast.makeText(context, "Agent prompt copied!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Palette.Berry3),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(
                    imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (copied) "Copied" else "Copy for Agent",
                    fontFamily = IBMPlexSansFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Share Text Button
            IconButton(
                onClick = {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, session.exportText())
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share Bluey Session"))
                },
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Session Button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Palette.Rose,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Entries List
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(session.displayEntries, key = { it.id }) { entry ->
                TranscriptEntryView(entry = entry)
            }
        }
    }
}

@Composable
private fun TranscriptEntryView(entry: TranscriptEntry) {
    when (entry.kind) {
        EntryKind.TRANSCRIPT -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                val speakerColor = when (entry.speaker?.uppercase()) {
                    "A" -> Color(0xFF8FB3FF)
                    "B" -> Color(0xFF5BE49B)
                    "C" -> Color(0xFFFFB86B)
                    else -> Palette.Berry1
                }
                Text(
                    text = "Speaker ${entry.speaker ?: "A"}",
                    fontFamily = IBMPlexMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = speakerColor,
                    modifier = Modifier.width(90.dp)
                )
                Text(
                    text = entry.text,
                    fontFamily = IBMPlexSansFontFamily,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        EntryKind.HEARD -> {
            Text(
                text = entry.text,
                fontFamily = IBMPlexSansFontFamily,
                fontSize = 13.sp,
                color = Palette.InkSoft,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        EntryKind.ASKED -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "YOU ASKED",
                        fontFamily = IBMPlexMonoFontFamily,
                        fontSize = 10.sp,
                        color = Palette.Berry1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Palette.Berry3,
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.text,
                            fontFamily = IBMPlexSansFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
        EntryKind.REPLY -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                MiniBlueyAvatar(modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = entry.text,
                        fontFamily = FredokaFontFamily,
                        fontSize = 15.sp,
                        color = Palette.Ink,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
        EntryKind.REPORT -> {
            var open by remember { mutableStateOf(false) }
            val lines = entry.text.split("\n\n")
            val title = lines.firstOrNull() ?: "Research Report"
            val body = lines.drop(1).joinToString("\n\n")

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFEEF0FF),
                border = androidx.compose.foundation.BorderStroke(2.dp, Palette.Berry2),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, end = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .clickable { open = !open }
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontFamily = FredokaFontFamily,
                            fontSize = 16.sp,
                            color = Palette.Ink,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Palette.Berry2
                        )
                    }

                    if (body.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = body,
                            fontFamily = IBMPlexSansFontFamily,
                            fontSize = 13.sp,
                            color = Palette.Ink,
                            maxLines = if (open) Int.MAX_VALUE else 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniBlueyAvatar(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(BlobShape())
            .background(Palette.blobBrush()),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Palette.Ink, CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Palette.Ink, CircleShape)
                )
            }
        }
    }
}
