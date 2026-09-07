package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceBorder

/**
 * Centered mobile frame container for responsive desktop / tablet view (approx 390 x 844 dp)
 * and seamless full bleed on mobile phones.
 */
@Composable
fun DeviceFrameContainer(
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF03060A)),
    contentAlignment = Alignment.Center
  ) {
    val isWideScreen = maxWidth > 500.dp

    if (isWideScreen) {
      Box(
        modifier = Modifier
          .padding(vertical = 16.dp)
          .widthIn(max = 412.dp)
          .heightIn(max = 890.dp)
          .shadow(24.dp, shape = RoundedCornerShape(36.dp), spotColor = Color(0x6600E5BC))
          .clip(RoundedCornerShape(36.dp))
          .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(36.dp))
          .background(DarkCanvas)
      ) {
        content()
      }
    } else {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(DarkCanvas)
      ) {
        content()
      }
    }
  }
}
