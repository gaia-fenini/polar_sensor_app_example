package mobappdev.example.sensorapplication.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/**
 * This repository provides a way to interact with the DataStore api,
 * with this API you can save key:value pairs
 *
 * Currently this file contains only one thing: getting the highscore as a flow
 * and writing to the highscore preference.
 * (a flow is like a waterpipe; if you put something different in the start,
 * the end automatically updates as long as the pipe is open)
 *
 * Date: 25-08-2023
 * Version: Skeleton code version 1.0
 * Author: Yeetivity
 *
 */
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
){
    private companion object {
        val HIGHSCORE = stringPreferencesKey("angles")
        const val TAG = "UserPreferencesRepo"
    }

    val highscore: Flow<List<List<Double>>?> = dataStore.data
        .catch {
            if (it is IOException) {
                Log.e(TAG, "Error reading preferences", it)
                emit(emptyPreferences())
            } else {
                throw it
            }
        }
        .map {preferences->
            val serializedAngles = preferences[HIGHSCORE]?:""
            if(serializedAngles.isNotEmpty()){
                serializedAngles.split(";")
                    .map { it.split(",") }
                    .map{angleStrings -> angleStrings.map { angle -> angle.toDouble() }}
            }
            else{
                emptyList()
            }
        }
    private fun serializeAngles(anglesList: List<List<Double>>): String {
        return anglesList.joinToString(";") { it.joinToString(",") }
    }

    private fun deserializeAngles(serializedAngles: String): List<List<Double>> {
        return serializedAngles.split(";").map { it.split(",").map { angle -> angle.toDouble() } }
    }
    suspend fun saveAngles(newList: List<Double>) {
        Log.d(TAG, "I'm saving $newList")
        dataStore.edit { preferences ->
            val currentList = preferences[HIGHSCORE]?.let { deserializeAngles(it) }?.toMutableList()
                ?: mutableListOf()
            currentList.add(newList)
            preferences[HIGHSCORE] = serializeAngles(currentList)
        }
    }
}