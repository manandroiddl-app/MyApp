package com.example.lifeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Transit_Last_Update")
data class TransitLastUpdateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "version")
    val version: String,

    @ColumnInfo(name = "last_update_time")
    val lastUpdateTime: Long
)
