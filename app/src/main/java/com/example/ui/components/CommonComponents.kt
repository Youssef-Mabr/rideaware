package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CameraSource
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DangerGlow
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealDark
import com.example.ui.theme.TealGlow
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningOrange

/**
 * High-touch glove-friendly primary button for motorcycle riders.
 * Min height 56dp with crisp high contrast text and tactile glow.
 */
@Composable
fun GloveButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  isDanger: Boolean = false,
  testTag: String = "glove_button"
) {
  val baseColor = if (isDanger) DangerRed else TealPrimary
  val contentColor = if (isDanger) Color.White else Color(0xFF020408)

  Button(
    onClick = onClick,
    enabled = enabled,
    modifier = modifier
      .fillMaxWidth()
      .height(56.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(18.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = baseColor,
      contentColor = contentColor,
      disabledContainerColor = DarkSurfaceElevated,
      disabledContentColor = TextMuted
    ),
    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 6.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
      }
      Text(
        text = text,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      )
    }
  }
}

/**
 * Secondary outlined glove button.
 */
@Composable
fun GloveOutlinedButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  borderColor: Color = DarkSurfaceBorder,
  textColor: Color = TextPrimary,
  testTag: String = "glove_outlined_button"
) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier
      .fillMaxWidth()
      .height(52.dp)
      .testTag(testTag),
    shape = RoundedCornerShape(16.dp),
    colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(borderColor, borderColor)))
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(text = text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

/**
 * Readiness / Connection Status Pill with pulsing live indicator.
 */
@Composable
fun StatusPill(
  label: String,
  isActive: Boolean,
  modifier: Modifier = Modifier,
  activeColor: Color = TealPrimary,
  inactiveColor: Color = TextMuted
) {
  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseAlpha"
  )

  Row(
    modifier = modifier
      .background(
        color = Color.White.copy(alpha = 0.05f),
        shape = RoundedCornerShape(100.dp)
      )
      .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.1f),
        shape = RoundedCornerShape(100.dp)
      )
      .padding(horizontal = 12.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(8.dp)
        .background(
          color = if (isActive) activeColor else inactiveColor,
          shape = CircleShape
        )
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      color = if (isActive) Color.White else TextMuted,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

/**
 * Common Top Bar with back navigation and clear action icon.
 */
@Composable
fun RideAwareTopBar(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  actions: @Composable (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    if (onBack != null) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .size(44.dp)
          .background(DarkSurfaceElevated, CircleShape)
          .border(1.dp, DarkSurfaceBorder, CircleShape)
          .testTag("top_bar_back_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Back",
          tint = TextPrimary
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        color = TextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
      if (subtitle != null) {
        Text(
          text = subtitle,
          color = TextSecondary,
          fontSize = 12.sp,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    if (actions != null) {
      actions()
    }
  }
}

/**
 * Metric Card for speed, battery, storage, distance.
 */
@Composable
fun MetricCard(
  title: String,
  value: String,
  unit: String? = null,
  icon: ImageVector? = null,
  accentColor: Color = TealPrimary,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(DarkSurface)
      .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(text = title, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (icon != null) {
          Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Row(verticalAlignment = Alignment.Bottom) {
        Text(
          text = value,
          color = TextPrimary,
          fontSize = 24.sp,
          fontWeight = FontWeight.Black
        )
        if (unit != null) {
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = unit,
            color = TextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 3.dp)
          )
        }
      }
    }
  }
}

/**
 * Animated Sound Waveform visualizer for Genie listening & speaking states.
 */
@Composable
fun AudioWaveformVisualizer(
  isAnimating: Boolean = true,
  color: Color = TealPrimary,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "audioWave")
  val phase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 6.28f,
    animationSpec = infiniteRepeatable(
      animation = tween(1400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "audioPhase"
  )

  Canvas(modifier = modifier) {
    val barCount = 18
    val barWidth = size.width / (barCount * 1.8f)
    val spacing = barWidth * 0.8f
    val startX = (size.width - (barCount * (barWidth + spacing))) / 2f
    val centerY = size.height / 2f

    for (i in 0 until barCount) {
      val x = startX + i * (barWidth + spacing)
      val waveFactor = if (isAnimating) {
        kotlin.math.abs(kotlin.math.sin(phase + i * 0.45f)).toFloat()
      } else {
        0.2f
      }
      val minHeight = 6.dp.toPx()
      val maxHeight = size.height * 0.85f
      val barHeight = minHeight + (maxHeight - minHeight) * waveFactor

      drawRoundRect(
        color = color.copy(alpha = 0.5f + 0.5f * waveFactor),
        topLeft = Offset(x, centerY - barHeight / 2f),
        size = Size(barWidth, barHeight),
        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
      )
    }
  }
}

/**
 * Realistic Simulated Camera View Canvas.
 * Renders an animated asphalt roadway perspective with moving lane markings,
 * distant horizon gradient, simulated ahead / rear vehicle silhouettes,
 * and toggleable AI object-detection bounding boxes!
 */
@Composable
fun SimulatedRoadScene(
  source: CameraSource,
  showDetectionBoxes: Boolean = true,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "roadMovement")
  val roadOffset by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "roadOffset"
  )

  val isRear = source == CameraSource.REAR

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color(0xFF070E1A))
      .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      val w = size.width
      val h = size.height
      val horizonY = h * 0.42f

      // Sky / Horizon gradient
      drawRect(
        brush = Brush.verticalGradient(
          colors = if (isRear) {
            listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0D1B2A))
          } else {
            listOf(Color(0xFF091322), Color(0xFF10253E), Color(0xFF0A192F))
          },
          startY = 0f,
          endY = horizonY
        ),
        topLeft = Offset.Zero,
        size = Size(w, horizonY)
      )

      // Road Surface
      val roadPath = Path().apply {
        moveTo(w * 0.46f, horizonY)
        lineTo(w * 0.54f, horizonY)
        lineTo(w * 0.95f, h)
        lineTo(w * 0.05f, h)
        close()
      }
      drawPath(
        path = roadPath,
        color = Color(0xFF121820)
      )

      // Road side verge lines (Cyan/Teal tint for futuristic smart helmet HUD)
      drawLine(
        color = if (isRear) Color(0xFF475569) else Color(0x8800E5BC),
        start = Offset(w * 0.46f, horizonY),
        end = Offset(w * 0.05f, h),
        strokeWidth = 2.5.dp.toPx()
      )
      drawLine(
        color = if (isRear) Color(0xFF475569) else Color(0x8800E5BC),
        start = Offset(w * 0.54f, horizonY),
        end = Offset(w * 0.95f, h),
        strokeWidth = 2.5.dp.toPx()
      )

      // Moving Dashed Center Line
      val centerCount = 5
      for (i in 0 until centerCount) {
        val t = ((i.toFloat() / centerCount) + roadOffset * (1f / centerCount)) % 1f
        val lineY1 = horizonY + (h - horizonY) * (t * t)
        val lineY2 = horizonY + (h - horizonY) * ((t + 0.08f) * (t + 0.08f)).coerceAtMost(1f)
        val lineWidth = 2.dp.toPx() + 6.dp.toPx() * t

        if (lineY1 in horizonY..h) {
          drawLine(
            color = Color(0xCCF8FAFC),
            start = Offset(w * 0.5f, lineY1),
            end = Offset(w * 0.5f, lineY2),
            strokeWidth = lineWidth
          )
        }
      }

      // Simulated Vehicle Silhouette
      if (!isRear) {
        // Front View: Vehicle ahead at 18m
        val carCenterX = w * 0.52f
        val carCenterY = horizonY + (h - horizonY) * 0.35f
        val carW = w * 0.28f
        val carH = h * 0.18f

        // Car body
        drawRoundRect(
          color = Color(0xFF1E293B),
          topLeft = Offset(carCenterX - carW / 2, carCenterY - carH / 2),
          size = Size(carW, carH),
          cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )
        // Red taillights
        drawCircle(
          color = Color(0xFFFF2222),
          radius = 4.dp.toPx(),
          center = Offset(carCenterX - carW * 0.35f, carCenterY + carH * 0.2f)
        )
        drawCircle(
          color = Color(0xFFFF2222),
          radius = 4.dp.toPx(),
          center = Offset(carCenterX + carW * 0.35f, carCenterY + carH * 0.2f)
        )
      } else {
        // Rear View: Trailing vehicle approaching in right mirror
        val carCenterX = w * 0.65f
        val carCenterY = horizonY + (h - horizonY) * 0.45f
        val carW = w * 0.34f
        val carH = h * 0.22f

        drawRoundRect(
          color = Color(0xFF233045),
          topLeft = Offset(carCenterX - carW / 2, carCenterY - carH / 2),
          size = Size(carW, carH),
          cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
        )
        // Headlights glowing
        drawCircle(
          color = Color(0xFFFFF7B2),
          radius = 5.dp.toPx(),
          center = Offset(carCenterX - carW * 0.32f, carCenterY + carH * 0.15f)
        )
        drawCircle(
          color = Color(0xFFFFF7B2),
          radius = 5.dp.toPx(),
          center = Offset(carCenterX + carW * 0.32f, carCenterY + carH * 0.15f)
        )
      }

      // Detection Bounding Boxes
      if (showDetectionBoxes) {
        if (!isRear) {
          // Box around car ahead
          val carCenterX = w * 0.52f
          val carCenterY = horizonY + (h - horizonY) * 0.35f
          val boxW = w * 0.32f
          val boxH = h * 0.22f
          drawRoundRect(
            color = TealPrimary,
            topLeft = Offset(carCenterX - boxW / 2, carCenterY - boxH / 2),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 1.5.dp.toPx())
          )
        } else {
          // Warning box around closing rear car
          val carCenterX = w * 0.65f
          val carCenterY = horizonY + (h - horizonY) * 0.45f
          val boxW = w * 0.38f
          val boxH = h * 0.26f
          drawRoundRect(
            color = WarningOrange,
            topLeft = Offset(carCenterX - boxW / 2, carCenterY - boxH / 2),
            size = Size(boxW, boxH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
          )
        }
      }
    }

    // Overlay Detection Labels
    if (showDetectionBoxes) {
      if (!isRear) {
        Box(
          modifier = Modifier
            .padding(top = 70.dp, start = 80.dp)
            .background(Color(0xCC002B22), RoundedCornerShape(4.dp))
            .border(1.dp, TealPrimary, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text("Car • 18m • 52 km/h", color = TealAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      } else {
        Box(
          modifier = Modifier
            .padding(top = 90.dp, end = 24.dp)
            .align(Alignment.TopEnd)
            .background(Color(0xCC3E1800), RoundedCornerShape(4.dp))
            .border(1.dp, WarningOrange, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text("Sedan • 22m • +18 km/h APPROACHING", color = WarningOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // Top HUD Info Bar inside camera view
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(Color(0xCC070B11), Color.Transparent)
          )
        )
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        // Blinking Red REC Dot
        val recTransition = rememberInfiniteTransition(label = "recBlink")
        val recAlpha by recTransition.animateFloat(
          initialValue = 0.2f,
          targetValue = 1f,
          animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
          ),
          label = "recAlpha"
        )
        Box(
          modifier = Modifier
            .size(8.dp)
            .background(DangerRed.copy(alpha = recAlpha), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isRear) "LIVE • REAR 1080p60" else "LIVE • FRONT 1080p60",
          color = TextPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Text(
        text = "2026-09-06 08:16:32",
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
