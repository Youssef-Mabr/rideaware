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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.model.RideSummary
import com.example.ui.components.RideAwareTopBar
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
import com.example.ui.theme.WarningOrange

enum class VaultFilterTab {
  ALL,
  PROTECTED,
  FAVORITES
}

@Composable
fun RidesVaultScreen(
  rides: List<RideSummary>,
  onSelectRide: (RideSummary) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(VaultFilterTab.ALL) }

  val filteredRides = remember(selectedTab, rides) {
    when (selectedTab) {
      VaultFilterTab.ALL -> rides
      VaultFilterTab.PROTECTED -> rides.filter { it.safetyEventCount > 0 }
      VaultFilterTab.FAVORITES -> rides.take(1)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkCanvas)
      .statusBarsPadding()
  ) {
    RideAwareTopBar(
      title = "Rides & Video Vault",
      subtitle = "Encrypted dual camera incident telemetry"
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
      // Filter Tabs (All, Protected, Favorites)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurface)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        VaultFilterTab.values().forEach { tab ->
          val isSelected = selectedTab == tab
          val title = when (tab) {
            VaultFilterTab.ALL -> "All Rides"
            VaultFilterTab.PROTECTED -> "Protected Clips"
            VaultFilterTab.FAVORITES -> "Favorites"
          }

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) TealPrimary else Color.Transparent)
              .clickable { selectedTab = tab }
              .padding(vertical = 10.dp)
              .testTag("vault_tab_${tab.name.lowercase()}"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = title,
              color = if (isSelected) Color(0xFF041912) else TextSecondary,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // List of Ride Cards
    LazyColumn(
      modifier = Modifier
        .weight(1f)
        .padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(filteredRides) { ride ->
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectRide(ride) }
            .testTag("ride_card_${ride.id}"),
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = DarkSurface),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
              listOf(
                if (ride.safetyEventCount > 0) WarningOrange.copy(alpha = 0.3f) else DarkSurfaceBorder,
                DarkSurfaceBorder
              )
            )
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp)
          ) {
            // Header: Title, Date, Chevron
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text(
                  text = ride.title,
                  color = TextPrimary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "${ride.date} • ${ride.startTime} - ${ride.endTime}",
                  color = TextSecondary,
                  fontSize = 12.sp
                )
              }

              Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View details",
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Summary Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Duration & Distance
              Column {
                Text("DISTANCE & TIME", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("${ride.distanceKm} km • ${ride.durationMinutes}m", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }

              // Safety Score
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SAFETY", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("${ride.safetyScore}", color = TealPrimary, fontSize = 14.sp, fontWeight = FontWeight.Black)
                  Text("/100", color = TextMuted, fontSize = 10.sp)
                }
              }

              // Protected Events Badge
              if (ride.safetyEventCount > 0) {
                Row(
                  modifier = Modifier
                    .background(WarningOrange.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                    .border(1.dp, WarningOrange.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(Icons.Filled.Lock, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(12.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "${ride.safetyEventCount} Events",
                    color = WarningOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              } else {
                Row(
                  modifier = Modifier
                    .background(DarkSurfaceElevated, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text("0 Incidents", color = TextMuted, fontSize = 11.sp)
                }
              }
            }
          }
        }
      }

      item {
        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }
}
