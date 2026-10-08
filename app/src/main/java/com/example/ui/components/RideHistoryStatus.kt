package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RideHistoryStatus(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  loading: Boolean = false,
  onRetry: (() -> Unit)? = null
) {
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp).testTag("ride_history_status"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (loading) CircularProgressIndicator(color = TealPrimary, modifier = Modifier.testTag("ride_history_loading"))
    Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Text(message, color = TextSecondary, fontSize = 14.sp)
    if (onRetry != null) GloveButton("Try again", onRetry, testTag = "ride_history_retry")
  }
}
