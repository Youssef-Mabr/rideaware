package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MockDataProvider
import com.example.model.AppDestination
import com.example.model.EmergencyContact
import com.example.model.ErrorDemoType
import com.example.model.HelmetStatus
import com.example.model.ProtectedClip
import com.example.model.RideSummary
import com.example.model.SafetyAlert
import com.example.ui.components.AppBottomBar
import com.example.ui.components.DeviceFrameContainer
import com.example.ui.screens.ActiveRideScreen
import com.example.ui.screens.CrashDetectionSosScreen
import com.example.ui.screens.FeatureIntroScreen
import com.example.ui.screens.GenieAssistantScreen
import com.example.ui.screens.HelmetManagementScreen
import com.example.ui.screens.HelmetSetupScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LiveCameraScreen
import com.example.ui.screens.PairingScreen
import com.example.ui.screens.PermissionScreen
import com.example.ui.screens.PreRideScreen
import com.example.ui.screens.RideDetailScreen
import com.example.ui.screens.RideSummaryScreen
import com.example.ui.screens.RidesVaultScreen
import com.example.ui.screens.SafetyAlertOverlay
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideoPlaybackScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        GeniApp()
      }
    }
  }
}

@Composable
fun GeniApp() {
  var currentDestination by remember { mutableStateOf(AppDestination.WELCOME) }
  var helmetStatus by remember { mutableStateOf(MockDataProvider.defaultHelmetStatus) }
  var emergencyContact by remember { mutableStateOf(MockDataProvider.defaultEmergencyContact) }
  var currentError by remember { mutableStateOf(ErrorDemoType.NONE) }
  var selectedRide by remember { mutableStateOf(MockDataProvider.mockRides.first()) }
  var selectedClip by remember { mutableStateOf(MockDataProvider.sampleProtectedClips.first()) }
  var activeSafetyAlert by remember { mutableStateOf<SafetyAlert?>(null) }
  var momentSavedToast by remember { mutableStateOf<String?>(null) }

  // Back button handling
  BackHandler(enabled = currentDestination != AppDestination.HOME && currentDestination != AppDestination.WELCOME) {
    when (currentDestination) {
      AppDestination.FEATURE_INTRO -> currentDestination = AppDestination.WELCOME
      AppDestination.PERMISSIONS -> currentDestination = AppDestination.FEATURE_INTRO
      AppDestination.PAIR_HELMET -> currentDestination = AppDestination.PERMISSIONS
      AppDestination.HELMET_SETUP -> currentDestination = AppDestination.PAIR_HELMET
      AppDestination.PRE_RIDE_CHECK -> currentDestination = AppDestination.HOME
      AppDestination.ACTIVE_RIDE -> currentDestination = AppDestination.HOME
      AppDestination.LIVE_CAMERAS -> currentDestination = AppDestination.HOME
      AppDestination.RIDE_SUMMARY -> currentDestination = AppDestination.HOME
      AppDestination.RIDE_DETAILS -> currentDestination = AppDestination.RIDES
      AppDestination.VIDEO_PLAYBACK -> currentDestination = AppDestination.RIDE_DETAILS
      AppDestination.CRASH_DETECTION_SOS -> currentDestination = AppDestination.ACTIVE_RIDE
      AppDestination.RIDES, AppDestination.GENIE, AppDestination.HELMET, AppDestination.SETTINGS -> {
        currentDestination = AppDestination.HOME
      }
      else -> {}
    }
  }

  // Auto-dismiss save moment toast after 2.5s
  LaunchedEffect(momentSavedToast) {
    if (momentSavedToast != null) {
      delay(2500)
      momentSavedToast = null
    }
  }

  // Show bottom nav on main dashboard tabs
  val showBottomNav = currentDestination in listOf(
    AppDestination.HOME,
    AppDestination.RIDES,
    AppDestination.GENIE,
    AppDestination.HELMET,
    AppDestination.SETTINGS
  )

  fun triggerSaveMoment() {
    momentSavedToast = "Moment Saved: 30s dual-angle video encrypted to helmet vault."
  }

  DeviceFrameContainer {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = DarkCanvas,
      bottomBar = {
        if (showBottomNav) {
          AppBottomBar(
            currentDestination = currentDestination,
            onNavigate = { dest -> currentDestination = dest }
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (currentDestination) {
          AppDestination.WELCOME -> {
            WelcomeScreen(
              onGetStarted = { currentDestination = AppDestination.FEATURE_INTRO },
              onAlreadyPaired = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.FEATURE_INTRO, AppDestination.INTRO -> {
            FeatureIntroScreen(
              onContinue = { currentDestination = AppDestination.PERMISSIONS },
              onBack = { currentDestination = AppDestination.WELCOME }
            )
          }

          AppDestination.PERMISSIONS -> {
            PermissionScreen(
              onPermissionsAllowed = { currentDestination = AppDestination.PAIR_HELMET },
              onContinueLimited = { currentDestination = AppDestination.PAIR_HELMET },
              onBack = { currentDestination = AppDestination.FEATURE_INTRO }
            )
          }

          AppDestination.PAIR_HELMET, AppDestination.PAIRING -> {
            PairingScreen(
              onPairingComplete = {
                helmetStatus = helmetStatus.copy(isConnected = true)
                currentDestination = AppDestination.HELMET_SETUP
              },
              onBack = { currentDestination = AppDestination.PERMISSIONS }
            )
          }

          AppDestination.HELMET_SETUP -> {
            HelmetSetupScreen(
              onFinishSetup = { currentDestination = AppDestination.HOME },
              onBack = { currentDestination = AppDestination.PAIR_HELMET }
            )
          }

          AppDestination.HOME -> {
            HomeScreen(
              helmetStatus = helmetStatus,
              currentError = currentError,
              onSelectError = { currentError = it },
              onStartRide = { currentDestination = AppDestination.PRE_RIDE_CHECK },
              onOpenLiveCameras = { currentDestination = AppDestination.LIVE_CAMERAS },
              onOpenGenie = { currentDestination = AppDestination.GENIE }
            )
          }

          AppDestination.PRE_RIDE_CHECK, AppDestination.PRE_RIDE -> {
            PreRideScreen(
              helmetStatus = helmetStatus,
              emergencyContact = emergencyContact,
              onStartActiveRide = { currentDestination = AppDestination.ACTIVE_RIDE },
              onBack = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.ACTIVE_RIDE -> {
            ActiveRideScreen(
              helmetStatus = helmetStatus,
              activeAlert = activeSafetyAlert,
              onSaveMoment = { triggerSaveMoment() },
              onEndRide = { currentDestination = AppDestination.RIDE_SUMMARY },
              onOpenLiveCamera = { currentDestination = AppDestination.LIVE_CAMERAS },
              onOpenGenie = { currentDestination = AppDestination.GENIE },
              onTriggerHazardAlert = {
                activeSafetyAlert = MockDataProvider.sampleAlerts.first()
              },
              onTriggerCrashSos = { currentDestination = AppDestination.CRASH_DETECTION_SOS }
            )
          }

          AppDestination.LIVE_CAMERAS, AppDestination.LIVE_CAMERA -> {
            LiveCameraScreen(
              onSaveMoment = { triggerSaveMoment() },
              onBack = {
                currentDestination = AppDestination.HOME
              }
            )
          }

          AppDestination.CRASH_DETECTION_SOS -> {
            CrashDetectionSosScreen(
              emergencyContact = emergencyContact,
              onCancel = { currentDestination = AppDestination.ACTIVE_RIDE }
            )
          }

          AppDestination.RIDE_SUMMARY -> {
            RideSummaryScreen(
              rideSummary = selectedRide,
              onViewClips = { currentDestination = AppDestination.RIDES },
              onBackToHome = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.RIDES -> {
            RidesVaultScreen(
              rides = MockDataProvider.mockRides,
              onSelectRide = { ride ->
                selectedRide = ride
                currentDestination = AppDestination.RIDE_DETAILS
              }
            )
          }

          AppDestination.RIDE_DETAILS, AppDestination.RIDE_DETAIL -> {
            RideDetailScreen(
              ride = selectedRide,
              onWatchDualPlayback = { clip ->
                selectedClip = clip
                currentDestination = AppDestination.VIDEO_PLAYBACK
              },
              onBack = { currentDestination = AppDestination.RIDES }
            )
          }

          AppDestination.VIDEO_PLAYBACK -> {
            VideoPlaybackScreen(
              clip = selectedClip,
              onBack = { currentDestination = AppDestination.RIDE_DETAILS }
            )
          }

          AppDestination.GENIE -> {
            GenieAssistantScreen()
          }

          AppDestination.HELMET -> {
            HelmetManagementScreen(
              helmetStatus = helmetStatus,
              onUnpairHelmet = {
                helmetStatus = helmetStatus.copy(isConnected = false)
                currentDestination = AppDestination.WELCOME
              }
            )
          }

          AppDestination.SETTINGS -> {
            SettingsScreen(
              emergencyContact = emergencyContact,
              onUpdateContact = { emergencyContact = it }
            )
          }
        }

        // Active Animated Safety Hazard Overlay (when triggered in Active Ride)
        activeSafetyAlert?.let { alert ->
          SafetyAlertOverlay(
            alert = alert,
            onSaveEvent = {
              triggerSaveMoment()
              activeSafetyAlert = null
            },
            onDismiss = { activeSafetyAlert = null },
            onSos = {
              activeSafetyAlert = null
              currentDestination = AppDestination.CRASH_DETECTION_SOS
            }
          )
        }

        // Floating "Moment Saved" Toast Notification
        AnimatedVisibility(
          visible = momentSavedToast != null,
          enter = fadeIn(),
          exit = fadeOut(),
          modifier = Modifier
            .padding(top = 40.dp, start = 20.dp, end = 20.dp)
            .align(Alignment.TopCenter)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated)
              .border(1.5.dp, TealPrimary, RoundedCornerShape(16.dp))
              .padding(horizontal = 16.dp, vertical = 12.dp)
              .testTag("moment_saved_toast")
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(28.dp)
                  .background(TealPrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Filled.Bookmark,
                  contentDescription = null,
                  tint = TealPrimary,
                  modifier = Modifier.size(16.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = momentSavedToast ?: "",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
