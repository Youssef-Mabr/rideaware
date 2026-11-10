package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helmet.ButtonAction
import com.example.helmet.HelmetButton
import com.example.helmet.HelmetConnectionState
import com.example.helmet.LedPattern
import com.example.model.HelmetStatus
import com.example.ui.HelmetCommunicationUiState
import com.example.ui.HelmetCommunicationViewModel
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
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.delay

@Composable
fun HelmetManagementScreen(
  helmetStatus: HelmetStatus,
  onUnpairHelmet: () -> Unit,
  modifier: Modifier = Modifier,
  commViewModel: HelmetCommunicationViewModel = viewModel()
) {
  val commState by commViewModel.state.collectAsStateWithLifecycle()
  var storageNotice by remember { mutableStateOf<String?>(null) }
  var isUpdatingFirmware by rememberSaveable { mutableStateOf(false) }
  var updateProgress by rememberSaveable { mutableFloatStateOf(0f) }
  var showUnpairDialog by rememberSaveable { mutableStateOf(false) }

  // Hardware Subsystem Toggles
  var frontHdr by rememberSaveable { mutableStateOf(true) }
  var rearBlindSpotRadar by rememberSaveable { mutableStateOf(true) }
  var windNoiseSuppression by rememberSaveable { mutableStateOf(true) }
  var ledBeaconEnabled by rememberSaveable { mutableStateOf(true) }
  var activeLedPattern by rememberSaveable { mutableStateOf(LedPattern.PULSE_TEAL) }

  val isConnected = commState.connectionState is HelmetConnectionState.Connected
  val displayBattery = commState.latestBattery?.percent ?: helmetStatus.batteryPercent

  LaunchedEffect(isUpdatingFirmware) {
    if (isUpdatingFirmware) {
      updateProgress = 0f
      while (updateProgress < 1f) {
        delay(250)
        updateProgress = (updateProgress + 0.15f).coerceAtMost(1f)
      }
      isUpdatingFirmware = false
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Smart Helmet",
      subtitle = if (isConnected) "Geni One • AptX Low Latency link active" else "Standby • Demo connection ready"
    )

    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. HERO HELMET HUD CARD
      HelmetHeroHudCard(
        commState = commState,
        displayBattery = displayBattery,
        onConnect = { commViewModel.connectDemoHelmet() },
        onDisconnect = { commViewModel.disconnectDemoHelmet() },
        onSendPing = { commViewModel.sendTestPing() }
      )

      // 2. HARDWARE SUBSYSTEMS & OPTICS GRID
      SubsystemsControlGrid(
        frontHdr = frontHdr,
        onFrontHdrChange = { frontHdr = it },
        rearRadar = rearBlindSpotRadar,
        onRearRadarChange = { rearBlindSpotRadar = it },
        windCut = windNoiseSuppression,
        onWindCutChange = { windNoiseSuppression = it },
        ledBeaconEnabled = ledBeaconEnabled,
        onLedBeaconEnabledChange = { enabled ->
          ledBeaconEnabled = enabled
          if (enabled) {
            commViewModel.sendTestLedCommand(activeLedPattern)
          } else {
            commViewModel.sendTestLedCommand(LedPattern.OFF)
          }
        },
        activeLedPattern = activeLedPattern,
        onSelectLedPattern = { pattern ->
          activeLedPattern = pattern
          commViewModel.sendTestLedCommand(pattern)
        }
      )

      // 3. STAGE 6B SIMULATOR & DIAGNOSTIC DECK
      Stage6BSimulatorDeck(
        commState = commState,
        commViewModel = commViewModel
      )

      // 4. STORAGE & FIRMWARE MAINTENANCE
      StorageAndMaintenanceCard(
        isUpdatingFirmware = isUpdatingFirmware,
        updateProgress = updateProgress,
        onUpdateFirmware = { isUpdatingFirmware = true },
        onFormatStorage = { storageNotice = "MicroSD card format simulation: Storage cleared." },
        onBackupVault = { storageNotice = "Vault clips queued for cloud sync." }
      )

      // 5. UNPAIR / RESET HELMET ACTION
      GloveButton(
        text = "Unpair helmet & reset",
        onClick = { showUnpairDialog = true },
        icon = Icons.Filled.BluetoothDisabled,
        isDanger = true,
        testTag = "unpair_helmet_button"
      )

      Spacer(modifier = Modifier.height(24.dp))
    }
  }

  if (storageNotice != null) {
    AlertDialog(
      onDismissRequest = { storageNotice = null },
      containerColor = DarkSurface,
      title = { Text("Storage vault", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text(storageNotice ?: "", color = TextSecondary, fontSize = 13.sp) },
      confirmButton = {
        TextButton(onClick = { storageNotice = null }) { Text("Close", color = TealAccent) }
      }
    )
  }

  if (showUnpairDialog) {
    AlertDialog(
      onDismissRequest = { showUnpairDialog = false },
      containerColor = DarkSurface,
      title = {
        Text("Unpair helmet?", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "This will reset helmet setup and return to pairing. Your ride history and emergency contacts will remain saved.",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        GloveButton(
          text = "Confirm unpair",
          onClick = {
            showUnpairDialog = false
            onUnpairHelmet()
          },
          isDanger = true,
          modifier = Modifier.width(160.dp),
          testTag = "confirm_unpair_button"
        )
      },
      dismissButton = {
        TextButton(onClick = { showUnpairDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

/**
 * High-tech Helmet Hero Card with visual helmet outline, live telemetry ribbon, and primary status switch.
 */
@Composable
private fun HelmetHeroHudCard(
  commState: HelmetCommunicationUiState,
  displayBattery: Int,
  onConnect: () -> Unit,
  onDisconnect: () -> Unit,
  onSendPing: () -> Unit
) {
  val connectionState = commState.connectionState
  val isConnected = connectionState is HelmetConnectionState.Connected
  val isConnecting = connectionState is HelmetConnectionState.Connecting
  val isError = connectionState is HelmetConnectionState.Error

  val borderBrush = Brush.linearGradient(
    colors = when {
      isConnected -> listOf(TealPrimary.copy(alpha = 0.8f), CyanAccent.copy(alpha = 0.4f))
      isConnecting -> listOf(CyanAccent.copy(alpha = 0.8f), TealPrimary.copy(alpha = 0.2f))
      isError -> listOf(WarningOrange.copy(alpha = 0.9f), DangerRed.copy(alpha = 0.4f))
      else -> listOf(DarkSurfaceBorder, DarkSurfaceBorder)
    }
  )

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .background(DarkSurface)
      .border(1.5.dp, borderBrush, RoundedCornerShape(24.dp))
      .padding(18.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      // Top status row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(10.dp)
              .background(
                color = when {
                  isConnected -> SuccessGreen
                  isConnecting -> CyanAccent
                  isError -> WarningOrange
                  else -> TextMuted
                },
                shape = CircleShape
              )
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = when (connectionState) {
              is HelmetConnectionState.Connected -> "LINK ACTIVE [DEMO]"
              is HelmetConnectionState.Connecting -> "CONNECTING... [DEMO]"
              is HelmetConnectionState.Error -> "LINK ERROR [DEMO]"
              is HelmetConnectionState.Disconnected -> "STANDBY [DEMO]"
            },
            color = when {
              isConnected -> SuccessGreen
              isConnecting -> CyanAccent
              isError -> WarningOrange
              else -> TextSecondary
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
          )
        }

        Box(
          modifier = Modifier
            .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
            .border(1.dp, TealPrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text("GENI ONE • REV 3", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Center visual: Stylized Helmet Silhouette + Visor Radar Wave
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "RideAware Sentinel X1",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = if (isConnected) "Dual 1080p Optics • AptX Low-Latency Active" else "Ready to pair with simulated helmet stream",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }

        // Helmet Canvas HUD Graphic
        HelmetCanvasGraphic(
          isConnected = isConnected,
          modifier = Modifier.size(76.dp)
        )
      }

      // Telemetry Ribbon: Battery, Link Quality, Storage
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Battery Meter
        Box(
          modifier = Modifier
            .weight(1f)
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.BatteryChargingFull,
                contentDescription = null,
                tint = if (displayBattery > 20) TealPrimary else WarningOrange,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Battery", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text(
              text = "$displayBattery%",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.testTag("demo_helmet_battery_label")
            )
            Text(
              text = commState.latestBattery?.let { if (it.isCharging) "Charging" else "~5.5h left" } ?: "~5.5h left",
              color = TextMuted,
              fontSize = 10.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        }

        // Bluetooth Link Quality
        Box(
          modifier = Modifier
            .weight(1f)
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.BluetoothConnected,
                contentDescription = null,
                tint = CyanAccent,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("BT Audio", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text(
              text = if (isConnected) "-58 dBm" else "Offline",
              color = if (isConnected) CyanAccent else TextMuted,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Text("AptX-LL • 24ms", color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        // Vault Memory
        Box(
          modifier = Modifier
            .weight(1f)
            .height(76.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceElevated)
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                Icons.Filled.SdCard,
                contentDescription = null,
                tint = TealAccent,
                modifier = Modifier.size(14.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Vault SD", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
            Text("72 GB", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("of 128 GB", color = TextMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }

      // Primary Quick Action
      when (connectionState) {
        is HelmetConnectionState.Disconnected -> {
          GloveButton(
            text = "Connect Demo Helmet",
            onClick = onConnect,
            icon = Icons.Filled.Bluetooth,
            testTag = "demo_helmet_connect_button"
          )
        }
        is HelmetConnectionState.Connecting -> {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = TealPrimary, strokeWidth = 2.5.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Linking with Demo Helmet...", color = TealAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
          }
        }
        is HelmetConnectionState.Connected -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            GloveOutlinedButton(
              text = "Send Ping",
              onClick = onSendPing,
              icon = Icons.Filled.Sensors,
              minHeight = 48.dp,
              modifier = Modifier.weight(1f),
              testTag = "demo_helmet_send_ping_button"
            )
            GloveOutlinedButton(
              text = "Disconnect",
              onClick = onDisconnect,
              icon = Icons.Filled.PowerSettingsNew,
              minHeight = 48.dp,
              textColor = DangerRed,
              borderColor = DangerRed.copy(alpha = 0.5f),
              modifier = Modifier.weight(1f),
              testTag = "demo_helmet_disconnect_button"
            )
          }
        }
        is HelmetConnectionState.Error -> {
          GloveButton(
            text = "Recover Link",
            onClick = onConnect,
            icon = Icons.Filled.Refresh,
            testTag = "demo_helmet_reconnect_button"
          )
        }
      }
    }
  }
}

/**
 * Stylized Canvas illustration of a smart motorcycle helmet with illuminated visor and radar arcs.
 */
@Composable
private fun HelmetCanvasGraphic(isConnected: Boolean, modifier: Modifier = Modifier) {
  val infiniteTransition = rememberInfiniteTransition(label = "helmetGlow")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 0.95f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    // Outer aura ring
    drawCircle(
      color = if (isConnected) TealPrimary.copy(alpha = 0.15f * pulseAlpha) else Color.White.copy(alpha = 0.05f),
      radius = w * 0.46f,
      center = Offset(w * 0.5f, h * 0.5f)
    )

    // Helmet outer shell silhouette path
    val helmetPath = Path().apply {
      moveTo(w * 0.25f, h * 0.75f)
      // Chin guard & jaw
      lineTo(w * 0.45f, h * 0.85f)
      lineTo(w * 0.75f, h * 0.80f)
      // Visor bottom corner
      lineTo(w * 0.82f, h * 0.55f)
      // Crown curvature
      cubicTo(w * 0.85f, h * 0.25f, w * 0.40f, h * 0.12f, w * 0.20f, h * 0.40f)
      // Rear neck protection
      cubicTo(w * 0.15f, h * 0.55f, w * 0.18f, h * 0.70f, w * 0.25f, h * 0.75f)
      close()
    }

    drawPath(
      path = helmetPath,
      color = Color(0xFF141C28)
    )
    drawPath(
      path = helmetPath,
      color = if (isConnected) TealPrimary.copy(alpha = 0.8f) else Color(0xFF2C394E),
      style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Visor Aperture
    val visorPath = Path().apply {
      moveTo(w * 0.48f, h * 0.40f)
      lineTo(w * 0.80f, h * 0.45f)
      lineTo(w * 0.72f, h * 0.62f)
      lineTo(w * 0.44f, h * 0.55f)
      close()
    }
    drawPath(
      path = visorPath,
      color = if (isConnected) CyanAccent.copy(alpha = 0.7f * pulseAlpha) else Color(0xFF0D121B)
    )
    drawPath(
      path = visorPath,
      color = if (isConnected) TealAccent else Color(0xFF3B485E),
      style = Stroke(width = 1.5.dp.toPx())
    )

    // Front Camera Lens dot
    drawCircle(
      color = if (isConnected) TealAccent else Color.Gray,
      radius = 2.5.dp.toPx(),
      center = Offset(w * 0.82f, h * 0.38f)
    )

    // Rear Radar Wave Indicator
    if (isConnected) {
      drawArc(
        color = CyanAccent.copy(alpha = 0.8f * pulseAlpha),
        startAngle = 135f,
        sweepAngle = 90f,
        useCenter = false,
        topLeft = Offset(w * 0.05f, h * 0.42f),
        size = Size(w * 0.24f, h * 0.24f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
      )
    }
  }
}

/**
 * 2x2 Subsystems Quick-Control tactile grid with quick toggles.
 */
@Composable
private fun SubsystemsControlGrid(
  frontHdr: Boolean,
  onFrontHdrChange: (Boolean) -> Unit,
  rearRadar: Boolean,
  onRearRadarChange: (Boolean) -> Unit,
  windCut: Boolean,
  onWindCutChange: (Boolean) -> Unit,
  ledBeaconEnabled: Boolean,
  onLedBeaconEnabledChange: (Boolean) -> Unit,
  activeLedPattern: LedPattern,
  onSelectLedPattern: (LedPattern) -> Unit
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Subsystems & Optics", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
      Text("Tactile Controls", color = TextMuted, fontSize = 12.sp)
    }

    // Row 1: Front Optics & Rear Radar
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SubsystemTile(
        title = "Front 1080p HDR",
        subtitle = if (frontHdr) "60 FPS Active" else "Camera Standby",
        icon = Icons.Filled.CameraAlt,
        iconTint = TealPrimary,
        isChecked = frontHdr,
        onCheckedChange = onFrontHdrChange,
        modifier = Modifier.weight(1f)
      )

      SubsystemTile(
        title = "Rear Blind-Radar",
        subtitle = if (rearRadar) "140° Radar Guard" else "Radar Muted",
        icon = Icons.Filled.Videocam,
        iconTint = CyanAccent,
        isChecked = rearRadar,
        onCheckedChange = onRearRadarChange,
        modifier = Modifier.weight(1f)
      )
    }

    // Row 2: Wind-Cut Mic & Dual Safety LED Beacons
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      SubsystemTile(
        title = "Wind-Cut Mic",
        subtitle = if (windCut) "Beamforming On" else "Standard Mic",
        icon = Icons.Filled.Mic,
        iconTint = TealAccent,
        isChecked = windCut,
        onCheckedChange = onWindCutChange,
        modifier = Modifier.weight(1f)
      )

      SubsystemTile(
        title = "Safety LED Beacon",
        subtitle = if (ledBeaconEnabled) activeLedPattern.name.replace('_', ' ') else "Beacons Off",
        icon = Icons.Filled.LightMode,
        iconTint = WarningOrange,
        isChecked = ledBeaconEnabled,
        onCheckedChange = onLedBeaconEnabledChange,
        modifier = Modifier.weight(1f)
      )
    }

    // Symmetric LED Beacon Mode Selector (visible when LED Beacon is enabled)
    AnimatedVisibility(visible = ledBeaconEnabled) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, WarningOrange.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
          .padding(8.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Safety Beacon Strobe Mode", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text("TAP TO SELECT", color = WarningOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold)
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            LedModePill(
              label = "Pulse Teal",
              selected = activeLedPattern == LedPattern.PULSE_TEAL,
              modifier = Modifier.weight(1f)
            ) {
              onSelectLedPattern(LedPattern.PULSE_TEAL)
            }
            LedModePill(
              label = "Hazard Flash",
              selected = activeLedPattern == LedPattern.HAZARD_AMBER_FLASH,
              modifier = Modifier.weight(1f)
            ) {
              onSelectLedPattern(LedPattern.HAZARD_AMBER_FLASH)
            }
            LedModePill(
              label = "Solid White",
              selected = activeLedPattern == LedPattern.SOLID_WHITE,
              modifier = Modifier.weight(1f)
            ) {
              onSelectLedPattern(LedPattern.SOLID_WHITE)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SubsystemTile(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  isChecked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .height(114.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(DarkSurface)
      .border(
        1.dp,
        if (isChecked) iconTint.copy(alpha = 0.4f) else DarkSurfaceBorder,
        RoundedCornerShape(16.dp)
      )
      .clickable { onCheckedChange(!isChecked) }
      .padding(12.dp)
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Switch(
          checked = isChecked,
          onCheckedChange = onCheckedChange,
          colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF041912),
            checkedTrackColor = TealPrimary,
            uncheckedThumbColor = TextMuted,
            uncheckedTrackColor = DarkSurfaceElevated,
            uncheckedBorderColor = DarkSurfaceBorder
          )
        )
      }
      Column {
        Text(
          text = title,
          color = TextPrimary,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = subtitle,
          color = if (isChecked) iconTint else TextMuted,
          fontSize = 11.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}

@Composable
private fun LedModePill(
  label: String,
  selected: Boolean,
  modifier: Modifier = Modifier,
  onClick: () -> Unit
) {
  Box(
    modifier = modifier
      .height(36.dp)
      .clip(RoundedCornerShape(8.dp))
      .background(if (selected) WarningOrange.copy(alpha = 0.2f) else DarkSurface)
      .border(1.dp, if (selected) WarningOrange else DarkSurfaceBorder, RoundedCornerShape(8.dp))
      .clickable(onClick = onClick),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = label,
      color = if (selected) WarningOrange else TextSecondary,
      fontSize = 11.sp,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}

/**
 * Stage 6B Mock Helmet & Telemetry Deck with Outbound & Inbound Command Buttons,
 * telemetry indicators, and test failure simulation.
 */
@Composable
private fun Stage6BSimulatorDeck(
  commState: HelmetCommunicationUiState,
  commViewModel: HelmetCommunicationViewModel
) {
  val isConnected = commState.connectionState is HelmetConnectionState.Connected
  var showDetails by rememberSaveable { mutableStateOf(true) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(DarkSurface)
      .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
      .padding(16.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
      // Header with expand/collapse
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .background(TealPrimary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Filled.Tune, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text("Simulator & Diagnostics", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Stage 6B Testing Deck • No real BLE needed", color = TextSecondary, fontSize = 11.sp)
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, TealPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text("DEMO / SIMULATED", color = TealAccent, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
      }

      // Telemetry Status Strip (Button Event, Command Ack)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        // Last Button Event
        Box(
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .padding(8.dp)
        ) {
          Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Last Button Event", color = TextSecondary, fontSize = 10.sp)
            Text(
              text = commState.latestButtonEvent?.let { "${it.button} ${it.action}" } ?: "None [Awaiting]",
              color = CyanAccent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("demo_helmet_button_label")
            )
          }
        }

        // Command Ack
        Box(
          modifier = Modifier
            .weight(1f)
            .height(58.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .padding(8.dp)
        ) {
          Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Text("Command Ack", color = TextSecondary, fontSize = 10.sp)
            Text(
              text = commState.latestAck?.let { if (it.success) "ACK: OK" else "REJECTED" } ?: "Ready [DEMO]",
              color = if (commState.latestAck?.success == false) DangerRed else SuccessGreen,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier.testTag("demo_helmet_ack_label")
            )
          }
        }
      }

      // Outbound commands
      Text("Outbound Commands (RideAware → Mock Helmet):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SimulatorActionButton(
          text = "Ping",
          onClick = { commViewModel.sendTestPing() },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_send_ping_button"
        )
        SimulatorActionButton(
          text = "Req Battery",
          onClick = { commViewModel.sendTestBatteryRequest() },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_req_battery_button"
        )
        SimulatorActionButton(
          text = "Pulse LED",
          onClick = { commViewModel.sendTestLedCommand(LedPattern.PULSE_TEAL) },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_set_led_button"
        )
      }

      // Inbound telemetry events
      Text("Simulate Inbound Helmet Events (Mock Helmet → RideAware):", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SimulatorActionButton(
          text = "PTT Press",
          onClick = { commViewModel.sendSampleButtonEvent(HelmetButton.VOICE_PTT, ButtonAction.PRESS) },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_btn_ptt_button"
        )
        SimulatorActionButton(
          text = "SOS Press",
          onClick = { commViewModel.sendSampleButtonEvent(HelmetButton.SOS, ButtonAction.PRESS) },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_btn_sos_button"
        )
        SimulatorActionButton(
          text = "Set Batt 74%",
          onClick = { commViewModel.sendSampleBattery(74) },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_send_battery_button"
        )
      }

      // Failure & Recovery Simulation
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        SimulatorActionButton(
          text = "Sim Failure",
          onClick = { commViewModel.simulateConnectionFailure(recoverable = true) },
          borderColor = DangerRed.copy(alpha = 0.6f),
          textColor = DangerRed,
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_fail_button"
        )
        SimulatorActionButton(
          text = "Recover Link",
          onClick = { commViewModel.reconnectAndRecover() },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_reconnect_button"
        )
        SimulatorActionButton(
          text = "Reset Disc",
          onClick = { commViewModel.disconnectDemoHelmet() },
          modifier = Modifier.weight(1f),
          testTag = "demo_helmet_reset_button"
        )
      }

      // Stage 6B Event Log
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(DarkCanvas)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
          .padding(8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("Communication Log", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          if (commState.recentLogs.isNotEmpty()) {
            Text(
              "Clear",
              color = TealAccent,
              fontSize = 11.sp,
              modifier = Modifier.clickable { commViewModel.clearLogs() }
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (commState.recentLogs.isEmpty()) {
          Text("No events yet. Dispatch a command or button event above.", color = TextMuted, fontSize = 11.sp)
        } else {
          commState.recentLogs.take(4).forEach { log ->
            Text(
              text = log,
              color = TextSecondary,
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              lineHeight = 13.sp
            )
          }
        }
      }
    }
  }
}

/**
 * Storage Vault and Firmware Update Maintenance Card.
 */
@Composable
private fun StorageAndMaintenanceCard(
  isUpdatingFirmware: Boolean,
  updateProgress: Float,
  onUpdateFirmware: () -> Unit,
  onFormatStorage: () -> Unit,
  onBackupVault: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(20.dp))
      .background(DarkSurface)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Firmware Section
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Firmware v2.4.1", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(
          if (isUpdatingFirmware) "Installing update preview..." else "Up to date • Production channel",
          color = if (isUpdatingFirmware) TealAccent else TextSecondary,
          fontSize = 12.sp
        )
      }

      if (!isUpdatingFirmware) {
        GloveOutlinedButton(
          text = "Check update",
          onClick = onUpdateFirmware,
          minHeight = 44.dp,
          fontSize = 13.sp,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          modifier = Modifier.width(130.dp),
          testTag = "check_firmware_update"
        )
      }
    }

    if (isUpdatingFirmware) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
          progress = { updateProgress.coerceIn(0f, 1f) },
          color = TealPrimary,
          trackColor = DarkSurfaceElevated,
          modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        )
        Text("Progress: ${(updateProgress * 100).toInt()}%", color = TealAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    // Storage Section
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("MicroSD Vault (72 GB free / 128 GB)", color = TextSecondary, fontSize = 12.sp)
        Text("44% used", color = TealAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
      LinearProgressIndicator(
        progress = { 0.44f },
        color = TealPrimary,
        trackColor = DarkSurfaceElevated,
        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        GloveOutlinedButton(
          text = "Format SD",
          onClick = onFormatStorage,
          minHeight = 46.dp,
          fontSize = 13.sp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f),
          testTag = "format_sd_button"
        )
        GloveOutlinedButton(
          text = "Backup Vault",
          onClick = onBackupVault,
          minHeight = 46.dp,
          fontSize = 13.sp,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
          modifier = Modifier.weight(1f),
          testTag = "backup_clips_button"
        )
      }
    }
  }
}

/**
 * Standardized simulator button with fixed 44dp height and pixel-perfect alignment.
 */
@Composable
private fun SimulatorActionButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  borderColor: Color = TealPrimary.copy(alpha = 0.4f),
  textColor: Color = TextPrimary,
  testTag: String
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .height(44.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(10.dp),
    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
    border = ButtonDefaults.outlinedButtonBorder.copy(
      brush = Brush.horizontalGradient(listOf(borderColor, borderColor))
    )
  ) {
    Text(
      text = text,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      textAlign = TextAlign.Center,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis
    )
  }
}
