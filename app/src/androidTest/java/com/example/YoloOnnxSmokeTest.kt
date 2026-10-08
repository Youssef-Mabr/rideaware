package com.example

import androidx.test.platform.app.InstrumentationRegistry
import com.example.camera.AssetCameraFrameSource
import com.example.model.CameraSource
import com.example.detection.YoloOnnxDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoloOnnxSmokeTest {
  @Test fun bundledImageProducesRealYoloDetections() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val detector = YoloOnnxDetector(context)
    try {
      val frame = AssetCameraFrameSource(context, "yolo_test_bus.jpg", CameraSource.FRONT).nextFrame(1234L)
      val detections = detector.detect(frame)
      android.util.Log.i("RideAwareYoloSmoke", detections.joinToString(" | ") {
        "${it.objectType}:${it.confidence}:${it.boundingBox}"
      })
      assertFalse(detections.isEmpty())
      assertTrue(detections.all { it.confidence in 0f..1f })
      assertTrue(detections.all { it.boundingBox.left in 0f..1f && it.boundingBox.right in 0f..1f })
    } finally {
      detector.close()
    }
  }
}