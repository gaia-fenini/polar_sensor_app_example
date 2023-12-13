package mobappdev.example.sensorapplication.ui.viewmodels

/**
 * File: DataVM.kt
 * Purpose: Defines the view model of the data screen.
 *          Uses Dagger-Hilt to inject a controller model
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobappdev.example.sensorapplication.domain.BluetoothDevice
import mobappdev.example.sensorapplication.domain.CsvWriter
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.domain.PolarController
import java.io.FileOutputStream
import java.io.OutputStream
import java.lang.Math.atan
import java.lang.Math.pow
import java.lang.Math.sqrt
import javax.inject.Inject
import kotlin.math.pow

@HiltViewModel
@SuppressLint("StaticFieldLeak")
class DataVM @Inject constructor(
    private val polarController: PolarController,
    private val internalSensorController: InternalSensorController,
    private val context: Context
): ViewModel() {

    private val csvWriter = CsvWriter(context)

    private val _isWriteSuccessful = mutableStateOf(false)
    val isWriteSuccessful: State<Boolean> get() = _isWriteSuccessful


    private var streamType: StreamType? = null

    private val accDataFlow = internalSensorController.currentLinAccUI
    val gyroDataFlow = internalSensorController.currentGyroUI
    val angleDataFlow = internalSensorController.currentAngleUI
    private val anglesList = internalSensorController.angles

    //private val accDataFlowPolar = polarController.currentAccUI
    //private val gyroDataFlowPolar = polarController.currentGyroUI

    val angleDataFlowPolar = polarController.currentAngle
    val angleListPolar = polarController.angleList

    val combinedDataFlow = combine(
        gyroDataFlow,
        accDataFlow,
        angleDataFlow
    ) { gyro, acc , ang->
        if (acc != null ) {
            CombinedSensorData.AccData(acc)
        } else if (gyro != null) {
            CombinedSensorData.GyroData(gyro)
        }
        else if(ang!= null){
            CombinedSensorData.AngleData(ang)
        }

        else {
            null
        }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _state = MutableStateFlow(DataUiState())

    val state = combine(
        polarController.scannedDevices,
        polarController.pairedDevices,
    //val hrList: List<Int> = emptyList(),
        polarController.currentAcc,
        polarController.currentGyro,
   // polarController.connected,
        _state
    ) { scannedDevices,pairedDevices, currentAcc, currentGyro,/* connected, */ state ->
        state.copy(

            scannedDevices = scannedDevices,
            pairedDevices = pairedDevices,
            currentAcc = currentAcc,
            currentGyro = currentGyro,
            //connected = connected,

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
        _state.update { it.copy(connected = true) }

    }

    fun disconnectFromSensor() {
        stopDataStream()
        polarController.disconnectFromDevice(_deviceId.value)
        _state.update { it.copy(connected = false) }
    }

    fun startGyroInt() {
        streamType = StreamType.LOCAL_GYRO
        internalSensorController.startGyroStream()
        _state.update { it.copy(measuring = true) }
    }
    fun startGyroPolar() {
        polarController.startGyroStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_GYRO
        _state.update { it.copy(measuring = true) }
    }

    fun startAccPolar() {
        polarController.startAccStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_ACC
        _state.update { it.copy(measuring = true) }
    }
    fun startAccInt() {
        internalSensorController.startImuStream()
        streamType = StreamType.LOCAL_ACC
        _state.update { it.copy(measuring = true) }

    }

    fun stopDataStream(){
         when (streamType) {
            StreamType.LOCAL_GYRO -> internalSensorController.stopGyroStream()
            StreamType.LOCAL_ACC -> internalSensorController.stopImuStream()
            StreamType.FOREIGN_ACC -> polarController.stopAccStreaming()
            StreamType.FOREIGN_GYRO -> polarController.stopGyroStreaming()
            else -> {} // Do nothing
        }
        //saveAngles()
        _state.update { it.copy(measuring = false) }
        Log.d("Test","${anglesList.value}")
        //writeCsvFile()
    }
    fun writeCsvFile() {
        viewModelScope.launch(Dispatchers.IO) {
            csvWriter.writeCsv("angles",when (streamType) {StreamType.LOCAL_GYRO , StreamType.LOCAL_ACC -> anglesList.value!!
                StreamType.FOREIGN_ACC , StreamType.FOREIGN_GYRO-> angleListPolar.value
                else-> emptyList()
            })
        }
    }
}


data class DataUiState(
    val scannedDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    //val hrList: List<Int> = emptyList(),
    val currentAcc: Triple<Int,Int,Int>? = Triple(0,0,0),
    val currentGyro: Triple<Float,Float,Float>? = Triple(0f,0f,0f),
    val angleList: List<Double> = emptyList(),
    val connected: Boolean = false,
    val measuring: Boolean = false,
    val connecting: Boolean = false
    )


enum class StreamType {
    LOCAL_GYRO, LOCAL_ACC, FOREIGN_ACC, FOREIGN_GYRO
}

sealed class CombinedSensorData {
    data class GyroData(val gyro: Triple<Float, Float, Float>?) : CombinedSensorData()
    //data class HrData(val hr: Int?) : CombinedSensorData()
    data class AccData(val acc: Triple<Float, Float, Float>?) : CombinedSensorData()
    data class AngleData(val acc: Double?) : CombinedSensorData()

}