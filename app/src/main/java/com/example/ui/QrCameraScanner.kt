package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.DarkCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TealAccent
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrCameraScanner(
  onPayload: (String) -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
  var permissionDenied by remember { mutableStateOf(false) }
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    hasPermission = granted
    permissionDenied = !granted
  }

  LaunchedEffect(Unit) {
    if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
  }

  Box(modifier.fillMaxSize().background(DarkCanvas)) {
    when {
      hasPermission -> CameraPreview(onPayload = onPayload)
      permissionDenied -> PermissionDeniedContent()
      else -> Text("Requesting camera access…", color = TextSecondary, modifier = Modifier.align(Alignment.Center))
    }
    ScannerTopBar(onClose = onClose)
  }
}

@Composable
private fun CameraPreview(onPayload: (String) -> Unit) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current
  val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
  val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
  val executor = remember { Executors.newSingleThreadExecutor() }
  val scanner = remember { BarcodeScanning.getClient() }

  DisposableEffect(cameraProviderFuture, lifecycleOwner, previewView) {
    val listener = Runnable {
      val provider = cameraProviderFuture.get()
      val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
      val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
      var delivered = false
      analysis.setAnalyzer(executor) { imageProxy ->
        val mediaImage = imageProxy.image
        if (mediaImage == null || delivered) {
          imageProxy.close()
        } else {
          scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { barcodes ->
              val value = barcodes.firstOrNull()?.rawValue?.trim()
              if (!value.isNullOrEmpty() && !delivered) {
                delivered = true
                onPayload(value)
              }
            }
            .addOnCompleteListener { imageProxy.close() }
        }
      }
      provider.unbindAll()
      provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
    }
    cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))
    onDispose {
      if (cameraProviderFuture.isDone) cameraProviderFuture.get().unbindAll()
      scanner.close()
      executor.shutdown()
    }
  }

  AndroidView(
    factory = { previewView },
    modifier = Modifier.fillMaxSize()
  )
}

@Composable
private fun ScannerTopBar(onClose: () -> Unit) {
  Column(
    Modifier.fillMaxWidth().padding(20.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    TextButton(onClick = onClose) { Text("Close", color = TealAccent) }
    Text("Scan helmet QR", color = TextPrimary, fontSize = 24.sp)
    Text("Point the camera at the generated QR code for RA-DEMO-001.", color = TextSecondary, fontSize = 14.sp)
  }
}

@Composable
private fun PermissionDeniedContent() {
  Text(
    "Camera permission is required to scan a helmet QR. Enable it in Android Settings, then return and try again.",
    color = TextSecondary,
    modifier = Modifier.fillMaxWidth().padding(28.dp),
    fontSize = 16.sp
  )
}

private fun Context.hasCameraPermission() = ContextCompat.checkSelfPermission(
  this, Manifest.permission.CAMERA
) == PackageManager.PERMISSION_GRANTED