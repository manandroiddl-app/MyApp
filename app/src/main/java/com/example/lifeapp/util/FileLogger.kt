package com.example.lifeapp.util

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val logFileName = "sync_debug.log"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    private fun getLogFile(): File {
        val dir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(dir, logFileName)
    }

    /**
     * 清空舊有的 Log 檔案 (通常在發起全新 Sync 時呼叫)
     */
    fun clearLog() {
        runCatching {
            val file = getLogFile()
            if (file.exists()) {
                file.writeText("")
            }
        }
    }

    /**
     * 追加一條帶有時間戳記的 Log 紀錄至檔案
     */
    fun log(message: String) {
        val timestamp = dateFormat.format(Date())
        val formattedMessage = "[$timestamp] $message\n"
        
        // 同步印至 Android Standard Logcat
        Log.d("FileLogger", message)

        runCatching {
            val file = getLogFile()
            FileWriter(file, true).use { writer ->
                writer.append(formattedMessage)
            }
        }
    }

    /**
     * 取得目前 Log 檔案的完整絕對路徑
     */
    fun getLogFilePath(): String {
        return getLogFile().absolutePath
    }
}
