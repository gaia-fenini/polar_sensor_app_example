package mobappdev.example.sensorapplication.domain

/**
 * File: InternalSensorController.kt
 * Purpose: Defines the blueprint for the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.flow.StateFlow

interface InternalSensorController {
    val currentLinAccUI: StateFlow<Triple<Float, Float, Float>?>
    val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
    val currentAngleUI: StateFlow<Double?>
    val streamingGyro: StateFlow<Boolean>
    val streamingLinAcc: StateFlow<Boolean>

    val angles : StateFlow<List<Double>?>

    fun startImuStream()
    fun stopImuStream()

    fun startGyroStream()
    fun stopGyroStream()
}