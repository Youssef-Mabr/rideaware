package com.example

import android.os.Bundle
import android.content.Context
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
import com.example.data.RideRepository
import com.example.ui.RideHistoryViewModel
import com.example.ui.RideHistoryState
import com.example.ui.EmergencyContactsViewModel
import com.example.ui.SosViewModel
import com.example.ui.HelmetPairingViewModel
import com.example.ui.GeminiAssistantViewModel
import com.example.ui.GeminiLiveVoiceViewModel
import com.example.safety.SafetyAlertManager
import com.example.ui.rememberLocationPermission
import com.example.ui.components.RideHistoryStatus
import com.example.ui.components.RideAwareTopBar
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppDestination
import com.example.model.EmergencyContact
import com.example.model.ErrorDemoType
import com.example.model.HelmetStatus
import com.example.model.ProtectedClip
import com.example.model.RideSummary
import com.example.model.SafetyAlert
import com.example.sos.SosTriggerSource
import com.example.ui.components.AppBottomBar
import com.example.ui.components.DeviceFrameContainer
import com.example.ui.screens.ActiveRideScreen
import com.example.ui.screens.CrashDetectionSosScreen
import com.example.ui.screens.FeatureIntroScreen
import com.example.ui.screens.GenieAssistantScreen
import com.example.ui.screens.HelmetManagementScreen
import com.example.ui.screens.HelmetPairingScreen
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
import com.example.recording.BundledTestVideoSource
import com.example.recording.RecordingSession
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
    enableEdgeToEdge(
      statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
      navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
    )
    setContent {
      MyApplicationTheme {
        GeniApp()
      }
    }
  }
}

