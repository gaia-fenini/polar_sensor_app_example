package mobappdev.example.sensorapplication.ui.viewmodels

/**
 * File: DataVM.kt
 * Purpose: Defines the view model of the data screen.
 *          Uses Dagger-Hilt to inject a controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.BluetoothDevice
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.domain.PolarController
import java.lang.Math.atan
import java.lang.Math.pow
import java.lang.Math.sqrt
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
class DataVM @Inject constructor(
    private val polarController: PolarController,
    private val internalSensorController: InternalSensorController
): ViewModel() {

    private var streamType: StreamType? = null //spostato

    private val accDataFlow = internalSensorController.currentLinAccUI
    private val gyroDataFlow = internalSensorController.currentGyroUI

    //private val accDataFlowPolar = polarController.currentAccUI
    //private val gyroDataFlowPolar = polarController.currentGyroUI

    private val _angle = MutableStateFlow(0f)
    val angle: StateFlow<Float>
        get() = _angle.asStateFlow()

    // Combine the two data flows
   /* val combinedDataFlow= combine(
        gyroDataFlow,
        hrDataFlow,
    ) { gyro, hr ->
        if (hr != null ) {
            CombinedSensorData.HrData(hr)
        } else if (gyro != null) {
            CombinedSensorData.GyroData(gyro)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
*/
    val combinedDataFlow= combine(
        gyroDataFlow,
        accDataFlow,
    ) { gyro, acc ->
        if (acc != null ) {
            CombinedSensorData.AccData(acc)
        } else if (gyro != null) {
            CombinedSensorData.GyroData(gyro)
        } else {
            null
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(DataUiState())

    val state = combine(
        polarController.pairedDevices,
        polarController.scannedDevices,
        polarController.accList,
        polarController.connected,
        _state
    ) { pairedDevices, scannedDevices, accList, connected, state ->
        state.copy(
            pairedDevices = pairedDevices,
            scannedDevices = scannedDevices,
            accList = accList,
            connected = connected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _state.value)

    fun startScan(){
        polarController.startDiscovery()
    }
    fun stopScan(){
        polarController.stopDiscovery()
    }

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String>
        get() = _deviceId.asStateFlow()


    fun chooseSensor(deviceId: String) {
        _deviceId.update { deviceId }
        connectToSensor()
    }

    fun connectToSensor() {
        polarController.connectToDevice(_deviceId.value)
    }

    fun disconnectFromSensor() {
        stopDataStream()
        polarController.disconnectFromDevice(_deviceId.value)
    }
/*
    fun startHr() {
        polarController.startHrStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_HR
        _state.update { it.copy(measuring = true) }
    }*/

    fun startGyroInt() {
        internalSensorController.startGyroStream()
        streamType = StreamType.LOCAL_GYRO

        _state.update { it.copy(measuring = true) }
    }
    /*fun startGyroPolar() {
        polarController.startGyroStreaming(_deviceId.value)
        streamType = StreamType.LOCAL_GYRO

        _state.update { it.copy(measuring = true) }
    }*/

    /*fun startAccPolar() {
        polarController.startAccStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_ACC
        _state.update { it.copy(measuring = true) }
    }*/
    fun startAccInt() {
        internalSensorController.startAccStream()
        streamType = StreamType.FOREIGN_ACC
        _state.update { it.copy(measuring = true) }
    }


    fun stopDataStream(){
        when (streamType) {
            StreamType.LOCAL_GYRO -> internalSensorController.stopGyroStream()
            StreamType.LOCAL_ACC -> internalSensorController.stopLinAccStream()
            StreamType.FOREIGN_ACC -> polarController.stopAccStreaming()
            StreamType.FOREIGN_GYRO -> polarController.stopGyroStreaming()
            else -> {} // Do nothing
        }
        _state.update { it.copy(measuring = false) }
    }


/*
    fun computeAngleAcc(){
            if (streamType == StreamType.LOCAL_ACC) {

                _angle.update { atan((accDataFlow.value?.first.pow(2)) / (sqrt(
                    accDataFlow.value?.second.pow(2) + accDataFlow.value.third.pow(2)))) }

            }

    }
*/
}


data class DataUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val hrList: List<Int> = emptyList(),
    val accList: List<Triple<Int,Int,Int>> = emptyList(),
    val gyroList: List<Triple<Int,Int,Int>> = emptyList(),
    val connected: Boolean = false,
    val measuring: Boolean = false
)


enum class StreamType {
    LOCAL_GYRO, LOCAL_ACC, FOREIGN_ACC, FOREIGN_GYRO
}

sealed class CombinedSensorData {
    data class GyroData(val gyro: Triple<Float, Float, Float>?) : CombinedSensorData()
    data class HrData(val hr: Int?) : CombinedSensorData()
    data class AccData(val gyro: Triple<Float, Float, Float>?) : CombinedSensorData()

}