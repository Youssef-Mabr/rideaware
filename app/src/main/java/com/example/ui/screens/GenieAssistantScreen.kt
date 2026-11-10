package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.GeminiLiveVoiceState
import com.example.ai.LiveVoiceTurn
import com.example.ai.RideAwareContextStore
import com.example.camera.CameraDetectionPipeline
import com.example.camera.SimulatedCameraFrameSource
import com.example.camera.SimulationScenario
import com.example.data.MockDataProvider
import com.example.detection.SimulatedYoloDetector
import com.example.model.CameraSource
import com.example.model.GenieState
import com.example.model.VoiceConversation
import com.example.ui.GeminiAssistantState
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.GloveButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.delay

private enum class GenieDemoSafetyScenario(val label: String, val detail: String) {
  REAR_LEFT_MOTORCYCLE("Sim: Rear-left motorcycle", "Blindspot vehicle approaching at 48 km/h"),
  REAR_RIGHT_CAR("Sim: Rear-right car", "Silver sedan closing distance at 45 km/h"),
  NO_CAMERA_DATA("Sim: Camera unavailable", "Visual radar standby · audio copilot active")
}

@Composable
fun GenieAssistantScreen(
  assistantState: GeminiAssistantState = GeminiAssistantState(),
  onAsk: (String) -> Unit = {},
  voiceState: GeminiLiveVoiceState = GeminiLiveVoiceState.DISCONNECTED,
  onVoiceStart: () -> Unit = {},
  onVoiceStop: () -> Unit = {},
  voiceError: String? = null,
  voiceStatusDetail: String? = null,
  latestVoiceTurn: LiveVoiceTurn? = null,
  onVoiceFinishTurn: () -> Unit = {},
  modifier: Modifier = Modifier,
  onBack: (() -> Unit)? = null
) {
  var genieState by remember { mutableStateOf(GenieState.IDLE) }
  var prompt by remember { mutableStateOf("What is behind me?") }
  var lastGeminiResponse by remember { mutableStateOf<String?>(null) }
  var lastRecordedVoiceTurnId by remember { mutableStateOf<Long?>(null) }
  var demoScenario by remember { mutableStateOf(GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE) }
  val contextUpdateTick by RideAwareContextStore.provider.updates.collectAsState()
  val compactSafetyLabel = remember(contextUpdateTick, demoScenario) {
    RideAwareContextStore.provider.buildCompactStatusLabel()
  }

  // Camera feed simulation for Gemini's safety context awareness
  LaunchedEffect(demoScenario, onBack) {
    if (onBack != null) return@LaunchedEffect
    when (demoScenario) {
      GenieDemoSafetyScenario.NO_CAMERA_DATA -> {
        RideAwareContextStore.provider.clearCameraSource(CameraSource.REAR)
        RideAwareContextStore.provider.clearCameraSource(CameraSource.FRONT)
        RideAwareContextStore.provider.clearActiveAlert()
      }
      GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE,
      GenieDemoSafetyScenario.REAR_RIGHT_CAR -> {
        val rearScenario = if (demoScenario == GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE) {
          SimulationScenario.LEFT_BLIND_SPOT_VEHICLE
        } else {
          SimulationScenario.REAR_APPROACHING_VEHICLE
        }
        val rearPipeline = CameraDetectionPipeline(
          frameSource = SimulatedCameraFrameSource(CameraSource.REAR, rearScenario),
          detector = SimulatedYoloDetector()
        )
        val frontPipeline = CameraDetectionPipeline(
          frameSource = SimulatedCameraFrameSource(CameraSource.FRONT, SimulationScenario.FRONT_LEAD_VEHICLE),
          detector = SimulatedYoloDetector()
        )
        repeat(2) { step ->
          val t = System.currentTimeMillis() - (2 - step) * 200L
          rearPipeline.analyzeNextFrame(t)
          frontPipeline.analyzeNextFrame(t)
        }
        while (true) {
          val now = System.currentTimeMillis()
          RideAwareContextStore.provider.recordCameraAnalysis(
            analysis = rearPipeline.analyzeNextFrame(now),
            isSimulated = true,
            nowMillis = now
          )
          RideAwareContextStore.provider.recordCameraAnalysis(
            analysis = frontPipeline.analyzeNextFrame(now),
            isSimulated = true,
            nowMillis = now
          )
          delay(1000)
        }
      }
    }
  }

  val context = LocalContext.current
  val microphoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
    if (granted) onVoiceStart()
  }

  val conversations = remember {
    mutableStateListOf<VoiceConversation>().apply {
      addAll(MockDataProvider.sampleConversations)
    }
  }

  val listState = rememberLazyListState()
  LaunchedEffect(conversations.size) {
    if (conversations.isNotEmpty()) listState.animateScrollToItem(conversations.lastIndex)
  }

  LaunchedEffect(assistantState.response) {
    assistantState.response?.takeIf { it != lastGeminiResponse }?.let {
      lastGeminiResponse = it
      conversations.add(VoiceConversation(prompt, it))
      genieState = GenieState.SPEAKING
    }
  }

  LaunchedEffect(latestVoiceTurn?.id) {
    val turn = latestVoiceTurn ?: return@LaunchedEffect
    if (turn.id != lastRecordedVoiceTurnId) {
      lastRecordedVoiceTurnId = turn.id
      conversations.add(VoiceConversation(turn.userTranscript, turn.modelTranscript))
    }
  }

  // Voice State Flags
  val isVoiceActive = voiceState == GeminiLiveVoiceState.CONNECTING ||
    voiceState == GeminiLiveVoiceState.LISTENING ||
    voiceState == GeminiLiveVoiceState.SPEAKING
  val isListening = voiceState == GeminiLiveVoiceState.LISTENING

  // Pulsing Orb Animation
  val infiniteTransition = rememberInfiniteTransition(label = "genieAura")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 0.88f,
    targetValue = 1.14f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )
  val pulseGlow by infiniteTransition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseGlow"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    // Top Bar
    RideAwareTopBar(
      title = "Geni Copilot",
      subtitle = if (isVoiceActive) "Gemini Live Active • Hands-Free Voice" else "Standby • Tap mic or pick a prompt",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 18.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // 1. ROAD SAFETY CONTEXT HUD BANNER
      RoadSafetyContextHud(
        onBack = onBack,
        compactSafetyLabel = compactSafetyLabel,
        demoScenario = demoScenario,
        onCycleScenario = {
          demoScenario = when (demoScenario) {
            GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE -> GenieDemoSafetyScenario.REAR_RIGHT_CAR
            GenieDemoSafetyScenario.REAR_RIGHT_CAR -> GenieDemoSafetyScenario.NO_CAMERA_DATA
            GenieDemoSafetyScenario.NO_CAMERA_DATA -> GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE
          }
        }
      )

      // 2. VOICE VISUALIZER & PTT ACTION DECK
      VoiceVisualizerDeck(
        voiceState = voiceState,
        genieState = genieState,
        isVoiceActive = isVoiceActive,
        isListening = isListening,
        pulseScale = pulseScale,
        pulseGlow = pulseGlow,
        voiceStatusDetail = voiceStatusDetail,
        voiceError = voiceError,
        onToggleVoice = {
          if (voiceState == GeminiLiveVoiceState.DISCONNECTED || voiceState == GeminiLiveVoiceState.ERROR) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
              onVoiceStart()
            } else {
              microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
          } else {
            onVoiceStop()
          }
        },
        onFinishTurn = onVoiceFinishTurn
      )

      // 3. CONVERSATION STREAM (SCROLLABLE)
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
      ) {
        items(conversations) { conv ->
          ConversationMessageItem(conv)
        }
      }

      // 4. QUICK RIDER PROMPT CHIPS
      QuickRiderPromptCarousel(
        onSelectPrompt = { userQ, sampleA ->
          prompt = userQ
          conversations.add(VoiceConversation(userQ, sampleA))
          genieState = GenieState.SPEAKING
        }
      )

      // 5. TEXT QUERY DOCK
      TextQueryDock(
        prompt = prompt,
        onPromptChange = { prompt = it },
        loading = assistantState.loading,
        error = assistantState.error,
        onSend = {
          genieState = GenieState.THINKING
          onAsk(prompt)
        }
      )
    }
  }
}

