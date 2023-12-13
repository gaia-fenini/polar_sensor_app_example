package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import mobappdev.example.sensorapplication.ui.viewmodels.DataUiState
import mobappdev.example.sensorapplication.ui.viewmodels.DataVM

@Composable
fun BluetoothDataScreen(
    vm: DataVM,navController: NavController
) {
    val state = vm.state.collectAsStateWithLifecycle().value
    val deviceId = vm.deviceId.collectAsStateWithLifecycle().value
    val angle = vm.angleDataFlowPolar.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text("Bluetooth")
        Text(text = if (state.connected) "connected to $deviceId" else "disconnected")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Column() {
                Text(
                    text = if (state.measuring) "Acc: ${state.currentAcc?.first}, ${state.currentAcc?.second}, ${state.currentAcc?.third}" else "-",
                    fontSize = 54.sp, //if (value.length < 3) 128.sp else 54.sp,
                    color = Color.Black,
                )

                Text(
                    text = if (state.measuring) "Gyro: ${state.currentGyro?.first}, ${state.currentGyro?.second}, ${state.currentGyro?.third}" else "-",
                    fontSize = 54.sp,
                    color = Color.Black,
                )
                Text(text = "Angle: $angle")

                this@Column.AnimatedVisibility(
                visible = (state.connecting),
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
                ),){
                Text("Polar is connecting...")
            }

        }}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = if (!state.connected) vm::connectToSensor else vm::disconnectFromSensor,

                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)

            ) {
                Text(text = if (!state.connected) {"Connect\n${deviceId}"} else {"Disconnect\n${deviceId}"})
            }

        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = if (!state.measuring && state.connected) vm::startAccPolar else vm::stopDataStream,
                enabled = (state.connected),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text(text = if (!state.measuring) {"Start\nAcc Stream"} else {"Stop\nAcc Stream"})
            }
            Button(
                onClick = if (!state.measuring && state.connected) vm::startGyroPolar else vm::stopDataStream,
                enabled = (state.connected),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text(text = if (!state.measuring) {"Start\nGyro and Acc Stream"} else {"Stop\nGyro and Acc Stream"})
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = vm::writeCsvFile,
                enabled = (state.measuring),
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