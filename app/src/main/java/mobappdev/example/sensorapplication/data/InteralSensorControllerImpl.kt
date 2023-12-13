package mobappdev.example.sensorapplication.data

/**
 * File: InternalSensorControllerImpl.kt
 * Purpose: Implementation of the Internal Sensor Controller.
 * Author: Jitse van Esch
 * Created: 2023-09-21
 * Last modified: 2023-09-21
 */

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.ui.viewmodels.StreamType
import kotlin.math.abs
import kotlin.math.pow

private const val LOG_TAG = "Internal Sensor Controller"

class InternalSensorControllerImpl(
    context: Context
): InternalSensorController, SensorEventListener {

    private var _currentLinAcc: Triple<Float, Float, Float>? = null

    // Expose acceleration to the UI
    private val _currentLinAccUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentLinAccUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentLinAccUI.asStateFlow()

    //xn
    private val _currentAngleUI = MutableStateFlow<Double?>(null)
    override val currentAngleUI: StateFlow<Double?>
        get() = _currentAngleUI.asStateFlow()

    private var _currentGyro: Triple<Float, Float, Float>? = null

    // Expose gyro to the UI on a certain interval
    private val _currentGyroUI = MutableStateFlow<Triple<Float, Float, Float>?>(null)
    override val currentGyroUI: StateFlow<Triple<Float, Float, Float>?>
        get() = _currentGyroUI.asStateFlow()

    private var _previousGyroAngle: Float? = 0f

    private var _currentGyroAngle: Float? = null

    private val _streamingGyro = MutableStateFlow(false)
    override val streamingGyro: StateFlow<Boolean>
        get() = _streamingGyro.asStateFlow()

    private val _streamingLinAcc = MutableStateFlow(false)
    override val streamingLinAcc: StateFlow<Boolean>
        get() = _streamingLinAcc.asStateFlow()


    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }
    //
    private val linAccSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    //
    private val _gyrozero = MutableStateFlow(0f)
    override val gyrozero: StateFlow<Float>
        get() = _gyrozero.asStateFlow()

    //xn




    //y
    private val _angles = MutableStateFlow<List<Double>> (emptyList())

    override val angles: StateFlow<List<Double>>
        get() = _angles.asStateFlow()



    var lastIndex = if (_angles.value!= null) _angles.value!!.lastIndex else 0



    @OptIn(DelicateCoroutinesApi::class)
    override fun startGyroStream() {
        if (gyroSensor == null) {
            Log.e(LOG_TAG, "Gyroscope sensor is not available on this device")
            return
        }
        if (_streamingGyro.value) {
            Log.e(LOG_TAG, "Gyroscope sensor is already streaming")
            return
        }
        if (linAccSensor == null) {
            Log.e(LOG_TAG, "Accelerometer sensor is not available on this device")
            return
        }
        if (_streamingLinAcc.value) {
            Log.e(LOG_TAG, "Accelerometer sensor is already streaming")
            return
        }

        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, linAccSensor, SensorManager.SENSOR_DELAY_UI)

        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_UI)

        // Start a coroutine to update the UI variable on a 2 Hz interval
        GlobalScope.launch(Dispatchers.Main) {
            _streamingLinAcc.value = true
            _streamingGyro.value = true
            while (_streamingGyro.value) {

                // Update the UI variable
                _currentLinAccUI.update { _currentLinAcc }
                _currentGyroUI.update { _currentGyro }
                computeAngleGyro()

                delay(300)
            }
        }
    }

    fun computeAngleGyro(){

        if(_currentLinAcc!=null && _previousGyroAngle!=null) {
            if (9.4f < _currentLinAcc!!.second && _currentLinAcc!!.second < 10f) {
                _currentGyroAngle = 0f
                _previousGyroAngle = _currentGyroAngle
                _currentAngleUI.update {
                    0.0
                }
                _angles.value = angles.value!! + _currentAngleUI.value!!
                Log.d("Test", "update zero: ${_currentGyroAngle}")
            } else {
                val fromAcc = 90 - kotlin.math.atan(
                    _currentLinAcc!!.second.toDouble().pow(2.0) / kotlin.math.sqrt(
                        _currentLinAcc!!.first.toDouble().pow(2.0)
                                + _currentLinAcc!!.third.toDouble().pow(2.0)
                    )
                ) * 180 / kotlin.math.PI
                _currentGyroAngle = _previousGyroAngle!! + _currentGyro!!.first * 0.5f
                val fromGyro = _currentGyroAngle!! * 180 / kotlin.math.PI

                _currentAngleUI.update {
                    (0.9 * fromAcc + 0.1 * abs(fromGyro))
                }
                _previousGyroAngle = _currentGyroAngle

                Log.d("Test", "from gyro: ${fromGyro} \n from internal ${fromAcc}")
                _angles.value = angles.value!! + _currentAngleUI.value!!

                //Log.d("Test","angle from acc:  ${fromAcc}")

            }     }  else{ Log.d("Test", "acc null") }
    }

    override fun startImuStream() {
        // Todo: implement
        if (linAccSensor == null) {
            Log.e(LOG_TAG, "Accelerometer sensor is not available on this device")
            return
        }
        if (_streamingLinAcc.value) {
            Log.e(LOG_TAG, "Accelerometer sensor is already streaming")
            return
        }
        // Register this class as a listener for gyroscope events
        sensorManager.registerListener(this, linAccSensor, SensorManager.SENSOR_DELAY_UI)

        // Start a coroutine to update the UI variable on a 2 Hz interval
        GlobalScope.launch(Dispatchers.Main) {
            _streamingLinAcc.value = true
            while (_streamingLinAcc.value) {
                // Update the UI variable
                _currentLinAccUI.update { _currentLinAcc }
                lastIndex = if (_angles.value!= null) _angles.value!!.lastIndex else 0
                computeAngle()
                delay(300)
            }
        }
    }
    fun computeAngle() {
        if(_currentLinAcc!=null && angles!=null){
            _currentAngleUI.update {
                90 - kotlin.math.atan(
                    _currentLinAcc!!.second.toDouble().pow(2.0) / kotlin.math.sqrt(
                        _currentLinAcc!!.first.toDouble().pow(2.0)
                                +_currentLinAcc!!.third.toDouble().pow(2.0)
                    )
                ) * 180 / kotlin.math.PI
            }
            if (_angles.value.isNotEmpty())
            {Log.d("Test","${lastIndex}")
                val newelem = 0.9 * _currentAngleUI.value!! +  0.1 * (_angles.value[lastIndex])
                _currentAngleUI.update{ newelem }
                _angles.value = angles.value + newelem}
            else{_angles.value =
                angles.value + 0.9 * _currentAngleUI.value!!}
            Log.d("Test","angle: ${currentAngleUI.value}" )
        }
        else{Log.d("Test", "acc null")}

    }

    override fun stopImuStream() {
        // Todo: implement
        if (_streamingLinAcc.value) {
            // Unregister the listener to stop receiving gyroscope events (automatically stops the coroutine as well)
            sensorManager.unregisterListener(this, linAccSensor)
            _streamingLinAcc.value = false
            _streamingGyro.value = false

        }
    }

    override fun stopGyroStream() {
        if (_streamingGyro.value) {
            // Unregister the listener to stop receiving gyroscope events (automatically stops the coroutine as well)
            sensorManager.unregisterListener(this, gyroSensor)
            _streamingGyro.value = false
        }
    }


    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            // Extract gyro data (angular speed around X, Y, and Z axes
            _currentGyro = Triple(event.values[0], event.values[1], event.values[2])
        }
        else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            // Extract gyro data (angular speed around X, Y, and Z axes
            _currentLinAcc = Triple(event.values[0], event.values[1], event.values[2])
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        // Not used in this example
    }

}