/**
 * Road safety context banner displaying what visual data Geni is monitoring.
 */
@Composable
private fun RoadSafetyContextHud(
  onBack: (() -> Unit)?,
  compactSafetyLabel: String,
  demoScenario: GenieDemoSafetyScenario,
  onCycleScenario: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurface)
      .border(1.dp, TealPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
      .clickable(role = Role.Button, onClick = onCycleScenario)
      .padding(horizontal = 12.dp, vertical = 8.dp)
      .testTag("genie_safety_context_toggle")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier = Modifier
            .size(28.dp)
            .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Filled.Videocam, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = if (onBack != null) "Live Ride Awareness" else demoScenario.label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = if (onBack != null) compactSafetyLabel else demoScenario.detail,
            color = TealAccent,
            fontSize = 10.sp,
            maxLines = 1
          )
        }
      }

      Box(
        modifier = Modifier
          .background(DarkSurfaceElevated, RoundedCornerShape(6.dp))
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text("TAP TO CYCLE", color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

/**
 * Hero Voice Visualizer with multi-ring animated orb, live waveform, state badge, and PTT controls.
 */
@Composable
private fun VoiceVisualizerDeck(
  voiceState: GeminiLiveVoiceState,
  genieState: GenieState,
  isVoiceActive: Boolean,
  isListening: Boolean,
  pulseScale: Float,
  pulseGlow: Float,
  voiceStatusDetail: String?,
  voiceError: String?,
  onToggleVoice: () -> Unit,
  onFinishTurn: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(
        Brush.verticalGradient(
          colors = listOf(DarkSurfaceElevated, DarkSurface)
        )
      )
      .border(1.dp, if (isVoiceActive) TealPrimary.copy(alpha = 0.5f) else DarkSurfaceBorder, RoundedCornerShape(20.dp))
      .padding(vertical = 12.dp, horizontal = 16.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Glowing Orb + State Ribbon Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        // Multi-ring Glowing Orb
        Box(
          modifier = Modifier
            .size(68.dp)
            .background(
              Brush.radialGradient(
                listOf(
                  if (isVoiceActive) TealPrimary.copy(alpha = 0.35f * pulseGlow) else Color.Transparent,
                  Color.Transparent
                )
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size((52 * (if (isVoiceActive) pulseScale else 1f)).dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  if (isVoiceActive) listOf(TealPrimary, CyanAccent)
                  else listOf(Color(0xFF141C28), Color(0xFF1E2A3B))
                )
              )
              .border(
                1.5.dp,
                if (isVoiceActive) Color.White.copy(alpha = 0.7f) else DarkSurfaceBorder,
                CircleShape
              ),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = if (isVoiceActive) Icons.Filled.RecordVoiceOver else Icons.Filled.SmartToy,
              contentDescription = "Genie Copilot",
              tint = if (isVoiceActive) Color(0xFF041912) else TealPrimary,
              modifier = Modifier.size(26.dp)
            )
          }
        }

        // Live Audio Waveform & Status Badge
        Column(
          modifier = Modifier
            .weight(1f)
            .padding(start = 12.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Status Badge Pill
          Box(
            modifier = Modifier
              .background(
                if (isVoiceActive) TealGlow else DarkSurfaceBorder.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
              )
              .border(
                1.dp,
                if (isVoiceActive) TealPrimary.copy(alpha = 0.6f) else Color.Transparent,
                RoundedCornerShape(12.dp)
              )
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(
                    if (isVoiceActive) SuccessGreen else TextMuted,
                    CircleShape
                  )
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when (voiceState) {
                  GeminiLiveVoiceState.CONNECTING -> "CONNECTING TO LIVE..."
                  GeminiLiveVoiceState.LISTENING -> "LIVE • LISTENING..."
                  GeminiLiveVoiceState.SPEAKING -> "LIVE • GENI SPEAKING"
                  GeminiLiveVoiceState.ERROR -> "VOICE LINK ERROR"
                  GeminiLiveVoiceState.DISCONNECTED -> when (genieState) {
                    GenieState.LISTENING -> "HEARING PROMPT..."
                    GenieState.THINKING -> "AI PROCESSING..."
                    GenieState.SPEAKING -> "VOICE PREVIEW ACTIVE"
                    GenieState.IDLE -> "VOICE STANDBY"
                  }
                },
                color = if (isVoiceActive) TealAccent else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp
              )
            }
          }

          // Waveform
          AudioWaveformVisualizer(
            isAnimating = isVoiceActive || genieState == GenieState.SPEAKING || genieState == GenieState.LISTENING,
            color = if (isVoiceActive) TealPrimary else CyanAccent.copy(alpha = 0.5f),
            modifier = Modifier
              .fillMaxWidth()
              .height(20.dp)
          )
        }
      }

      // Voice Action Buttons Row (Start Voice / Stop Voice / Done Speaking)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Glove-friendly Voice Toggle Button
        Button(
          onClick = onToggleVoice,
          modifier = Modifier
            .weight(1f)
            .height(48.dp)
            .testTag("gemini_voice_toggle"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (isVoiceActive) DangerRed else TealPrimary,
            contentColor = if (isVoiceActive) Color.White else Color(0xFF041912)
          ),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(
            imageVector = if (isVoiceActive) Icons.Filled.MicOff else Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(Modifier.width(8.dp))
          Text(
            text = if (isVoiceActive) "Stop Voice" else "Start Voice (Live)",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (isListening) {
          OutlinedButton(
            onClick = onFinishTurn,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("gemini_voice_done_speaking"),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealAccent),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = Brush.horizontalGradient(listOf(TealPrimary, TealPrimary))
            )
          ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
              text = "Done Speaking",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }
      }

      // Detail or Error Feedback
      voiceStatusDetail?.let { detail ->
        Text(
          text = detail,
          color = TealAccent,
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("gemini_voice_status_detail")
        )
      }

      voiceError?.let { error ->
        Text(
          text = error,
          color = Color(0xFFFF8A80),
          fontSize = 11.sp,
          textAlign = TextAlign.Center,
          modifier = Modifier.testTag("gemini_voice_error")
        )
      }
    }
  }
}

