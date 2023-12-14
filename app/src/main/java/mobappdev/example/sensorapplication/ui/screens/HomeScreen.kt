package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import mobappdev.example.sensorapplication.domain.BluetoothDevice
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.text.style.TextAlign

@Composable
fun HomeScreen(
    vm: DataVM,
    navController: NavController,
) {
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
    var bt by remember { mutableStateOf<Boolean>(false) }
    var scanning by remember { mutableStateOf<Boolean>(false) }

    val value: String = when (val combinedSensorData = vm.combinedDataFlow.collectAsState().value) {
        is CombinedSensorData.GyroData -> {
            val triple = combinedSensorData.gyro
            if (triple == null) {
                "-"
            } else {
                String.format("%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
            }

        }

        is CombinedSensorData.AccData -> {
            val triple = combinedSensorData.acc
            if (triple == null) {
                "-"
            } else {
                String.format("%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
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
        Text(
            text = "SENSOR APPLICATION",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(18.dp))

        Text(textAlign = TextAlign.Center,
            text = "Measure the Range Of Movement of your shoulder!",
            style = MaterialTheme.typography.headlineSmall
        )
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = CenterHorizontally
            ) {

                    Box(
                        contentAlignment = Center,
                        modifier = Modifier.weight(1f)
                    ) {
                        this@Column.AnimatedVisibility(
                            visible = (bt == true),
                            enter = fadeIn(
                                animationSpec = tween(
                                    300,
                                    easing = LinearOutSlowInEasing
                                )
                            ),
                            exit = fadeOut(
                                animationSpec = tween(
                                    300,
                                    easing = LinearOutSlowInEasing
                                )
                            ),

                            ) {
                        LazyColumn(modifier = Modifier) {
                            item {
                                Text(
                                    text = "Paired devices",
                                    style = MaterialTheme.typography.headlineMedium
                                )

                            }
                            items(state.pairedDevices) {

                                    device ->
                                if (device.name != null && device.name.contains("Polar")) {
                                    Text(text = device.name,
                                        modifier = Modifier.clickable {
                                            (vm::chooseSensor)(device.name.substring(12, 20));
                                            navController.navigate("bluetooth")
                                        })
                                }


                            }
                            item {
                                Text(
                                    text = "Scanned devices",
                                    style = MaterialTheme.typography.headlineMedium
                                )

                            }
                            items(state.scannedDevices) { device ->
                                if (device.name != null && device.name.contains("Polar")) {

                                    Text(text = device.name,
                                        modifier = Modifier.clickable {
                                            (vm::chooseSensor)(device.name.substring(12, 20));
                                            navController.navigate("bluetooth")
                                        })

                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Choose the sensor type:",
                    color = Color.Black,
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(modifier = Modifier.padding(8.dp),
                        onClick = { navController.navigate("Internal") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = "Use phone's sensors")
                    }
                    Button(modifier = Modifier.padding(8.dp),
                        onClick = {
                            if (!scanning) {
                            vm.startScan()
                            bt = true
                            scanning = true
                            } else {
                               vm.stopScan()
                               scanning = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = if (!scanning) "Search BT sensor" else "Stop BT scan")
                    }

                    Button(onClick = { navController.navigate("store") })
                    {Text("View History")}

                    /*Button(
                        onClick = { (vm::stopScan)() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = "Stop BT scan")
                    }*/

                }

            }
        }
    }
}
