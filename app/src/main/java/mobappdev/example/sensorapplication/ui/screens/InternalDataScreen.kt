package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

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
import mobappdev.example.sensorapplication.ui.viewmodels.CombinedSensorData
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@Composable

fun InternalDataScreen(
    vm: DataVM, navController: NavController
) {
    //val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
    val combinedSensorData = vm.combinedDataFlow.collectAsState().value
    val angles = vm.angleDataFlow.collectAsState().value
    val gyro = vm.gyroDataFlow.collectAsState().value
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
        Text(text = "Internal Mode")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Column {
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

            }

        }
        if(combinedSensorData!=null){
            Text(text = "Angle: $angles")
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = vm::startAccInt,
               /* enabled = (!state.measuring),*/
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start\nAcc Stream")
            }
            Button(
                onClick = vm::startGyroInt,
               /* enabled = (!state.measuring),*/
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Start\nGyro Stream")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {(vm::stopDataStream)()},
                /*enabled = (state.measuring),*/
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text(text = "Stop\nstream")
            }
            Button(
                onClick = {(vm::writeCsvFile)()},
                /*enabled = (state.measuring),*/
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