/**
 * Beautiful conversation speech bubble item.
 */
@Composable
private fun ConversationMessageItem(conv: VoiceConversation) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    // Rider Query (Right)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, TealPrimary.copy(alpha = 0.25f), RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Text(
          text = conv.userQuery,
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    // Geni Response (Left)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.Start
    ) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
          .background(DarkSurface)
          .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp)
      ) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(
            imageVector = Icons.Filled.VolumeUp,
            contentDescription = null,
            tint = TealPrimary,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = conv.genieResponse,
            color = TealAccent,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}

/**
 * 2x2 Tactile Quick Rider Prompt shortcuts.
 */
@Composable
private fun QuickRiderPromptCarousel(
  onSelectPrompt: (question: String, response: String) -> Unit
) {
  val suggestions = listOf(
    Triple("Rear view", "\"Is anything behind me?\"", "A silver sedan is approaching from your rear-right at approximately 45 km/h."),
    Triple("Battery", "\"How is my helmet battery?\"", "Your helmet battery is at 88%. You have roughly 5.5 hours of ride time remaining."),
    Triple("Fuel stop", "\"Find the nearest petrol station.\"", "The nearest fuel stop is 1.8 km ahead on your right."),
    Triple("Translate", "\"Translate: 'Where is the workshop?' into Arabic.\"", "'Ayna warshat al-tasleeh?'")
  )

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Text(
      text = "Quick Rider Prompts:",
      color = TextSecondary,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      // Suggestion 1: Rear View
      PromptPill(
        title = suggestions[0].first,
        icon = Icons.Filled.Security,
        modifier = Modifier.weight(1f),
        testTag = "genie_suggest_behind",
        onClick = { onSelectPrompt(suggestions[0].second, suggestions[0].third) }
      )
      // Suggestion 2: Battery
      PromptPill(
        title = suggestions[1].first,
        icon = Icons.Filled.BatteryChargingFull,
        modifier = Modifier.weight(1f),
        testTag = "genie_suggest_battery",
        onClick = { onSelectPrompt(suggestions[1].second, suggestions[1].third) }
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      // Suggestion 3: Fuel Stop
      PromptPill(
        title = suggestions[2].first,
        icon = Icons.Filled.LocalGasStation,
        modifier = Modifier.weight(1f),
        testTag = "genie_suggest_petrol",
        onClick = { onSelectPrompt(suggestions[2].second, suggestions[2].third) }
      )
      // Suggestion 4: Translate
      PromptPill(
        title = suggestions[3].first,
        icon = Icons.Filled.Translate,
        modifier = Modifier.weight(1f),
        testTag = "genie_suggest_translate",
        onClick = { onSelectPrompt(suggestions[3].second, suggestions[3].third) }
      )
    }
  }
}

