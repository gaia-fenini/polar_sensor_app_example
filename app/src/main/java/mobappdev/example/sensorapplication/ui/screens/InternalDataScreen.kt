package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mobappdev.example.sensorapplication.R

@Composable

fun InternalDataScreen(
    vm: DataVM, navController: NavController
) {
    //val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
    val combinedSensorData = vm.combinedDataFlow.collectAsState().value
    val angles = vm.angleDataFlow.collectAsState().value
    val angleList by rememberUpdatedState(vm.anglesList.collectAsState())
    val gyro = vm.gyroDataFlow.collectAsState().value
    var accStream by remember { mutableStateOf(false) }
    var accGyroStream by remember { mutableStateOf(false) }

    var valueGyro: String = ""
    if( gyro!=null ) {
        valueGyro=
            String.format("Gyro:%.1f, %.1f, %.1f", gyro.first, gyro.second, gyro.third)
    }

    val value: String = when (combinedSensorData) {

        is CombinedSensorData.GyroData -> {
            val triple = combinedSensorData.gyro
            if (triple == null) {
                "-"
            } else {
                String.format("Gyro:%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
            }
        }

        is CombinedSensorData.AccData -> {
            val triple = combinedSensorData.acc
            if (triple == null) {
                "-"
            } else {
                String.format("Acc:%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
            }
        }
         else -> "-"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(text = "INTERNAL MODE",
        style = MaterialTheme.typography.headlineMedium)
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            /*Column {
                Text(
                    text = value,
                    fontSize = if (value.length < 3) 128.sp else 54.sp,
                    color = Color.Black,
                )
                Text(
                    text = valueGyro,
                    fontSize = if (value.length < 3) 128.sp else 54.sp,
                    color = Color.Black,
                )

            }*/

            //RealTimeGraph(vm = vm)
            //if (!accStream && !accGyroStream && angleList.value!!.isNotEmpty()) {
                for (value in angleList.value!!) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            val chart = LineChart(context)  // Initialise the chart
                            val white = Color.White.hashCode()
                            chart.setBackgroundColor(white)
                            val entries: List<Entry> =
                                angleList.value!!.indices.map { it.toFloat() }
                                    .zip(angleList.value!!.map { it.toFloat() }) { x, y ->
                                        Entry(x, y)
                                    }  // Convert the x and y data into entries
                            Log.d("Test", "entries: $entries")
                            val dataSet = LineDataSet(entries, "label").apply {
                            }  // Create a dataset of entries
                            dataSet.addEntry(entries[entries.lastIndex])
                            val linedata = LineData(dataSet).apply {
                                setDrawValues(true)
                                isHighlightEnabled = true
                                setValueTypeface(Typeface.DEFAULT_BOLD)
                                setValueTextSize(0f)
                            }  // Pass the dataset to the chart
                            // Enable touch gestures
                            chart.data = linedata
                            chart.setTouchEnabled(true)
                            chart.isDragEnabled = true
                            chart.isScaleXEnabled = true
                            chart.isScaleYEnabled = false
                            // Refresh and return the chart
                            val yAxis = chart.axisLeft
                            yAxis.axisMaximum = 100f
                            yAxis.axisMinimum = 0f

                            val rightAxis = chart.axisRight
                            rightAxis.isEnabled = false
                            chart.description.isEnabled = false

                            dataSet.notifyDataSetChanged()
                            linedata.notifyDataChanged()
                            chart.notifyDataSetChanged()
                            //chart.animateXY(3000, 3000);
                            chart.invalidate()
                            chart
                        }
                    )
                }
            //}


        }

        Text(text = "Angle: ${String.format("%.2f", angles)}",
            style = MaterialTheme.typography.headlineSmall)


        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {
                    if (!accStream) {
                        vm.startAccInt()
                        accStream = true
                    } else {
                        vm.stopDataStream()
                        accStream = false
                    }
                },
                enabled = (!accGyroStream),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = if (!accStream) "Start\nAcc Stream" else "Stop\nAcc Stream",
                textAlign = TextAlign.Center)
            }
            Button(
                onClick = {
                    if (!accGyroStream) {
                        vm.startGyroInt()
                        accGyroStream = true
                    } else {
                        vm.stopDataStream()
                        accGyroStream = false
                    }
                },
                enabled = (!accStream),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = if (!accGyroStream) "Start Acc\nand Gyro Stream" else "Stop Acc\nand Gyro Stream",
                    textAlign = TextAlign.Center)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            /*Button(
                onClick = {(vm::stopDataStream)()},
                /*enabled = (state.measuring),*/
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Stop\nstream")
            }*/
            Button(
                onClick = {(vm::writeCsvFile)()},
                enabled = (!accStream && !accGyroStream),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Export data")
            }
        }
    }
}
/*
class RealTimeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val vm: DataVM
) : View(context, attrs, defStyleAttr) {

    private val graphColor = Color.Blue
    private val linePaint = Paint().apply {
        color = 1
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private var angle = vm.angleDataFlow.value
    private var angleList = emptyList<Double?>()

     override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas?.let {
            val width = it.width.toFloat()
            val height = it.height.toFloat()

            if (angleList.size > 1) {
                val deltaX = width / (angleList.size - 1)
                val scaleY = height / angleList.maxOrNull()!!

                val path = android.graphics.Path()
                path.moveTo(0f, height - angleList.first() * scaleY)

                for (i in 1 until angleList.size) {
                    path.lineTo(i * deltaX, height - angleList[i] * scaleY)
                }

                it.drawPath(path, linePaint)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimeGraph(vm: DataVM) {
    var isUpdating by remember { mutableStateOf(false) }

    // Observe changes in the angleDataFlow and trigger recomposition
    val angles by rememberUpdatedState(newValue = vm.angleDataFlow.collectAsState().value)
    var anglesList = emptyList<Double?>()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Gray)
        ) {
            RealTimeGraphView(angles, anglesList, isUpdating)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = if (isUpdating) "Updating..." else "Stopped",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
        }
    }
}

@Composable
fun RealTimeGraphView(angles: Double?, anglesList: List<Double?>, isUpdating: Boolean) {
    //val context = LocalContext.current

    var anglesList = anglesList 
    AndroidView(
        factory = { ctx ->
            RealTimeGraphView(ctx)
        },
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ).apply {
        DisposableEffect(this) {
            onDispose {
                // Clean up if needed
            }
        }

        LaunchedEffect(isUpdating) {
            while (isUpdating) {
                anglesList = (anglesList + angles)!!
                delay(500) // Adjust the delay as needed
            }
        }
    }
}

 */