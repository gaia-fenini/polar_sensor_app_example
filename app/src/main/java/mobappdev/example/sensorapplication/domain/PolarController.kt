package mobappdev.example.sensorapplication.domain

/**
 * File: PolarController.kt
 * Purpose: Defines the blueprint for the polar controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */


import kotlinx.coroutines.flow.StateFlow

interface PolarController {

    val scannedDevices: StateFlow<List<BluetoothDevice>>

    val pairedDevices: StateFlow<List<BluetoothDevice>>
    fun startDiscovery()
    fun stopDiscovery()

    fun release()

    val currentAcc: StateFlow<Triple<Int, Int, Int>?>
    val accList: StateFlow<List<Triple<Int, Int, Int>>>

    val currentGyro: StateFlow<Triple<Float, Float, Float>?>
    val gyroList: StateFlow<List<Triple<Float, Float, Float>>>

    val connected: StateFlow<Boolean>
    val measuring: StateFlow<Boolean>

    fun connectToDevice(deviceId: String)
    fun disconnectFromDevice(deviceId: String)


    //fun startAccStreaming(deviceId: String)
    fun stopAccStreaming()
   // fun startGyroStreaming(deviceId: String)
    fun stopGyroStreaming()

}