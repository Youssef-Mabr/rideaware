package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSource
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.components.SimulatedRoadScene
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LiveCameraScreen(
  onSaveMoment: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var currentSource by remember { mutableStateOf(CameraSource.FRONT) }
  var isSplitView by remember { mutableStateOf(false) }
  var showDetectionBoxes by remember { mutableStateOf(true) }
  var isFullScreen by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    if (!isFullScreen) {
      RideAwareTopBar(
        title = "Live Helmet Feeds",
        subtitle = if (isSplitView) "Dual Synchronized HUD" else "${if (currentSource == CameraSource.FRONT) "Front View" else "Rear Radar"} Stream",
        onBack = onBack,
        actions = {
          IconButton(
            onClick = { isFullScreen = true },
            modifier = Modifier.testTag("camera_fullscreen_toggle")
          ) {
            Icon(
              imageVector = Icons.Filled.Fullscreen,
              contentDescription = "Full Screen",
              tint = TealPrimary
            )
          }
        }
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = if (isFullScreen) 8.dp else 16.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Main Camera Feed View Area
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        if (isSplitView) {
          // Split View: Top Front, Bottom Rear
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(modifier = Modifier.weight(1f)) {
              SimulatedRoadScene(
                source = CameraSource.FRONT,
                showDetectionBoxes = showDetectionBoxes,
                modifier = Modifier.fillMaxSize()
              )
            }
            Box(modifier = Modifier.weight(1f)) {
              SimulatedRoadScene(
                source = CameraSource.REAR,
                showDetectionBoxes = showDetectionBoxes,
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        } else {
          // Single Full Camera View (Front or Rear)
          SimulatedRoadScene(
            source = currentSource,
            showDetectionBoxes = showDetectionBoxes,
            modifier = Modifier.fillMaxSize()
          )
        }

        // Full Screen Exit Floating Button
        if (isFullScreen) {
          IconButton(
            onClick = { isFullScreen = false },
            modifier = Modifier
              .padding(12.dp)
              .align(Alignment.TopEnd)
              .background(Color(0xCC000000), CircleShape)
          ) {
            Icon(
              imageVector = Icons.Filled.FullscreenExit,
              contentDescription = "Exit Fullscreen",
              tint = Color.White
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Control Toolbar: Mode Selectors, Object Detection Toggle, Save Moment
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Mode Selector Tabs (Front, Rear, Split)
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(4.dp),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          // Front Mode Tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (!isSplitView && currentSource == CameraSource.FRONT) TealPrimary else Color.Transparent)
              .clickable {
                isSplitView = false
                currentSource = CameraSource.FRONT
              }
              .padding(vertical = 10.dp)
              .testTag("tab_cam_front"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Front (1080p)",
              color = if (!isSplitView && currentSource == CameraSource.FRONT) Color(0xFF020408) else TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Rear Mode Tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (!isSplitView && currentSource == CameraSource.REAR) TealPrimary else Color.Transparent)
              .clickable {
                isSplitView = false
                currentSource = CameraSource.REAR
              }
              .padding(vertical = 10.dp)
              .testTag("tab_cam_rear"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Rear Radar",
              color = if (!isSplitView && currentSource == CameraSource.REAR) Color(0xFF020408) else TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Split Mode Tab
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSplitView) TealPrimary else Color.Transparent)
              .clickable { isSplitView = true }
              .padding(vertical = 10.dp)
              .testTag("tab_cam_split"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "Split Dual",
              color = if (isSplitView) Color(0xFF020408) else TextSecondary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Secondary Action Row: Object Detection Toggle & Save Moment
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Object Detection Toggle Button
          Row(
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurfaceElevated)
              .border(
                1.dp,
                if (showDetectionBoxes) TealPrimary.copy(alpha = 0.5f) else DarkSurfaceBorder,
                RoundedCornerShape(14.dp)
              )
              .clickable { showDetectionBoxes = !showDetectionBoxes }
              .padding(horizontal = 12.dp)
              .testTag("toggle_detection_boxes"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = if (showDetectionBoxes) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
              contentDescription = null,
              tint = if (showDetectionBoxes) TealPrimary else TextMuted,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (showDetectionBoxes) "AI Boxes: ON" else "AI Boxes: OFF",
              color = if (showDetectionBoxes) TextPrimary else TextMuted,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          // Save Moment Primary Action
          Button(
            onClick = onSaveMoment,
            modifier = Modifier
              .weight(1.3f)
              .height(52.dp)
              .testTag("live_camera_save_moment"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = TealPrimary,
              contentColor = Color(0xFF041912)
            )
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Save Moment", fontSize = 14.sp, fontWeight = FontWeight.Black)
            }
          }
        }
      }
    }
  }
}
