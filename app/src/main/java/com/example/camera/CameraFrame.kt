package com.example.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.model.CameraSource

data class CameraFrame(
  val source: CameraSource,
  val capturedAtMillis: Long,
  val sequence: Long,
  val scenario: SimulationScenario,
  val bitmap: Bitmap? = null
)

enum class SimulationScenario {
  FRONT_LEAD_VEHICLE,
  REAR_APPROACHING_VEHICLE,
  LEFT_BLIND_SPOT_VEHICLE,
  PEDESTRIAN_CROSSING,
  EMPTY_ROAD
}

interface CameraFrameSource {
  fun nextFrame(nowMillis: Long = System.currentTimeMillis()): CameraFrame
}

class SimulatedCameraFrameSource(
  private val source: CameraSource,
  private val scenario: SimulationScenario = when (source) {
    CameraSource.REAR -> SimulationScenario.REAR_APPROACHING_VEHICLE
    else -> SimulationScenario.FRONT_LEAD_VEHICLE
  }
) : CameraFrameSource {
  private var sequence = 0L

  override fun nextFrame(nowMillis: Long): CameraFrame = CameraFrame(
    source = source,
    capturedAtMillis = nowMillis,
    sequence = sequence++,
    scenario = scenario
  )
}

class AssetCameraFrameSource(
  private val context: Context,
  private val assetName: String,
  private val source: CameraSource
) : CameraFrameSource {
  private var sequence = 0L
  private val bitmap: Bitmap by lazy {
    context.assets.open(assetName).use(BitmapFactory::decodeStream)
      ?: error("Could not decode camera test asset: $assetName")
  }

  override fun nextFrame(nowMillis: Long): CameraFrame = CameraFrame(
    source = source,
    capturedAtMillis = nowMillis,
    sequence = sequence++,
    scenario = SimulationScenario.EMPTY_ROAD,
    bitmap = bitmap
  )
}