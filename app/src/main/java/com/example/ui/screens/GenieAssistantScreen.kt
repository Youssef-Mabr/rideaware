package com.example.ui.screens

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockDataProvider
import com.example.model.GenieState
import com.example.model.VoiceConversation
import com.example.ui.components.AudioWaveformVisualizer
import com.example.ui.components.RideAwareTopBar
import com.example.ui.GeminiAssistantState
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.collectAsState
import com.example.ai.ContextFreshness
import com.example.ai.GeminiLiveVoiceState
import com.example.ai.LiveVoiceTurn
import com.example.ai.RideAwareContextStore
import com.example.camera.CameraDetectionPipeline
import com.example.camera.SimulatedCameraFrameSource
import com.example.camera.SimulationScenario
import com.example.detection.SimulatedYoloDetector
import com.example.model.CameraSource
import com.example.ui.GeminiLiveVoiceViewModel
import kotlinx.coroutines.delay

private enum class GenieDemoSafetyScenario(val label: String) {
  REAR_LEFT_MOTORCYCLE("Sim: Rear-left motorcycle"),
  REAR_RIGHT_CAR("Sim: Rear-right car"),
  NO_CAMERA_DATA("Sim: Camera unavailable")
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

  LaunchedEffect(demoScenario, onBack) {
    // When opened from Active Ride (onBack != null), MainActivity already feeds the ride camera pipeline.
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
        // Warm up 3 frames so TemporalObjectTracker and HazardEvaluator establish track persistence
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

  // Glowing orb animation
  val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
  val orbScale by infiniteTransition.animateFloat(
    initialValue = 0.85f,
    targetValue = 1.15f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "orbScale"
  )

  fun triggerQuestion(question: String, answer: String) {
    genieState = GenieState.LISTENING
    conversations.add(VoiceConversation(question, "..."))
    // Simulate thinking then speaking
  }

  val isVoiceActive = voiceState == GeminiLiveVoiceState.CONNECTING ||
    voiceState == GeminiLiveVoiceState.LISTENING ||
    voiceState == GeminiLiveVoiceState.SPEAKING

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Geni voice preview",
      onBack = onBack,
      subtitle = if (isVoiceActive) "Gemini Live active · microphone streaming" else "Scripted examples · microphone is off"
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Genie Glowing Avatar / Orb & Status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp)
      ) {
        Box(
          modifier = Modifier
            .size(100.dp)
            .background(
              Brush.radialGradient(
                listOf(TealPrimary.copy(alpha = 0.35f * orbScale), Color.Transparent)
              ),
              CircleShape
            ),
          contentAlignment = Alignment.Center
        ) {
          Box(
            modifier = Modifier
              .size((70 * orbScale).dp)
              .clip(CircleShape)
              .background(
                Brush.linearGradient(
                  listOf(TealPrimary, CyanAccent)
                )
              )
              .border(2.dp, Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Filled.Mic,
              contentDescription = "Genie",
              tint = Color(0xFF041912),
              modifier = Modifier.size(36.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // State Badge: Listening, Thinking, Speaking
        Row(
          modifier = Modifier
            .background(TealGlow, RoundedCornerShape(20.dp))
            .border(1.dp, TealPrimary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(modifier = Modifier.size(8.dp).background(TealPrimary, CircleShape))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = when (voiceState) {
              GeminiLiveVoiceState.CONNECTING -> "CONNECTING TO GENI LIVE..."
              GeminiLiveVoiceState.LISTENING -> "LIVE • LISTENING — SPEAK NOW"
              GeminiLiveVoiceState.SPEAKING -> "LIVE • GENI IS SPEAKING"
              GeminiLiveVoiceState.ERROR -> "LIVE VOICE ERROR"
              GeminiLiveVoiceState.DISCONNECTED -> when (genieState) {
                GenieState.LISTENING -> "PREVIEWING YOUR PROMPT..."
                GenieState.THINKING -> "LOADING SAMPLE RESPONSE..."
                GenieState.SPEAKING -> "SAMPLE RESPONSE"
                GenieState.IDLE -> "READY • SELECT A SAMPLE PROMPT"
              }
            },
            color = TealAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sound Waveform
        AudioWaveformVisualizer(
          isAnimating = genieState == GenieState.SPEAKING ||
            genieState == GenieState.LISTENING ||
            voiceState == GeminiLiveVoiceState.LISTENING ||
            voiceState == GeminiLiveVoiceState.SPEAKING,
          color = TealPrimary,
          modifier = Modifier.width(180.dp).height(28.dp)
        )

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Button(
            onClick = {
              if (voiceState == GeminiLiveVoiceState.DISCONNECTED || voiceState == GeminiLiveVoiceState.ERROR) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) onVoiceStart()
                else microphoneLauncher.launch(Manifest.permission.RECORD_AUDIO)
              } else onVoiceStop()
            },
            modifier = Modifier.testTag("gemini_voice_toggle")
          ) {
            Icon(Icons.Filled.Mic, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (voiceState == GeminiLiveVoiceState.DISCONNECTED || voiceState == GeminiLiveVoiceState.ERROR) "Start voice" else "Stop voice")
          }

          if (voiceState == GeminiLiveVoiceState.LISTENING) {
            OutlinedButton(
              onClick = onVoiceFinishTurn,
              modifier = Modifier.testTag("gemini_voice_done_speaking")
            ) {
              Text("Done speaking", color = TealAccent)
            }
          }
        }

        voiceStatusDetail?.let { detail ->
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = detail,
            color = TealAccent,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("gemini_voice_status_detail")
          )
        }

        voiceError?.let { error ->
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = error,
            color = Color(0xFFFF8A80),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("gemini_voice_error")
          )
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Conversation Stream (User Questions & Genie Answers)
      LazyColumn(
        state = listState,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(conversations) { conv ->
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // User Query Bubble (Right)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.End
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
                  .background(DarkSurfaceElevated)
                  .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp))
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

            // Genie Answer Bubble (Left)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.Start
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                  .background(DarkSurface)
                  .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
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
      }

      Spacer(modifier = Modifier.height(10.dp))

      OutlinedTextField(
        value = prompt,
        onValueChange = { prompt = it },
        label = { Text("Ask RideAware") },
        enabled = !assistantState.loading,
        singleLine = false,
        modifier = Modifier.fillMaxWidth().testTag("gemini_prompt_input")
      )
      Button(
        onClick = { genieState = GenieState.THINKING; onAsk(prompt) },
        enabled = !assistantState.loading && prompt.isNotBlank(),
        modifier = Modifier.fillMaxWidth().testTag("gemini_send_button")
      ) {
        if (assistantState.loading) CircularProgressIndicator(modifier = Modifier.size(18.dp))
        else Text("Send to Gemini")
      }
      assistantState.error?.let { error ->
        Text(error, color = Color(0xFFFF8A80), fontSize = 12.sp, modifier = Modifier.testTag("gemini_error"))
      }

      // Example Voice Command Suggestion Chips & Safety Context Status
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Choose a sample prompt:",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, TealPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(role = Role.Button) {
              demoScenario = when (demoScenario) {
                GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE -> GenieDemoSafetyScenario.REAR_RIGHT_CAR
                GenieDemoSafetyScenario.REAR_RIGHT_CAR -> GenieDemoSafetyScenario.NO_CAMERA_DATA
                GenieDemoSafetyScenario.NO_CAMERA_DATA -> GenieDemoSafetyScenario.REAR_LEFT_MOTORCYCLE
              }
            }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("genie_safety_context_toggle")
        ) {
          Text(
            text = if (onBack != null) compactSafetyLabel else demoScenario.label,
            color = TealAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        val suggestions = listOf(
          "\"Is anything behind me?\"" to "A silver sedan is approaching from your rear-right at approximately 45 km/h.",
          "\"How is my helmet battery?\"" to "Your helmet battery is at 86%. You have roughly 5 hours of ride time remaining.",
          "\"Find the nearest petrol station.\"" to "The nearest fuel stop is 1.8 km ahead on the right.",
          "\"What is my current safety score?\"" to "Your current safety score is 94. Smooth ride so far.",
          "\"Translate: 'Where is the workshop?' into Arabic.\"" to "'Ayna warshat al-tasleeh?'"
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                val s = suggestions[0]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 12.dp)
              .testTag("genie_suggest_behind")
          ) {
            Text("Rear view", color = TextPrimary, fontSize = 14.sp)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                val s = suggestions[1]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 12.dp)
              .testTag("genie_suggest_battery")
          ) {
            Text("Battery", color = TextPrimary, fontSize = 14.sp)
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                val s = suggestions[2]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 12.dp)
              .testTag("genie_suggest_petrol")
          ) {
            Text("Fuel stop", color = TextPrimary, fontSize = 14.sp)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable(role = Role.Button) {
                val s = suggestions[4]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .heightIn(min = 48.dp).padding(horizontal = 12.dp, vertical = 12.dp)
              .testTag("genie_suggest_translate")
          ) {
            Text("Translate", color = TextPrimary, fontSize = 14.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
