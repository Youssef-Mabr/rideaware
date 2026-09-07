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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay

@Composable
fun GenieAssistantScreen(
  modifier: Modifier = Modifier
) {
  var genieState by remember { mutableStateOf(GenieState.SPEAKING) }
  val conversations = remember {
    mutableStateListOf<VoiceConversation>().apply {
      addAll(MockDataProvider.sampleConversations)
    }
  }
  val listState = rememberLazyListState()

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

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Geni AI Voice Assistant",
      subtitle = "Hands-free helmet companion • Inspired by Ali Baba"
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
            text = when (genieState) {
              GenieState.LISTENING -> "LISTENING TO HELMET MIC..."
              GenieState.THINKING -> "PROCESSING QUERY..."
              GenieState.SPEAKING -> "SPEAKING TO INTERCOM"
              GenieState.IDLE -> "READY • SAY \"HEY GENI\""
            },
            color = TealAccent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sound Waveform
        AudioWaveformVisualizer(
          isAnimating = genieState == GenieState.SPEAKING || genieState == GenieState.LISTENING,
          color = TealPrimary,
          modifier = Modifier.width(180.dp).height(28.dp)
        )
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

      // Example Voice Command Suggestion Chips
      Text(
        text = "Tap a suggested voice command to test:",
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.align(Alignment.Start)
      )

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
              .clickable {
                val s = suggestions[0]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("genie_suggest_behind")
          ) {
            Text(suggestions[0].first, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable {
                val s = suggestions[1]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("genie_suggest_battery")
          ) {
            Text(suggestions[1].first, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
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
              .clickable {
                val s = suggestions[2]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("genie_suggest_petrol")
          ) {
            Text(suggestions[2].first, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(DarkSurface)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .clickable {
                val s = suggestions[4]
                conversations.add(VoiceConversation(s.first, s.second))
                genieState = GenieState.SPEAKING
              }
              .padding(horizontal = 8.dp, vertical = 6.dp)
              .testTag("genie_suggest_translate")
          ) {
            Text(suggestions[4].first, color = TextPrimary, fontSize = 11.sp, maxLines = 1)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
