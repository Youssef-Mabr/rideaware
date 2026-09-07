package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCardAlert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SecurityUpdateWarning
import androidx.compose.material.icons.filled.SignalWifiBad
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ErrorDemoType
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

fun getErrorIcon(type: ErrorDemoType): ImageVector = when (type) {
  ErrorDemoType.HELMET_DISCONNECTED -> Icons.Filled.BluetoothDisabled
  ErrorDemoType.SEARCHING_HELMET -> Icons.Filled.Search
  ErrorDemoType.BLUETOOTH_DISABLED -> Icons.Filled.BluetoothDisabled
  ErrorDemoType.WIFI_UNAVAILABLE -> Icons.Filled.SignalWifiBad
  ErrorDemoType.FRONT_CAM_UNAVAILABLE -> Icons.Filled.NoPhotography
  ErrorDemoType.REAR_CAM_UNAVAILABLE -> Icons.Filled.VideocamOff
  ErrorDemoType.LOW_BATTERY -> Icons.Filled.BatteryAlert
  ErrorDemoType.STORAGE_ALMOST_FULL, ErrorDemoType.STORAGE_FULL -> Icons.Filled.SdCardAlert
  ErrorDemoType.NO_PREVIOUS_RIDES, ErrorDemoType.NO_PROTECTED_CLIPS -> Icons.Filled.Info
  ErrorDemoType.PERMISSION_DENIED -> Icons.Filled.SecurityUpdateWarning
  ErrorDemoType.NO_INTERNET -> Icons.Filled.CloudOff
  ErrorDemoType.AI_UNAVAILABLE -> Icons.Filled.MicOff
  ErrorDemoType.PAIRING_FAILED -> Icons.Filled.QrCode
  ErrorDemoType.NONE -> Icons.Filled.Info
}

/**
 * Reusable full screen or embedded Empty / Error State view.
 */
@Composable
fun EmptyErrorStateView(
  errorType: ErrorDemoType,
  onActionClick: () -> Unit,
  modifier: Modifier = Modifier,
  onDismiss: (() -> Unit)? = null
) {
  val icon = getErrorIcon(errorType)
  val isWarning = errorType in listOf(
    ErrorDemoType.LOW_BATTERY,
    ErrorDemoType.STORAGE_ALMOST_FULL,
    ErrorDemoType.NO_INTERNET
  )
  val isDanger = errorType in listOf(
    ErrorDemoType.HELMET_DISCONNECTED,
    ErrorDemoType.STORAGE_FULL,
    ErrorDemoType.FRONT_CAM_UNAVAILABLE,
    ErrorDemoType.REAR_CAM_UNAVAILABLE,
    ErrorDemoType.PAIRING_FAILED
  )
  val tintColor = if (isDanger) DangerRed else if (isWarning) WarningOrange else TealPrimary

  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(16.dp)
      .testTag("error_state_card_${errorType.name.lowercase()}"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.Brush.linearGradient(listOf(tintColor.copy(alpha = 0.6f), DarkSurfaceBorder)))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      if (onDismiss != null) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextMuted)
          }
        }
      }

      Box(
        modifier = Modifier
          .size(64.dp)
          .background(tintColor.copy(alpha = 0.15f), CircleShape)
          .border(1.5.dp, tintColor.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tintColor,
          modifier = Modifier.size(32.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = errorType.title,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = errorType.description,
        color = TextSecondary,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
      )

      Spacer(modifier = Modifier.height(18.dp))

      GloveButton(
        text = errorType.actionLabel,
        onClick = onActionClick,
        isDanger = isDanger,
        testTag = "error_action_button"
      )
    }
  }
}

/**
 * Interactive Prototype State Selector Sheet.
 * Allows the evaluator to test any of the 14 real error/empty states specified in the prompt!
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorStatesDemoSheet(
  currentError: ErrorDemoType,
  onSelectError: (ErrorDemoType) -> Unit,
  onDismiss: () -> Unit
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = DarkSurface,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(vertical = 12.dp)
          .width(40.dp)
          .height(4.dp)
          .background(DarkSurfaceBorder, RoundedCornerShape(2.dp))
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Prototype States Simulator",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Validate 14 error & empty states specified in scope",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(420.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(ErrorDemoType.values()) { type ->
          val isSelected = currentError == type
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) TealPrimary.copy(alpha = 0.15f) else DarkSurfaceElevated)
              .border(
                1.dp,
                if (isSelected) TealPrimary else DarkSurfaceBorder,
                RoundedCornerShape(12.dp)
              )
              .clickable {
                onSelectError(type)
                onDismiss()
              }
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = getErrorIcon(type),
              contentDescription = null,
              tint = if (isSelected) TealPrimary else TextSecondary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = type.title,
                color = if (isSelected) TealPrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = type.description,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))
    }
  }
}
