package mobappdev.example.sensorapplication.ui.screens

/**
 * File: BluetoothDataScreen.kt
 * Purpose: Defines the UI of the data screen.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-07-11
 */

import android.content.ContentValues.TAG
import android.util.Log
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
import androidx.compose.material3.Snackbar
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
import com.polar.sdk.api.errors.PolarInvalidArgument
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
    val stateConnection = vm.stateConnection.collectAsState().value
    val isAcc = vm.isAcc.collectAsState().value
    val isAccNGyro = vm.isAccNGyro.collectAsState().value
    var errorMessage by remember { mutableStateOf<String?>(null)}
    var isFailed = vm.isFailed.collectAsState().value


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = CenterHorizontally
    ) {
        Text("Bluetooth")
        Text(text = if (stateConnection.connected) "connected to $deviceId" else if (stateConnection.connecting) "Connecting to $deviceId" else "disconnected")

        if (isFailed) {
            errorMessage?.let { errorMsg ->
                Snackbar(
                    action = {
                        Button(onClick = {
                            "BT required too much time to connect. Please control that your BT and sensor are on"

                        }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(errorMsg)
                }
            }
        }

        //Text(text = if (polar) "connected to $deviceId" else "disconnected")
        Box(
            contentAlignment = Center,
            modifier = Modifier.weight(1f)
        ) {
            Column() {
                Text(
                    text = if (stateConnection.measuring && (isAcc || isAccNGyro) ) "Acc: ${state.currentAcc?.first}, ${state.currentAcc?.second}, ${state.currentAcc?.third}" else "",
                    fontSize = 54.sp, //if (value.length < 3) 128.sp else 54.sp,
                    color = Color.Black,
                )

                Text(
                    text = if (stateConnection.measuring && isAccNGyro) "Gyro: ${state.currentGyro?.first}, ${state.currentGyro?.second}, ${state.currentGyro?.third}" else "",
                    fontSize = 54.sp,
                    color = Color.Black,
                )
                Text(text = "Angle: ${String.format("%.2f", angle)}",
                    style = MaterialTheme.typography.headlineSmall)


        }}
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){


            Button(
                onClick = {
                    try {
                        if (!stateConnection.connected) {
                            vm.connectToSensor()
                        } else {
                            vm.disconnectFromSensor()
                        }
                    } catch (polarInvalidArgument: PolarInvalidArgument) {
                        val attempt = if (stateConnection.connected) "disconnect" else "connect"
                        val errorMsg = "Failed to $attempt. Reason $polarInvalidArgument "
                        Log.e(TAG, errorMsg)
                        errorMessage = errorMsg
                    }
                },
                enabled = (!stateConnection.connecting),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = Color.Gray)
            ) {
                Text(text = if (!stateConnection.connected) {"Connect\n${deviceId}"} else {"Disconnect\n${deviceId}"})
            }


        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = {
                    if (!stateConnection.measuring && stateConnection.connected) {
                        vm.startAccPolar()
                        vm.setIsAcc(true)
                    } else {
                        vm.stopDataStream()
                        vm.setIsAcc(false)
                    }},
                enabled = (stateConnection.connected && !isAccNGyro),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text(text = if (!stateConnection.measuring) {"Start\nAcc Stream"} else {"Stop\nAcc Stream"})
            }
            Button(
                onClick = {
                    if (!stateConnection.measuring && stateConnection.connected) {
                        vm.startAccAndGyroStreaming()
                        vm.setIsAccNGyro(true)
                    } else {
                        vm.stopAccAndGyroStreaming()
                        vm.setIsAccNGyro(false)
                    }},
                enabled = (stateConnection.connected && !isAcc),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary)
            ) {
                Text(text = if (!stateConnection.measuring) {"Start\nGyro and Acc Stream"} else {"Stop\nGyro and Acc Stream"})
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth()
        ){
            Button(
                onClick = vm::writeCsvFile,
                enabled = (!stateConnection.measuring),
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