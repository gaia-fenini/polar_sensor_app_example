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
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import mobappdev.example.sensorapplication.data.UserPreferencesRepository
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
    private val userPreferencesRepository: UserPreferencesRepository,
    private val polarController: PolarController,
    private val internalSensorController: InternalSensorController,
    private val context: Context
): ViewModel() {

    private val _highscore = MutableStateFlow<List<List<Double>>?>(null)
    val highscore: StateFlow<List<List<Double>>?>
        get() = _highscore

    private val csvWriter = CsvWriter(context)

    private val _isWriteSuccessful = mutableStateOf(false)
    val isWriteSuccessful: State<Boolean> get() = _isWriteSuccessful

    private val _isAcc = MutableStateFlow(false)
    val isAcc: StateFlow<Boolean> get() = _isAcc

    private val _isAccNGyro = MutableStateFlow(false)
    val isAccNGyro: StateFlow<Boolean> get() = _isAccNGyro

    private val _isFailed = MutableStateFlow(false)
    val isFailed: StateFlow<Boolean> get() = _isFailed


    private var streamType: StreamType? = null

    private val accDataFlow = internalSensorController.currentLinAccUI
    val gyroDataFlow = internalSensorController.currentGyroUI
    val angleDataFlow = internalSensorController.currentAngleUI
    val anglesList = internalSensorController.angles


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

    init {
        // Code that runs during creation of the vm
        viewModelScope.launch {
            userPreferencesRepository.highscore.collect {
                _highscore.value = it
            }
        }
    }

    private val _stateConnection = MutableStateFlow(Connection())

    val stateConnection = combine(
        polarController.connected,
        polarController.connecting,
        polarController.measuring,
        _stateConnection
    ) { connected, connecting, measuring, stateConnection ->
        stateConnection.copy(

            connected = connected,
            connecting = connecting,
            measuring = measuring

        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), _stateConnection.value)
    data class Connection(
        val connected: Boolean = false,
        val connecting: Boolean = false,
        val measuring: Boolean = false
    )

    fun startScan(){
        polarController.startDiscovery()
    }
    fun stopScan(){
        polarController.stopDiscovery()
    }

    fun setIsAcc(value: Boolean) {
        _isAcc.value = value
    }

    fun setIsAccNGyro(value: Boolean) {
        _isAccNGyro.value = value
    }

    private val _deviceId = MutableStateFlow("")
    val deviceId: StateFlow<String>
        get() = _deviceId.asStateFlow()


    fun chooseSensor(deviceId: String) {
        _deviceId.update { deviceId }
        connectToSensor()
    }

    fun connectToSensor() {
        _isFailed.value = false
        viewModelScope.launch {
            try {
                withTimeout(5000L) {
                    polarController.connectToDevice(_deviceId.value)
                    //_state.update { it.copy(connected = true) }
                    _stateConnection.update {
                        it.copy(
                            connected = polarController.connected.value,
                            connecting = polarController.connecting.value
                        )
                    }
                }

            } catch (e: TimeoutCancellationException) {
                _isFailed.value = true
                Log.d(TAG, "$e")
            } finally {
                stopScan()
            }
        }
    }

    fun disconnectFromSensor() {
        stopDataStream()
        polarController.disconnectFromDevice(_deviceId.value)
        //_state.update { it.copy(connected = false) }
        _stateConnection.update { it.copy(connected = polarController.connected.value,
            connecting = polarController.connecting.value) }
    }

    fun startGyroInt() {
        streamType = StreamType.LOCAL_GYRO
        internalSensorController.startGyroStream()
        //_state.update { it.copy(measuring = true) }
    }
    fun startGyroPolar() {
        polarController.startGyroStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_GYRO
        _stateConnection.update { it.copy(measuring = polarController.measuring.value) }
    }

    fun startAccPolar() {
        polarController.startAccStreaming(_deviceId.value)
        streamType = StreamType.FOREIGN_ACC
        _stateConnection.update { it.copy(measuring = polarController.measuring.value) }
    }
    fun startAccInt() {
        internalSensorController.startImuStream()
        streamType = StreamType.LOCAL_ACC
        //_state.update { it.copy(measuring = true) }

    }

    private var accJob: Job? = null
    private var gyroJob: Job? = null

    fun startAccAndGyroStreaming() {
        accJob = viewModelScope.launch {
            startAccPolar()
            }

        gyroJob = viewModelScope.launch {
            startGyroPolar()
        }
    }

    fun stopAccAndGyroStreaming() {
        accJob = viewModelScope.launch {polarController.stopAccStreaming() }
        gyroJob = viewModelScope.launch {polarController.stopGyroStreaming() }
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
        //_state.update { it.copy(measuring = false) }
        Log.d("Test","${anglesList.value}")
        viewModelScope.launch {
            userPreferencesRepository.saveAngles(anglesList.value!!)
        }
        //writeCsvFile()
    }
    fun writeCsvFile() {
        Log.d("Test","preparing to write")
        Log.d("Test", "list: ${anglesList.value}")
        Log.d("Test","type: ${streamType}")
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("Test","writing")
            csvWriter.writeCsv("angles",when (streamType) {StreamType.LOCAL_GYRO , StreamType.LOCAL_ACC -> anglesList.value!!
                StreamType.FOREIGN_ACC , StreamType.FOREIGN_GYRO-> angleListPolar.value
                else-> emptyList()
            })
        }
    }

    fun export(list : List<Double>){

        viewModelScope.launch(Dispatchers.IO) {
            Log.d("Test","writing")
            csvWriter.writeCsv("angles", list )
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
    //val connected: Boolean = false,
    //val measuring: Boolean = false,
    //val connecting: Boolean = false
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