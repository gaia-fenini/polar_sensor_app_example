package mobappdev.example.sensorapplication.di

/**
 * File: AppModule.kt
 * Purpose: Defines the implementation of Dagger-Hilt injection.
 * Author: Jitse van Esch
 * Created: 2023-07-08
 * Last modified: 2023-09-21
 */

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import mobappdev.example.sensorapplication.data.AndroidPolarController
import mobappdev.example.sensorapplication.data.InternalSensorControllerImpl
import mobappdev.example.sensorapplication.data.UserPreferencesRepository
import mobappdev.example.sensorapplication.domain.InternalSensorController
import mobappdev.example.sensorapplication.domain.PolarController
import java.io.File
import java.util.prefs.Preferences
import javax.inject.Singleton

private const val USER_PREFERENCES = "user_preferences"
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun providePreferencesDataStore(@ApplicationContext appContext: Context): DataStore<androidx.datastore.preferences.core.Preferences> {
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler(
                produceNewData = { emptyPreferences() }
            ),
            migrations = listOf(SharedPreferencesMigration(appContext,USER_PREFERENCES)),
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            produceFile = { appContext.preferencesDataStoreFile(USER_PREFERENCES)}
        )
    }
    @Provides
    @Singleton
    fun providePolarController(@ApplicationContext context: Context): PolarController {
        return AndroidPolarController(context)
    }

    @Provides
    @Singleton
    fun provideInternalSensorController(@ApplicationContext context: Context): InternalSensorController {
        return InternalSensorControllerImpl(context)
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}