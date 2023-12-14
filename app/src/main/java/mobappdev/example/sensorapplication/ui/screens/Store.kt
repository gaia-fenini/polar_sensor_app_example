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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        Text(text = "Store:",
            modifier = Modifier.fillMaxWidth()
                .padding(16.dp) )
        LazyColumn {
            items(oldValues!!) { doubleList ->
                DoubleListRow(vm, doubleList)
            }

        }
    }
}


@Composable
fun DoubleListRow(vm: DataVM,doubleList: List<Double>) {
    // You can customize the appearance of each row here
    LazyRow {
        // Display each double in the list
        item{Button(onClick = {(vm::export)(doubleList)}){
            Text("export")}}
        items(doubleList) { doubleValue ->
            // You can customize the appearance of each double value here
            // For example, you can use Text or any other Compose components
            Text(
                text = "$doubleValue",
                modifier = Modifier.padding(8.dp)
            )

        }

    }
}