package mobappdev.example.sensorapplication.domain

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException

class CsvWriter (private val context: Context){
    fun writeCsv(fileName: String, dataList: List<Double>) {
        val filePath = getFilePath(fileName)
        Log.d("Test", filePath)
        try {
            val file = File(filePath)
            if (file.exists()) {
                // Delete the existing file
                if (!file.delete()) {
                    // Handle the case where file deletion fails
                    throw IOException("Failed to delete existing file: $filePath")
                }
            }
            // Create a new file
            if (!file.createNewFile()) {
                // Handle the case where file creation fails
                throw IOException("Failed to create a new file: $filePath")
            }

            FileOutputStream(filePath).use { writer ->
                for (number in dataList) {
                    writer.write((number.toString()+ ",").toByteArray())
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


    private fun getFilePath(fileName: String): String {

        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "sensorapplication"
        )

        if (!directory.exists()) {
            directory.mkdirs()
        }

        return "${directory.absolutePath}/$fileName.csv"
    }
}