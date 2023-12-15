package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.graphics.Typeface
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import mobappdev.example.sensorapplication.domain.BluetoothDevice
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM
import java.util.Locale

@Composable
fun Store(
    vm: DataVM,
    navController: NavController,
) {
    val oldValues by vm.highscore.collectAsState()  //Lista di liste

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(text = "History:",
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp) ,

            fontWeight = FontWeight.Bold,
            fontSize = 30.sp, // Adjust the font size as needed
        )
        LazyColumn {
            items(oldValues!!) { doubleList ->
                DoubleListRow(vm, doubleList)
                Spacer(modifier = Modifier.height(10.dp))
            }

        }
    }
}


@Composable
fun DoubleListRow(vm: DataVM,doubleList: List<Double>) {
    var expanded by remember { mutableStateOf(false) }

    Box( modifier = Modifier
        .fillMaxWidth()
        .height(if (expanded) 200.dp else 40.dp)
        .background(Color.White)
        .clickable { expanded = !expanded }) {
        // You can customize the appearance of each row here
        Column {
            Row {
                Button(onClick = { (vm::export)(doubleList) }) {
                    Text("Export Data")}
                LazyRow {
                    // Display each double in the list
                    items(doubleList) { doubleValue ->
                        // You can customize the appearance of each double value here
                        // For example, you can use Text or any other Compose components
                        val formattedValue = String.format(Locale.getDefault(), "%.2f", doubleValue)
                        Text(
                            text = "$formattedValue",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        val chart = LineChart(context)  // Initialise the chart
                        val entries: List<Entry> =
                            doubleList!!.indices.map { it.toFloat() }
                                .zip(doubleList.map { it.toFloat() }) { x, y ->
                                    Entry(
                                        x, y
                                    )
                                }  // Convert the x and y data into entries
                        Log.d("Test","entries: $entries")
                        val dataSet = LineDataSet(entries, "Angles(°)").apply {
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
                        chart.description.isEnabled = false

                        // Refresh and return the chart

                        dataSet.notifyDataSetChanged()
                        linedata.notifyDataChanged()
                        chart.notifyDataSetChanged()
                        chart.animateXY(1000, 1000);
                        chart.invalidate()
                        chart
                    }
                )
            }
        }
    }
}