package mobappdev.example.sensorapplication.data

/**
 * File: AndroidPolarController.kt
 * Purpose: Implementation of the PolarController Interface.
 *          Communicates with the polar API
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarAccelerometerData
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarHrData
import com.polar.sdk.api.model.PolarSensorSetting
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.PolarController
import java.util.UUID

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
    /*Non ci servirà:
    private var hrDisposable: Disposable? = null
    private val TAG = "AndroidPolarController"

    private val _currentHR = MutableStateFlow<Int?>(null)
    override val currentHR: StateFlow<Int?>
        get() = _currentHR.asStateFlow()

    private val _hrList = MutableStateFlow<List<Int>>(emptyList())
    override val hrList: StateFlow<List<Int>>
        get() = _hrList.asStateFlow()
    *///

    private var accDisposable: Disposable? = null
    private val TAG = "AndroidPolarController"

    private val _currentAcc = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentAcc: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentAcc.asStateFlow()

    private val _accList = MutableStateFlow<List<Triple<Float, Float, Float>>>(emptyList())
    override val accList: StateFlow<List<Triple<Float, Float, Float>>>
        get() = _accList.asStateFlow()

    ///

    private var gyroDisposable: Disposable? = null

    private val _currentGyro = MutableStateFlow<Triple<Float, Float, Float>>(null)
    override val currentGyro: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyro.asStateFlow()

    private val _gyroList = MutableStateFlow<List<Triple<Float, Float, Float>>>(emptyList())
    override val gyroList: StateFlow<List<Triple<Float, Float, Float>>>
        get() = _gyroList.asStateFlow()
    ///

    private val _connected = MutableStateFlow(false)
    override val connected: StateFlow<Boolean>
        get() = _connected.asStateFlow()

    private val _measuring = MutableStateFlow(false)
    override val measuring: StateFlow<Boolean>
        get() = _measuring.asStateFlow()

    init {
        api.setPolarFilter(false) //da settare true?

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
            }

            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTING: ${polarDeviceInfo.deviceId}")
            }

            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "DISCONNECTED: ${polarDeviceInfo.deviceId}")
                _connected.update { false }
            }

            override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
                Log.d(TAG, "DIS INFO uuid: $uuid value: $value")
            }
        })
    }

    override fun connectToDevice(deviceId: String) {
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

   /* override fun startHrStreaming(deviceId: String) {
        val isDisposed = hrDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            hrDisposable = api.startHrStreaming(deviceId)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { hrData: PolarHrData ->
                        for (sample in hrData.samples) {
                            _currentHR.update { sample.hr }
                            _hrList.update { hrList ->
                                hrList + sample.hr
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Hr stream failed.\nReason $error")
                    },
                    { Log.d(TAG, "Hr stream complete")}
                )
        } else {
            Log.d(TAG, "Already streaming")
        }

    }*/

    //aggiunta:
   private fun requestStreamSettings(identifier: String, feature: PolarBleApi.PolarDeviceDataType): Flowable<PolarSensorSetting> {
       val availableSettings = api.requestStreamSettings(identifier, feature)
       val allSettings = api.requestFullStreamSettings(identifier, feature)
           .onErrorReturn { error: Throwable ->
               Log.w(TAG, "Full stream settings are not available for feature $feature. REASON: $error")
               PolarSensorSetting(emptyMap())
           }
       return Single.zip(availableSettings, allSettings) { available: PolarSensorSetting, all: PolarSensorSetting ->
           if (available.settings.isEmpty()) {
               throw Throwable("Settings are not available")
           } else {
               Log.d(TAG, "Feature " + feature + " available settings " + available.settings)
               Log.d(TAG, "Feature " + feature + " all settings " + all.settings)
               return@zip android.util.Pair(available, all)
           }
       }
           .observeOn(AndroidSchedulers.mainThread())
           .toFlowable()
           .flatMap { sensorSettings: android.util.Pair<PolarSensorSetting, PolarSensorSetting> ->
               DialogUtility.showAllSettingsDialog(
                   this@MainActivity,
                   sensorSettings.first.settings,
                   sensorSettings.second.settings
               ).toFlowable()
           }
   }

    override fun startAccStreaming(deviceId: String) {
        val isDisposed = accDisposable?.isDisposed ?: true
        if(isDisposed) {
            _measuring.update { true }
            ///
            accDisposable = requestStreamSettings(deviceId, PolarBleApi.PolarDeviceDataType.ACC)
                .flatMap { settings: PolarSensorSetting ->
                    api.startAccStreaming(deviceId, settings)
                }///
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    { accData: PolarAccelerometerData ->
                        for (sample in accData.samples) {
                            _currentAcc.update { sample.acc}
                            _hrList.update { hrList ->
                                hrList + sample.hr
                            }
                        }
                    },
                    { error: Throwable ->
                        Log.e(TAG, "Hr stream failed.\nReason $error")
                    },
                    { Log.d(TAG, "Hr stream complete")}
                )
        } else {
            Log.d(TAG, "Already streaming")
        }

    }

    override fun stopHrStreaming() {
        _measuring.update { false }
        hrDisposable?.dispose()
        _currentHR.update { null }
    }
}