@Composable
private fun PromptPill(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  modifier: Modifier = Modifier,
  testTag: String,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .height(46.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurface)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
      .clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 10.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(16.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = title,
        color = TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

/**
 * Bottom Text Query Dock with input field and Send action.
 */
@Composable
private fun TextQueryDock(
  prompt: String,
  onPromptChange: (String) -> Unit,
  loading: Boolean,
  error: String?,
  onSend: () -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      OutlinedTextField(
        value = prompt,
        onValueChange = onPromptChange,
        placeholder = { Text("Ask Geni copilot...", color = TextMuted, fontSize = 13.sp) },
        enabled = !loading,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = DarkSurface,
          unfocusedContainerColor = DarkSurface,
          focusedBorderColor = TealPrimary,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
          .weight(1f)
          .height(52.dp)
          .testTag("gemini_prompt_input")
      )

      Button(
        onClick = onSend,
        enabled = !loading && prompt.isNotBlank(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary, contentColor = Color(0xFF041912)),
        modifier = Modifier
          .size(52.dp)
          .testTag("gemini_send_button"),
        contentPadding = PaddingValues(0.dp)
      ) {
        if (loading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF041912), strokeWidth = 2.dp)
        } else {
          Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
        }
      }
    }

    error?.let { err ->
      Text(err, color = Color(0xFFFF8A80), fontSize = 11.sp, modifier = Modifier.testTag("gemini_error"))
    }
  }
}