@Composable
fun GeniApp() {
  val historyViewModel: RideHistoryViewModel = viewModel()
  val historyState by historyViewModel.state.collectAsStateWithLifecycle()
  val historyRides = (historyState as? RideHistoryState.Ready)?.rides.orEmpty()
  val contactsViewModel: EmergencyContactsViewModel = viewModel()
  val contactsState by contactsViewModel.state.collectAsStateWithLifecycle()
  val sosViewModel: SosViewModel = viewModel()
  val sosState by sosViewModel.state.collectAsStateWithLifecycle()
  val helmetPairingViewModel: HelmetPairingViewModel = viewModel()
  val helmetPairingState by helmetPairingViewModel.state.collectAsStateWithLifecycle()
  val geminiAssistantViewModel: GeminiAssistantViewModel = viewModel()
  val geminiAssistantState by geminiAssistantViewModel.state.collectAsStateWithLifecycle()
  val geminiLiveVoiceViewModel: GeminiLiveVoiceViewModel = viewModel()
  val geminiLiveVoiceState by geminiLiveVoiceViewModel.state.collectAsStateWithLifecycle()
  val locationPermission = rememberLocationPermission()
  val emergencyContact = contactsState.contacts.firstOrNull() ?: EmergencyContact(
    name = "", relationship = "", phoneNumber = "", phone = ""
  )
  val context = LocalContext.current
  val preferences = remember { context.getSharedPreferences("rideaware_preferences", Context.MODE_PRIVATE) }
  val screenState = rememberSaveableStateHolder()
  var currentDestination by rememberSaveable {
    mutableStateOf(if (preferences.getBoolean("demo_explored", false)) AppDestination.HOME else AppDestination.WELCOME)
  }
  var auxiliaryReturn by rememberSaveable { mutableStateOf(AppDestination.HOME) }
  var confirmEndRide by rememberSaveable { mutableStateOf(false) }
  var rideStartTimeMillis by rememberSaveable { mutableStateOf(0L) }
  var rideDurationSeconds by rememberSaveable { mutableStateOf(0) }
  var rideDistanceKm by rememberSaveable { mutableStateOf(0f) }
  var rideMaxSpeedKmH by rememberSaveable { mutableStateOf(58) }
  var rideSafetyEventCount by rememberSaveable { mutableStateOf(0) }
  var rideSaveRequested by rememberSaveable { mutableStateOf(false) }
  val recordingSource = remember(context) { BundledTestVideoSource(context) }
  var activeRecordingSession by remember { mutableStateOf<RecordingSession?>(null) }
  var speedUnitKmH by remember { mutableStateOf(preferences.getBoolean("metric_units", true)) }
  fun finishSetup() {
    preferences.edit().putBoolean("demo_explored", true).apply()
    currentDestination = AppDestination.HOME
  }
  var helmetStatus by remember { mutableStateOf(MockDataProvider.defaultHelmetStatus) }
  var currentError by remember { mutableStateOf(ErrorDemoType.NONE) }
  var selectedRideId by rememberSaveable { mutableStateOf<String?>(null) }
  val selectedRide = historyRides.firstOrNull { it.id == selectedRideId }
  var selectedClipId by rememberSaveable { mutableStateOf(MockDataProvider.sampleProtectedClips.first().id) }
  val selectedClip = (selectedRide?.clips.orEmpty() + selectedRide?.protectedClips.orEmpty()).firstOrNull { it.id == selectedClipId }
  var activeSafetyAlert by remember { mutableStateOf<SafetyAlert?>(null) }
  val safetyAlertManager = remember { SafetyAlertManager() }
  var momentSavedToast by remember { mutableStateOf<String?>(null) }

  fun openSimulatedHelmetSos() {
    // Asking never blocks the countdown; denial still produces an SOS event.
    locationPermission.requestIfNeeded()
    rideSafetyEventCount++
    sosViewModel.trigger(SosTriggerSource.SIMULATED_HELMET_BUTTON)
    screenState.removeState(AppDestination.CRASH_DETECTION_SOS.name)
    currentDestination = AppDestination.CRASH_DETECTION_SOS
  }

  LaunchedEffect(currentDestination) {
    if (currentDestination == AppDestination.RIDES) historyViewModel.refresh()
  }

  // Back button handling
  BackHandler(enabled = currentDestination != AppDestination.HOME && currentDestination != AppDestination.WELCOME) {
    if (activeSafetyAlert != null) {
      activeSafetyAlert = null
    } else when (currentDestination) {
      AppDestination.FEATURE_INTRO -> currentDestination = AppDestination.WELCOME
      AppDestination.PERMISSIONS -> currentDestination = AppDestination.FEATURE_INTRO
      AppDestination.PAIR_HELMET -> currentDestination = AppDestination.PERMISSIONS
      AppDestination.HELMET_SETUP -> currentDestination = AppDestination.PAIR_HELMET
      AppDestination.HELMET_PAIRING -> currentDestination = AppDestination.SETTINGS
      AppDestination.PRE_RIDE_CHECK -> currentDestination = AppDestination.HOME
      AppDestination.ACTIVE_RIDE -> confirmEndRide = true
      AppDestination.LIVE_CAMERAS -> currentDestination = auxiliaryReturn
      AppDestination.RIDE_SUMMARY -> currentDestination = AppDestination.HOME
      AppDestination.RIDE_DETAILS -> currentDestination = AppDestination.RIDES
      AppDestination.VIDEO_PLAYBACK -> currentDestination = AppDestination.RIDE_DETAILS
      AppDestination.CRASH_DETECTION_SOS -> {
        sosViewModel.cancel()
        sosViewModel.reset()
        currentDestination = AppDestination.ACTIVE_RIDE
      }
      AppDestination.GENIE -> currentDestination = auxiliaryReturn
      AppDestination.RIDES, AppDestination.HELMET, AppDestination.SETTINGS -> {
        currentDestination = AppDestination.HOME
      }
      else -> {}
    }
  }

  // Auto-dismiss save moment toast after 2.5s
  LaunchedEffect(momentSavedToast) {
    if (momentSavedToast != null) {
      delay(6000)
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
    momentSavedToast = "No video was recorded or saved."
  }

  if (confirmEndRide) {
    AlertDialog(
      onDismissRequest = { confirmEndRide = false },
      title = { Text("End this ride?") },
      text = { Text("The ride timer will stop and the ride summary will open.") },
      confirmButton = {
        TextButton(onClick = {
          if (!rideSaveRequested && rideStartTimeMillis > 0L) {
            rideSaveRequested = true
            val clip = activeRecordingSession?.stop()
            activeRecordingSession = null
            RideRepository.saveCompletedRide(
              startTimeMillis = rideStartTimeMillis,
              endTimeMillis = System.currentTimeMillis(),
              durationSeconds = rideDurationSeconds,
              distanceKm = rideDistanceKm,
              maxSpeedKmH = rideMaxSpeedKmH,
              safetyEventCount = rideSafetyEventCount,
              clip = clip
            )
          }
          confirmEndRide = false
          auxiliaryReturn = AppDestination.HOME
          currentDestination = AppDestination.RIDE_SUMMARY
        }) { Text("End ride") }
      },
      dismissButton = { TextButton(onClick = { confirmEndRide = false }) { Text("Keep riding") } }
    )
  }

  DeviceFrameContainer {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = DarkCanvas,
      bottomBar = {
        if (showBottomNav && !(currentDestination == AppDestination.GENIE && auxiliaryReturn == AppDestination.ACTIVE_RIDE)) {
          AppBottomBar(
            currentDestination = currentDestination,
            onNavigate = { dest -> auxiliaryReturn = AppDestination.HOME; currentDestination = dest }
          )
        }
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
      ) {
        screenState.SaveableStateProvider(currentDestination.name) {
        when (currentDestination) {
          AppDestination.WELCOME -> {
            WelcomeScreen(
              onGetStarted = { currentDestination = AppDestination.FEATURE_INTRO },
              onAlreadyPaired = { finishSetup() }
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
              pairingData = helmetPairingState,
              onSubmitQr = helmetPairingViewModel::submitQr,
              onPairingComplete = {
                helmetStatus = helmetStatus.copy(isConnected = true)
                currentDestination = AppDestination.HELMET_SETUP
              },
              onBack = { currentDestination = AppDestination.PERMISSIONS }
            )
          }

          AppDestination.HELMET_SETUP -> {
            HelmetSetupScreen(
              onFinishSetup = { finishSetup() },
              onBack = { currentDestination = AppDestination.PAIR_HELMET }
            )
          }

          AppDestination.HOME -> {
            HomeScreen(
              helmetStatus = helmetStatus,
              currentError = currentError,
              onSelectError = { currentError = it },
              onStartRide = { currentDestination = AppDestination.PRE_RIDE_CHECK },
              onOpenLiveCameras = { auxiliaryReturn = AppDestination.HOME; currentDestination = AppDestination.LIVE_CAMERAS },
              onOpenGenie = { auxiliaryReturn = currentDestination; currentDestination = AppDestination.GENIE }
            )
          }

          AppDestination.PRE_RIDE_CHECK, AppDestination.PRE_RIDE -> {
            PreRideScreen(
              helmetStatus = helmetStatus,
              emergencyContact = emergencyContact,
              onStartActiveRide = {
                rideStartTimeMillis = System.currentTimeMillis()
                rideDurationSeconds = 0
                rideDistanceKm = 0f
                rideMaxSpeedKmH = 58
                rideSafetyEventCount = 0
                rideSaveRequested = false
                activeRecordingSession = recordingSource.start()
                screenState.removeState(AppDestination.ACTIVE_RIDE.name)
                currentDestination = AppDestination.ACTIVE_RIDE
              },
              onBack = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.ACTIVE_RIDE -> {
            ActiveRideScreen(
              helmetStatus = helmetStatus,
              activeAlert = activeSafetyAlert,
              onSaveMoment = { triggerSaveMoment() },
              onEndRide = { confirmEndRide = true },
              speedUnitKmH = speedUnitKmH,
              onRideMetricsChanged = { duration, distance, maxSpeed ->
                rideDurationSeconds = duration
                rideDistanceKm = distance
                rideMaxSpeedKmH = maxSpeed
              },
              onCameraAnalysis = { analysis ->
                safetyAlertManager.onHazards(analysis.hazards).firstOrNull()?.let { alert ->
                  activeSafetyAlert = alert
                  rideSafetyEventCount++
                }
              },
              onOpenLiveCamera = { auxiliaryReturn = AppDestination.ACTIVE_RIDE; currentDestination = AppDestination.LIVE_CAMERAS },
              onOpenGenie = { auxiliaryReturn = currentDestination; currentDestination = AppDestination.GENIE },
              onTriggerHazardAlert = {
                rideSafetyEventCount++
                activeSafetyAlert = MockDataProvider.sampleAlerts.first()
              },
              onTriggerCrashSos = {
                openSimulatedHelmetSos()
              }
            )
          }

          AppDestination.LIVE_CAMERAS, AppDestination.LIVE_CAMERA -> {
            LiveCameraScreen(
              onSaveMoment = { triggerSaveMoment() },
              onBack = { currentDestination = auxiliaryReturn }
            )
          }

          AppDestination.CRASH_DETECTION_SOS -> {
            CrashDetectionSosScreen(
              state = sosState,
              onCancelCountdown = sosViewModel::cancel,
              onReturnToRide = {
                sosViewModel.reset()
                currentDestination = AppDestination.ACTIVE_RIDE
              }
            )
          }

          AppDestination.RIDE_SUMMARY -> {
            RideSummaryScreen(
              rideSummary = MockDataProvider.mockRides.first(),
              onViewClips = { currentDestination = AppDestination.RIDES },
              onBackToHome = { currentDestination = AppDestination.HOME }
            )
          }

          AppDestination.RIDES -> {
            RidesVaultScreen(
              rides = historyRides,
              isLoading = historyState is RideHistoryState.Loading,
              errorMessage = (historyState as? RideHistoryState.Error)?.message,
              onRetry = historyViewModel::refresh,
              onSelectRide = { ride ->
                selectedRideId = ride.id
                currentDestination = AppDestination.RIDE_DETAILS
              }
            )
          }

          AppDestination.RIDE_DETAILS, AppDestination.RIDE_DETAIL -> {
            if (selectedRide != null) RideDetailScreen(
              ride = selectedRide,
              onWatchDualPlayback = { clip ->
                selectedClipId = clip.id
                currentDestination = AppDestination.VIDEO_PLAYBACK
              },
              onBack = { currentDestination = AppDestination.RIDES }
            )
            else Column(Modifier.statusBarsPadding()) {
              RideAwareTopBar("Ride Details", onBack = { currentDestination = AppDestination.RIDES })
              RideHistoryStatus(
                title = if (historyState is RideHistoryState.Loading) "Loading ride" else "Ride unavailable",
                message = (historyState as? RideHistoryState.Error)?.message ?: "Return to your rides or try again.",
                loading = historyState is RideHistoryState.Loading,
                onRetry = if (historyState is RideHistoryState.Loading) null else historyViewModel::refresh
              )
            }
          }

          AppDestination.VIDEO_PLAYBACK -> {
            if (selectedClip != null) VideoPlaybackScreen(
              clip = selectedClip,
              onBack = { currentDestination = AppDestination.RIDE_DETAILS }
            )
            else Column(Modifier.statusBarsPadding()) {
              RideAwareTopBar("Video unavailable", onBack = { currentDestination = AppDestination.RIDE_DETAILS })
              RideHistoryStatus("No saved video", "This ride has no video available for playback.")
            }
          }

          AppDestination.GENIE -> {
            GenieAssistantScreen(
              assistantState = geminiAssistantState,
              onAsk = geminiAssistantViewModel::ask,
              voiceState = geminiLiveVoiceState,
              onVoiceStart = geminiLiveVoiceViewModel::start,
              onVoiceStop = geminiLiveVoiceViewModel::stop,
              onBack = if (auxiliaryReturn == AppDestination.ACTIVE_RIDE) ({ currentDestination = AppDestination.ACTIVE_RIDE }) else null
            )
          }

          AppDestination.HELMET -> {
            HelmetManagementScreen(
              helmetStatus = helmetStatus,
              onUnpairHelmet = {
                helmetStatus = helmetStatus.copy(isConnected = false)
                preferences.edit().putBoolean("demo_explored", false).apply()
                currentDestination = AppDestination.WELCOME
              }
            )
          }

          AppDestination.SETTINGS -> {
            SettingsScreen(
              emergencyContacts = contactsState.contacts,
              contactsLoading = contactsState.loading,
              contactsError = contactsState.loadError,
              contactOperationInProgress = contactsState.operationInProgress,
              contactOperationError = contactsState.operationError,
              onAddContact = contactsViewModel::add,
              onUpdateContact = contactsViewModel::update,
              onDeleteContact = contactsViewModel::delete,
              onRetryContacts = contactsViewModel::retry,
              onClearContactError = contactsViewModel::clearOperationError,
              onOpenHelmetPairing = { currentDestination = AppDestination.HELMET_PAIRING },
              speedUnitKmH = speedUnitKmH,
              onSpeedUnitChange = { speedUnitKmH = it; preferences.edit().putBoolean("metric_units", it).apply() }
            )
          }

          AppDestination.HELMET_PAIRING -> {
            HelmetPairingScreen(
              state = helmetPairingState,
              onSubmitQr = helmetPairingViewModel::submitQr,
              onUnpair = helmetPairingViewModel::unpair,
              onRetry = helmetPairingViewModel::retry,
              onClearOperationError = helmetPairingViewModel::clearOperationError,
              onBack = { currentDestination = AppDestination.SETTINGS }
            )
          }
        }

        } // Save each screen's state when navigating away.

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
              openSimulatedHelmetSos()
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
              .semantics { liveRegion = LiveRegionMode.Polite }
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
