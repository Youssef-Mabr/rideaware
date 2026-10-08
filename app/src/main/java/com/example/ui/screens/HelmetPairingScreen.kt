package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.helmet.HelmetQrCatalog
import com.example.ui.HelmetPairingState
import com.example.ui.QrCameraScanner
import com.example.ui.components.GloveButton
import com.example.ui.components.GloveOutlinedButton
import com.example.ui.components.RideAwareTopBar
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TealAccent
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun HelmetPairingScreen(
  state: HelmetPairingState,
  onSubmitQr: (String) -> Unit,
  onUnpair: () -> Unit,
  onRetry: () -> Unit,
  onClearOperationError: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showPayloadDialog by rememberSaveable { mutableStateOf(false) }
  var showUnpairDialog by rememberSaveable { mutableStateOf(false) }
  var showCameraScanner by rememberSaveable { mutableStateOf(false) }
  var qrPayload by rememberSaveable { mutableStateOf("") }

  if (showCameraScanner) {
    QrCameraScanner(
      onPayload = { payload ->
        showCameraScanner = false
        onSubmitQr(payload)
      },
      onClose = { showCameraScanner = false }
    )
    return
  }

  Column(modifier.fillMaxSize().background(DarkCanvas)) {
    RideAwareTopBar(
      title = "Helmet pairing",
      subtitle = "Pair a RideAware helmet QR",
      onBack = onBack
    )
    Column(
      Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      when {
        state.loading -> LoadingState()
        state.unauthenticated -> MessageCard(
          title = "Sign-in required",
          message = "Your account is still starting. Return here in a moment to pair a helmet.",
          error = true,
          tag = "helmet_pairing_unauthenticated"
        )
        state.loadError != null -> {
          MessageCard("Couldn’t load helmet pairing", state.loadError, true, "helmet_pairing_load_error")
          GloveOutlinedButton("Try again", onRetry, testTag = "retry_helmet_pairing_button")
        }
        state.pairedHelmet != null -> PairedHelmetCard(
          helmetId = state.pairedHelmet.helmetId,
          pairedAt = state.pairedHelmet.pairedAt?.let { "Paired to your account" } ?: "Finishing account sync…",
          busy = state.operationInProgress,
          onUnpair = { showUnpairDialog = true }
        )
        else -> UnpairedHelmetContent(
          busy = state.operationInProgress,
          onScanCamera = { showCameraScanner = true },
          onSimulateScan = { onSubmitQr(HelmetQrCatalog.SAMPLE_HELMET_ID) },
          onEnterPayload = {
            onClearOperationError()
            showPayloadDialog = true
          }
        )
      }

      state.operationError?.let { error ->
        MessageCard("Pairing needs attention", error, true, "helmet_pairing_operation_error")
      }

      Text(
        "Pairing saves only the helmet ID to your account. It does not use Bluetooth, Wi-Fi, or communicate with a helmet.",
        color = TextSecondary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 20.dp)
      )
    }
  }

  if (showPayloadDialog) {
    AlertDialog(
      onDismissRequest = { if (!state.operationInProgress) showPayloadDialog = false },
      containerColor = DarkSurface,
      title = { Text("Simulate QR scan", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Enter the QR content. The only provisioned sample helmet is ${HelmetQrCatalog.SAMPLE_HELMET_ID}.", color = TextSecondary, fontSize = 13.sp)
          OutlinedTextField(
            value = qrPayload,
            onValueChange = { if (it.length <= 80) qrPayload = it },
            label = { Text("QR content") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("helmet_qr_payload_input")
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = { onSubmitQr(qrPayload); showPayloadDialog = false },
          enabled = !state.operationInProgress,
          modifier = Modifier.testTag("confirm_helmet_qr_payload")
        ) { Text("Check QR") }
      },
      dismissButton = { TextButton(onClick = { showPayloadDialog = false }) { Text("Cancel") } }
    )
  }

  if (showUnpairDialog) {
    AlertDialog(
      onDismissRequest = { if (!state.operationInProgress) showUnpairDialog = false },
      containerColor = DarkSurface,
      title = { Text("Unpair helmet?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("This removes the helmet ID from your account. No hardware connection is affected.", color = TextSecondary) },
      confirmButton = {
        TextButton(
          enabled = !state.operationInProgress,
          onClick = { onUnpair(); showUnpairDialog = false },
          modifier = Modifier.testTag("confirm_unpair_paired_helmet")
        ) { Text("Unpair", color = DangerRed) }
      },
      dismissButton = { TextButton(onClick = { showUnpairDialog = false }) { Text("Cancel") } }
    )
  }
}

@Composable
private fun LoadingState() {
  Column(
    Modifier.fillMaxWidth().padding(vertical = 72.dp).testTag("helmet_pairing_loading"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    CircularProgressIndicator(color = TealPrimary)
    Text("Loading helmet pairing…", color = TextSecondary)
  }
}

@Composable
private fun UnpairedHelmetContent(
  busy: Boolean,
  onScanCamera: () -> Unit,
  onSimulateScan: () -> Unit,
  onEnterPayload: () -> Unit
) {
  Text("No helmet paired", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.testTag("helmet_unpaired"))
  Text("Use the provisioned sample QR below to test pairing before physical helmets are available.", color = TextSecondary, textAlign = TextAlign.Center, fontSize = 14.sp)
  SampleHelmetQr(HelmetQrCatalog.SAMPLE_HELMET_ID)
  Text(HelmetQrCatalog.SAMPLE_HELMET_ID, color = TealAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
  Text("This QR is valid. Any other code shows a clear invalid-QR message.", color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
  GloveButton(
    text = if (busy) "Opening scanner…" else "Scan with camera",
    onClick = { if (!busy) onScanCamera() },
    enabled = !busy,
    icon = Icons.Filled.QrCodeScanner,
    testTag = "scan_helmet_qr_camera"
  )
  GloveOutlinedButton(
    text = if (busy) "Pairing helmet…" else "Simulate scan sample QR",
    onClick = onSimulateScan,
    testTag = "simulate_sample_helmet_qr"
  )
  GloveOutlinedButton(
    text = if (busy) "Pairing in progress…" else "Enter QR content",
    onClick = { if (!busy) onEnterPayload() },
    testTag = "enter_helmet_qr_content"
  )
}

@Composable
private fun PairedHelmetCard(helmetId: String, pairedAt: String, busy: Boolean, onUnpair: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(DarkSurface).border(1.dp, SuccessGreen.copy(alpha = 0.55f), RoundedCornerShape(20.dp)).padding(22.dp)
      .testTag("helmet_paired"),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Box(Modifier.size(60.dp).background(SuccessGreen.copy(alpha = 0.15f), CircleShape), contentAlignment = Alignment.Center) {
      androidx.compose.material3.Icon(Icons.Filled.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(38.dp))
    }
    Text("Helmet Paired", color = SuccessGreen, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(helmetId, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    Text(pairedAt, color = TextSecondary, fontSize = 13.sp)
    Spacer(Modifier.height(4.dp))
    GloveOutlinedButton(
      text = if (busy) "Updating pairing…" else "Unpair helmet",
      onClick = { if (!busy) onUnpair() },
      testTag = "unpair_paired_helmet"
    )
  }
}

@Composable
private fun MessageCard(title: String, message: String, error: Boolean, tag: String) {
  Column(
    Modifier.fillMaxWidth().background(if (error) DangerRed.copy(alpha = 0.12f) else DarkSurface, RoundedCornerShape(16.dp))
      .border(1.dp, if (error) DangerRed.copy(alpha = 0.6f) else DarkSurfaceBorder, RoundedCornerShape(16.dp)).padding(16.dp).testTag(tag),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    Text(title, color = if (error) DangerRed else TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    Text(message, color = TextSecondary, fontSize = 14.sp)
  }
}

@Composable
private fun SampleHelmetQr(helmetId: String) {
  val matrix = remember(helmetId) { QRCodeWriter().encode(helmetId, BarcodeFormat.QR_CODE, 29, 29) }
  Box(
    Modifier.size(210.dp).background(Color.White, RoundedCornerShape(18.dp)).padding(14.dp).testTag("sample_helmet_qr"),
    contentAlignment = Alignment.Center
  ) {
    Canvas(Modifier.fillMaxSize()) {
      val moduleSize = size.minDimension / matrix.width
      for (row in 0 until matrix.height) for (column in 0 until matrix.width) {
        if (matrix[column, row]) drawRect(Color.Black, topLeft = androidx.compose.ui.geometry.Offset(column * moduleSize, row * moduleSize), size = androidx.compose.ui.geometry.Size(moduleSize, moduleSize), style = Fill)
      }
    }
  }
}
