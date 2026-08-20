package com.example.lifeapp.util

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import com.example.lifeapp.data.local.AppDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DbExportHelper {

    suspend fun exportDatabaseToExternalStorage(context: Context, database: AppDatabase): String {
        return try {
            // 1. 強制把 Room 的 WAL 數據 checkpoint 寫回主 .db 檔
            database.openHelper.writableDatabase.query(
                SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL);")
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    Log.d("DbExport", "WAL Checkpoint result: ${cursor.getInt(0)}")
                }
            }

            // 2. 取得原 Room 數據庫檔案路徑
            val dbName = "lifeapp_database"
            val dbFile = context.getDatabasePath(dbName)

            if (!dbFile.exists()) {
                return "失敗：找不到數據庫檔案 $dbName"
            }

            // 3. 定義外置儲存路徑：Android/data/com.example.lifeapp/files/
            val exportDir = context.getExternalFilesDir(null) ?: return "失敗：無法存取外部儲存空間"
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val exportFile = File(exportDir, "lifeapp_database_export.db")

            // 4. 複製檔案
            FileInputStream(dbFile).use { input ->
                FileOutputStream(exportFile).use { output ->
                    input.copyTo(output)
                }
            }

            Log.d("DbExport", "DB 成功導出至: ${exportFile.absolutePath}")
            "成功導出至:\n${exportFile.absolutePath}"
        } catch (e: Exception) {
            Log.e("DbExport", "Export DB error", e)
            "導出失敗: ${e.localizedMessage}"
        }
    }
}
