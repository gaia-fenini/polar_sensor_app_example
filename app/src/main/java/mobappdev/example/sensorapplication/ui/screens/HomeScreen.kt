package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

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

@Composable
fun HomeScreen(
    vm: DataVM,
    navController: NavController,

) {
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value

    val value: String = when (val combinedSensorData = vm.combinedDataFlow.collectAsState().value) {
        is CombinedSensorData.GyroData -> {
            val triple = combinedSensorData.gyro
            if (triple == null) {
                "-"
            } else {
                String.format("%.1f, %.1f, %.1f", triple.first, triple.second, triple.third)
            }

        }
        is CombinedSensorData.HrData -> combinedSensorData.hr.toString()
        else -> "-"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text(text = "SENSOR APPLICATION")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Column {
                Text(
                    text = "Choose the sensor type:",
                    color = Color.Black,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { navController.navigate("Internal") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = "Internal")
                    }
                    Button(
                        onClick = { (vm::startScan)() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = "Polar")
                    }
                    Button(
                        onClick = { (vm::stopScan)() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(text = "Stop scan")
                    }

                }
                Box(contentAlignment = Center,
                    modifier = Modifier.weight(1f)){

                    LazyColumn(modifier = Modifier){
                        item{
                            Text(text = "Paired devices")

                        }
                        items(state.pairedDevices){

                                device->
                            if (device.name!=null && device.name.contains("Polar")) {
                                Text(text = device.name,
                                    modifier = Modifier.clickable { (vm::chooseSensor)(device.name.substring(12,20));
                                                 navController.navigate("BluetoothDataScreen")
                                    })
                            }



                        }
                        item{
                            Text(text = "scanned devices")

                        }
                        items(state.scannedDevices){
                                device->
                            if (device.name != null && device.name.contains("Polar")) {

                                Text(text = device.name.substring(12,20),
                                    modifier = Modifier.clickable{(vm::chooseSensor)(device.name.substring(12,20));
                                        navController.navigate("bluetooth")})

                            }}
                    }
                }

            }
        }
    }
}
