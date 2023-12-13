package mobappdev.example.sensorapplication.domain

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.IOException

class CsvWriter (private val context: Context){
    fun writeCsv(fileName: String, dataList: List<Double>) {
        val filePath = getFilePath(fileName)

        try {
            FileWriter(filePath).use { writer ->
                for (number in dataList) {
                    writer.append(number.toString())
                    writer.append(',')
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