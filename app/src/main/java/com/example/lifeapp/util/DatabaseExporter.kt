package com.example.lifeapp.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseExporter {

    private const val TAG = "DatabaseExporter"
    private const val DB_NAME = "life_app_database" // ⚠️ 請確認與你的 RoomDatabase 名稱一致

    fun exportDatabaseToExternalFiles(context: Context) {
        try {
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                showToast(context, "❌ 匯出失敗：Room 資料庫檔案尚未建立")
                return
            }

            // 目標資料夾: Android/data/com.example.lifeapp/files/db_backup/
            val targetDir = File(context.getExternalFilesDir(null), "db_backup")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val targetDbFile = File(targetDir, "life_app_test.db")
            copyFile(dbFile, targetDbFile)

            // 複製 SQLite WAL / SHM 暫存檔（若存在）
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) copyFile(walFile, File(targetDir, "life_app_test.db-wal"))

            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) copyFile(shmFile, File(targetDir, "life_app_test.db-shm"))

            val msg = "✅ DB 已匯出至 Android/data/.../files/db_backup/"
            Log.d(TAG, msg)
            showToast(context, msg)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 匯出失敗", e)
            showToast(context, "❌ 匯出失敗: ${e.localizedMessage}")
        }
    }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun showToast(context: Context, message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
