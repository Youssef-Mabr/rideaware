package com.example.detection

import android.content.Context
import android.graphics.Bitmap
import com.example.camera.CameraFrame
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.io.File
import kotlin.math.max
import kotlin.math.min

class YoloOnnxDetector(
  context: Context,
  modelAssetName: String = "yolov8n_compatible.onnx",
  private val inputSize: Int = 640
) : ObjectDetector, AutoCloseable {
  private val environment = OrtEnvironment.getEnvironment()
  private val session: OrtSession

  init {
    val modelFile = File(context.filesDir, modelAssetName)
    if (!modelFile.exists() || modelFile.length() == 0L) {
      context.assets.open(modelAssetName).use { input ->
        modelFile.outputStream().use { output -> input.copyTo(output) }
      }
    }
    session = environment.createSession(modelFile.absolutePath, OrtSession.SessionOptions())
  }

  override fun detect(frame: CameraFrame): List<DetectionResult> {
    val bitmap = frame.bitmap ?: return emptyList()
    val input = YoloImagePreprocessor.prepare(bitmap, inputSize)
    OnnxTensor.createTensor(environment, FloatBuffer.wrap(input.values), longArrayOf(1, 3, inputSize.toLong(), inputSize.toLong())).use { tensor ->
      val inputName = session.inputNames.first()
      session.run(mapOf(inputName to tensor)).use { result ->
        val raw = result[0].value as Array<*>
        val channels = raw[0] as Array<*>
        val channelCount = channels.size
        val candidateCount = (channels[0] as FloatArray).size
        val candidates = buildList {
          for (index in 0 until candidateCount) {
            var bestClass = -1
            var bestScore = 0f
            for (classIndex in 0 until min(80, channelCount - 4)) {
              val score = (channels[classIndex + 4] as FloatArray)[index]
              if (score > bestScore) { bestScore = score; bestClass = classIndex }
            }
            if (bestClass >= 0) add(YoloDetectionCandidate(
              centerX = (channels[0] as FloatArray)[index] / inputSize,
              centerY = (channels[1] as FloatArray)[index] / inputSize,
              width = (channels[2] as FloatArray)[index] / inputSize,
              height = (channels[3] as FloatArray)[index] / inputSize,
              classIndex = bestClass,
              confidence = bestScore
            ))
          }
        }
        return YoloPostprocessor.candidatesToResults(candidates, frame.capturedAtMillis, frame.source, frame.sequence)
      }
    }
  }

  override fun close() { session.close(); environment.close() }
}

data class PreprocessedImage(val values: FloatArray)

object YoloImagePreprocessor {
  fun prepare(bitmap: Bitmap, inputSize: Int): PreprocessedImage {
    val resized = Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true)
    val pixels = IntArray(inputSize * inputSize)
    resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)
    val values = FloatArray(3 * inputSize * inputSize)
    val planeSize = inputSize * inputSize
    for (index in pixels.indices) {
      val pixel = pixels[index]
      values[index] = ((pixel shr 16) and 0xff) / 255f
      values[planeSize + index] = ((pixel shr 8) and 0xff) / 255f
      values[2 * planeSize + index] = (pixel and 0xff) / 255f
    }
    return PreprocessedImage(values)
  }
}