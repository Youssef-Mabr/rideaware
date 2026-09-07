package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

enum class PairingState {
  SCANNING,
  DISCOVERING,
  SUCCESS,
  RETRY
}

@Composable
fun PairingScreen(
  onPairingComplete: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var pairingState by remember { mutableStateOf(PairingState.SCANNING) }
  var showManualCodeDialog by remember { mutableStateOf(false) }
  var manualCodeInput by remember { mutableStateOf("RA-8820") }

  // Laser scanner animation
  val infiniteTransition = rememberInfiniteTransition(label = "laserScan")
  val laserY by infiniteTransition.animateFloat(
    initialValue = 0.05f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "laserY"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
      .navigationBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Pair Helmet",
      subtitle = "Connect to Geni One",
      onBack = onBack
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
          text = "Scan the QR code inside your helmet.",
          color = TextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Look on the inner lining under the cheek pad.",
          color = TextSecondary,
          fontSize = 13.sp,
          textAlign = TextAlign.Center
        )
      }

      // QR Scanner Viewfinder Frame
      Box(
        modifier = Modifier
          .size(260.dp)
          .clip(RoundedCornerShape(24.dp))
          .background(DarkSurfaceElevated)
          .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
      ) {
        when (pairingState) {
          PairingState.SCANNING -> {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
              val w = size.width
              val h = size.height
              val cornerLen = 28.dp.toPx()
              val strokeW = 4.dp.toPx()
              val color = TealPrimary

              // 4 Viewfinder Corner Brackets
              // Top-Left
              drawLine(color, Offset(0f, 0f), Offset(cornerLen, 0f), strokeW)
              drawLine(color, Offset(0f, 0f), Offset(0f, cornerLen), strokeW)
              // Top-Right
              drawLine(color, Offset(w - cornerLen, 0f), Offset(w, 0f), strokeW)
              drawLine(color, Offset(w, 0f), Offset(w, cornerLen), strokeW)
              // Bottom-Left
              drawLine(color, Offset(0f, h), Offset(cornerLen, h), strokeW)
              drawLine(color, Offset(0f, h - cornerLen), Offset(0f, h), strokeW)
              // Bottom-Right
              drawLine(color, Offset(w - cornerLen, h), Offset(w, h), strokeW)
              drawLine(color, Offset(w, h - cornerLen), Offset(w, h), strokeW)

              // Laser Scanning Line
              val currentLaserY = h * laserY
              drawLine(
                color = TealAccent,
                start = Offset(0f, currentLaserY),
                end = Offset(w, currentLaserY),
                strokeWidth = 2.5.dp.toPx()
              )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = TealPrimary.copy(alpha = 0.6f),
                modifier = Modifier.size(54.dp)
              )
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Aligning QR Token...",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
            }
          }

          PairingState.DISCOVERING -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              CircularProgressIndicator(
                color = TealPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
              )
              Spacer(modifier = Modifier.height(16.dp))
              Text(
                text = "Found Helmet: RA-8820",
                color = TealAccent,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Negotiating Bluetooth & Wi-Fi tokens...",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
              )
            }
          }

          PairingState.SUCCESS -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(60.dp)
                  .background(SuccessGreen.copy(alpha = 0.2f), CircleShape)
                  .border(2.dp, SuccessGreen, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.CheckCircle,
                  contentDescription = null,
                  tint = SuccessGreen,
                  modifier = Modifier.size(36.dp)
                )
              }
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Geni One Connected!",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Secure pairing token verified",
                color = SuccessGreen,
                fontSize = 12.sp
              )
            }
          }

          PairingState.RETRY -> {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Box(
                modifier = Modifier
                  .size(54.dp)
                  .background(DangerRed.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Refresh,
                  contentDescription = null,
                  tint = DangerRed,
                  modifier = Modifier.size(30.dp)
                )
              }
              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = "Pairing Timeout",
                color = DangerRed,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Ensure helmet is switched on",
                color = TextSecondary,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Feature Info Pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .background(DarkSurface, RoundedCornerShape(20.dp))
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
          .padding(horizontal = 14.dp, vertical = 6.dp)
      ) {
        Text(
          text = "Bluetooth setup • Wi-Fi video • No cable",
          color = TextSecondary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // Actions based on state
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Progress Dots (Step 3 of 4)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(bottom = 14.dp)
        ) {
          Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
          Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
          Box(modifier = Modifier.size(24.dp, 6.dp).background(TealPrimary, RoundedCornerShape(3.dp)))
          Box(modifier = Modifier.size(6.dp).background(DarkSurfaceBorder, CircleShape))
        }

        when (pairingState) {
          PairingState.SCANNING -> {
            GloveButton(
              text = "Scan Helmet QR",
              onClick = {
                pairingState = PairingState.DISCOVERING
              },
              icon = Icons.Filled.QrCodeScanner,
              testTag = "scan_helmet_qr_button"
            )

            Spacer(modifier = Modifier.height(10.dp))

            GloveOutlinedButton(
              text = "Enter pairing code instead",
              onClick = { showManualCodeDialog = true },
              icon = Icons.Filled.Keyboard,
              testTag = "enter_pairing_code_button"
            )
          }

          PairingState.DISCOVERING -> {
            LaunchedEffect(Unit) {
              delay(1600)
              pairingState = PairingState.SUCCESS
            }
            GloveOutlinedButton(
              text = "Cancel Pairing",
              onClick = { pairingState = PairingState.SCANNING },
              testTag = "cancel_pairing_button"
            )
          }

          PairingState.SUCCESS -> {
            GloveButton(
              text = "Proceed to System Checks",
              onClick = onPairingComplete,
              icon = Icons.Filled.CheckCircle,
              testTag = "proceed_setup_button"
            )
          }

          PairingState.RETRY -> {
            GloveButton(
              text = "Try Scanning Again",
              onClick = { pairingState = PairingState.SCANNING },
              icon = Icons.Filled.Refresh,
              testTag = "retry_pairing_button"
            )
          }
        }
      }
    }
  }

  // Manual Pairing Code Modal
  if (showManualCodeDialog) {
    AlertDialog(
      onDismissRequest = { showManualCodeDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Enter Helmet Pairing Code", fontWeight = FontWeight.Bold, fontSize = 18.sp)
      },
      text = {
        Column {
          Text(
            text = "Enter the 6-character identifier printed near the helmet USB-C port.",
            color = TextSecondary,
            fontSize = 13.sp
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = manualCodeInput,
            onValueChange = { manualCodeInput = it },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedBorderColor = TealPrimary,
              unfocusedBorderColor = DarkSurfaceBorder
            ),
            modifier = Modifier.fillMaxWidth().testTag("manual_code_input")
          )
        }
      },
      confirmButton = {
        GloveButton(
          text = "Connect Helmet",
          onClick = {
            showManualCodeDialog = false
            pairingState = PairingState.DISCOVERING
          },
          modifier = Modifier.width(160.dp),
          testTag = "confirm_manual_code_button"
        )
      },
      dismissButton = {
        TextButton(onClick = { showManualCodeDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}
