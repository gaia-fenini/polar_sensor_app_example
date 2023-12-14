package mobappdev.example.sensorapplication.data

/**
 * File: AndroidPolarController.kt
 * Purpose: Implementation of the PolarController Interface.
 *          Communicates with the polar API
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarGyroData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.BluetoothDeviceDomain
import mobappdev.example.sensorapplication.domain.FoundDeviceReceiver
import mobappdev.example.sensorapplication.domain.PolarController
import mobappdev.example.sensorapplication.domain.toBluetoothDeviceDomain
import mobappdev.example.sensorapplication.ui.MainActivity
import java.util.UUID
import kotlin.math.abs
import kotlin.math.pow


@SuppressLint("MissingPermission")

class AndroidPolarController (
    private val context: Context,
): PolarController {

    private val api: PolarBleApi by lazy {
        // Notice all features are enabled
        PolarBleApiDefaultImpl.defaultImplementation(
            context = context,
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
                PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
                PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO
            )
        )
    }


    private val bluetoothManager by lazy{
    context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy{
        bluetoothManager?.adapter
    }


    private var accDisposable: Disposable? = null
    private val TAG = "AndroidPolarController"


    private val _scannedDevices= MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices= MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val foundDeviceReceiver = FoundDeviceReceiver{ device->
        _scannedDevices.update { devices->
            val newDevice = device.toBluetoothDeviceDomain()
            if (newDevice in devices) devices else devices + newDevice
    }
    }


    override fun startDiscovery() {
       //check permission

        context.registerReceiver(
            foundDeviceReceiver,
            IntentFilter(android.bluetooth.BluetoothDevice.ACTION_FOUND)
        )
        updatePairedDevices()
        bluetoothAdapter?.startDiscovery()

    }

    override fun stopDiscovery() {
        //check permission

        bluetoothAdapter?.cancelDiscovery()
    }

    override fun release() {
        context.unregisterReceiver(foundDeviceReceiver)
    }
    private fun updatePairedDevices(){
        //if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)){
          //  return}
        bluetoothAdapter
            ?.bondedDevices
            ?.map{ it.toBluetoothDeviceDomain()}
            ?.also{ devices->
                _pairedDevices.update { devices }}
    }
    private fun hasPermission(permission: String): Boolean{
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }


    private val _currentAcc = MutableStateFlow<Triple<Int, Int, Int>?>(null)

    override val currentAcc: StateFlow<Triple<Int, Int, Int>?>
        get() = _currentAcc.asStateFlow()

    private val _accList = MutableStateFlow<List<Triple<Int, Int, Int>>>(emptyList())
    override val accList: StateFlow<List<Triple<Int, Int, Int>>>
        get() = _accList.asStateFlow()

    ///

    private var gyroDisposable: Disposable? = null

    private val _currentGyro = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentGyro: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyro.asStateFlow()

    private val _gyroList = MutableStateFlow<List<Triple<Float, Float, Float>>>(emptyList())
    override val gyroList: StateFlow<List<Triple<Float, Float, Float>>>
        get() = _gyroList.asStateFlow()

    ///
    private val _currentAngle = MutableStateFlow<Double?>(null)
    override val currentAngle: StateFlow<Double?>
        get() = _currentAngle.asStateFlow()

    private val _angleList = MutableStateFlow<List<Double>>(emptyList())
    override val angleList: StateFlow<List<Double>>
        get() = _angleList.asStateFlow()

    var lastIndex = if (angleList.value!= null) _angleList.value.lastIndex else 0

    private var _previousGyroAngle: Float? = 0f

    private var _currentGyroAngle: Float? = null

    private val _gyrozero = MutableStateFlow(0f)
    override val gyrozero: StateFlow<Float>
        get() = _gyrozero.asStateFlow()

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean>
        get() = _connected.asStateFlow()

    private val _connecting = MutableStateFlow(false)
    override val connecting: StateFlow<Boolean>
        get() = _connecting.asStateFlow()

    private val _measuring = MutableStateFlow(false)
    override val measuring: StateFlow<Boolean>
        get() = _measuring.asStateFlow()

    init {
        updatePairedDevices()
        api.setPolarFilter(true) //da settare true?

        val enableSdkLogs = false
        if(enableSdkLogs) {
            api.setApiLogger { s: String -> Log.d("Polar API Logger", s) }
        }

        api.setApiCallback(object: PolarBleApiCallback() {
            override fun batteryLevelReceived(identifier: String, level: Int) {
                Log.d(TAG, "BATTERY LEVEL: $level")
            }

            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { true }
                _connecting.update{false}

            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {

                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { false }
                _connecting.update{false}

            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }
        })
    }




    override fun connectToDevice(deviceId: String) {
        _connecting.update{true}

        try {
            api.connectToDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to connect to $deviceId.\n Reason $polarInvalidArgument")
        }
    }

    override fun disconnectFromDevice(deviceId: String) {
        try {
            api.disconnectFromDevice(deviceId)
        } catch (polarInvalidArgument: PolarInvalidArgument) {
            Log.e(TAG, "Failed to disconnect from $deviceId.\n Reason $polarInvalidArgument")
        }
    }

    fun computeAngle() {
        if(_currentAcc!=null && _angleList!=null){
            _currentAngle.update {
                90 - kotlin.math.atan( (_currentAcc.value!!.second.toDouble()*9.81/1000).pow(2.0)/
                    kotlin.math.sqrt(
                        (_currentAcc.value!!.first.toDouble()*9.81/1000).pow(2.0)
                        + (_currentAcc.value!!.third.toDouble()*9.81/1000).pow(2.0)
            ))* 180 / kotlin.math.PI}
            if(_angleList.value.isNotEmpty()){
                val newelem = 0.9 * _currentAngle.value!! + 0.1 * (_angleList.value[lastIndex])
                _currentAngle.update{newelem}
                _angleList.value = _angleList.value + newelem
            }
            else{
                _angleList.value = angleList.value + _currentAngle.value!!
            }
             Log.d("Test","angle: ${currentAngle.value}" )
        }
        else{Log.d("Test", "acc null")}
    }

    fun computeAngleGyro(){
        if(_currentAcc!=null && _previousGyroAngle!=null){
            if(900f<_currentAcc.value!!.second && _currentAcc.value!!.second<1100f){
                _currentGyroAngle = 0f
                _previousGyroAngle = _currentGyroAngle
                _currentAngle.update{0.0}
                _angleList.value = _angleList.value!! + _currentAngle.value!!
            }
            else{
            val fromAcc = 90 - kotlin.math.atan(
                (_currentAcc.value!!.second.toDouble()*9.81/1000).pow(2.0) / kotlin.math.sqrt(
                    (_currentAcc.value!!.first.toDouble()*9.81/1000).pow(2.0)
                            + (_currentAcc.value!!.third.toDouble()*9.81/1000).pow(2.0)
                )
            ) * 180 / kotlin.math.PI
            _currentGyroAngle = _previousGyroAngle!! + _currentGyro.value!!.first * 0.5f
                val fromGyro = _currentGyroAngle!!
            _currentAngle.update {
                (0.99 * fromAcc + 0.01 * abs(fromGyro))
            }
            _previousGyroAngle = _currentGyroAngle
            Log.d("Test","${_currentGyroAngle}")
            _angleList.value = angleList.value + _currentAngle.value!!
        }  }     else{Log.d("Test", "acc null")}
    }

    override fun startAccStreaming(deviceId: String) {
        val mainActivity = MainActivity()
        _accList.update { emptyList() }
        _angleList.update { emptyList() }
        _currentAcc.update { Triple(0,0,0) }
        _currentAngle.update { null }
        Log.d(TAG, "Im in the start acc stream ")
        val isDisposed = accDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            Log.d(TAG, "measuring is ${_measuring.value}")
            accDisposable =
                mainActivity.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
                    .flatMap { settings: PolarSensorSetting->
                        api.startAccStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { accData: PolarAccelerometerData ->
                            for (sample in accData.samples) {
                                Log.d(TAG, "I'm in the for loop")
                                _currentAcc.update{Triple(sample.x, sample.y, sample.z)
                                }
                                Log.d(TAG, "I've updated the currentacc")
                                _accList.update { accList ->
                                    val updateListAcc =
                                    accList + Triple(sample.x, sample.y, sample.z)
                                    Log.d(TAG, "update acclist: ${updateListAcc}")
                                    updateListAcc
                                }
                                lastIndex = if (_angleList.value!= null) _angleList.value.lastIndex else 0
                                computeAngle()
                            }
                        },
                        { error: Throwable ->
                            Log.e(TAG, "Accelerometer stream failed.\nReason $error")
                        },
                        { Log.d(TAG, "Accelerometer stream complete")}
                    )
        } else {
            Log.d(TAG, "Already streaming")
        }
    }


    override fun startGyroStreaming(deviceId: String) {
        val mainActivity = MainActivity()
        _gyroList.update { emptyList() }
        _angleList.update { emptyList() }
        _currentGyro.update { Triple(0f,0f,0f) }
        _currentAngle.update {null}
        _currentAcc.update { Triple(0,0,0) }
        Log.d(TAG, "I'm in the startGyroStreaming in the AndroidPolarController")
        val isDisposed = gyroDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            Log.d(TAG, "_measuring is true")
            gyroDisposable =
                mainActivity.requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.GYRO)
                    .flatMap { settings: PolarSensorSetting ->
                        api.startGyroStreaming(deviceId, settings)
                    }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        { gyroData: PolarGyroData ->
                            for (sample in gyroData.samples) {
                                Log.d(TAG, "I'm in the for loop in the function in the APC")
                                _currentGyro.update {
                                    Triple(sample.x, sample.y, sample.z)

                                }
                                Log.d(TAG, "I have updated the current gyro")
                                _gyroList.update { gyroList ->
                                    val updatedList =
                                        gyroList + Triple(sample.x, sample.y, sample.z)
                                    Log.d(TAG, "Updated Gyro List: $updatedList")
                                    updatedList
                                }
                                computeAngleGyro()
                            }

                        },
                        { error: Throwable ->
                            Log.e(TAG, "Gyro stream failed.\nReason $error")
                        },
                        { Log.d(TAG, "Gyro stream complete, ${_currentGyro.value}")}
                    )
        } else {
            Log.d(TAG, "Already streaming")
        }

    }

    override fun stopAccStreaming() {
        _measuring.update { false }
        accDisposable?.dispose()
        //_currentAcc.update { null }
    }

    override fun stopGyroStreaming() {
        _measuring.update { false }
        gyroDisposable?.dispose()
        //_currentGyro.update { null }
    }

}

