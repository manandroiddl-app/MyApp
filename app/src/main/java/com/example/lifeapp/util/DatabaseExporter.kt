package com.example.lifeapp.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.example.lifeapp.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseExporter {

    private const val TAG = "DatabaseExporter"
    private const val DB_NAME = "lifeapp_database" // ⚠️ 精準對齊 DatabaseModule.kt 中的名稱

    /**
     * 🚀 將 Room 資料庫匯出至 `Android/data/com.example.lifeapp/files/db_backup/`
     * 支援 SQLite WAL 模式，確保數據完整寫入並可被 DB Browser for SQLite 開啟
     */
    fun exportDatabaseToExternalFiles(context: Context, database: AppDatabase? = null) {
        try {
            // 1. 強制觸發 Room 建立實體 SQLite 檔案 (若尚未初始化)
            database?.openHelper?.writableDatabase

            // 2. 取得 Room 原始資料庫檔案
            val dbFile = context.getDatabasePath(DB_NAME)
            if (!dbFile.exists()) {
                showToast(context, "❌ 匯出失敗：資料庫檔案尚未建立，請先點擊「下載數據」！")
                return
            }

            // 3. 設定目標目錄: Android/data/com.example.lifeapp/files/db_backup/
            val targetDir = File(context.getExternalFilesDir(null), "db_backup")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // 4. 複製主 DB 檔
            val targetDbFile = File(targetDir, "$DB_NAME.db")
            copyFile(dbFile, targetDbFile)

            // 5. 複製 WAL 與 SHM 暫存檔（SQLite WAL 模式必備）
            val walFile = File(dbFile.path + "-wal")
            if (walFile.exists()) {
                copyFile(walFile, File(targetDir, "$DB_NAME.db-wal"))
            }

            val shmFile = File(dbFile.path + "-shm")
            if (shmFile.exists()) {
                copyFile(shmFile, File(targetDir, "$DB_NAME.db-shm"))
            }

            val journalFile = File(dbFile.path + "-journal")
            if (journalFile.exists()) {
                copyFile(journalFile, File(targetDir, "$DB_NAME.db-journal"))
            }

            val successMsg = "✅ 已成功匯出至 Android/data/.../files/db_backup/$DB_NAME.db"
            Log.d(TAG, successMsg)
            showToast(context, successMsg)

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
