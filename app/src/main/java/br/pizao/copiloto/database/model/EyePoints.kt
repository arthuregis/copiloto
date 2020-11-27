package br.pizao.copiloto.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "left_eye_table")
data class LeftEye(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    @ColumnInfo
    val time: Long,
    @ColumnInfo
    val index: Int,
    @ColumnInfo
    val x: Float,
    @ColumnInfo
    val y: Float,
    @ColumnInfo
    val anglex: Float,
    @ColumnInfo
    val angley: Float,
    @ColumnInfo
    val anglez: Float
)

@Entity(tableName = "right_eye_table")
data class RightEye(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L,
    @ColumnInfo
    val time: Long,
    @ColumnInfo
    val index: Int,
    @ColumnInfo
    val x: Float,
    @ColumnInfo
    val y: Float,
    @ColumnInfo
    val anglex: Float,
    @ColumnInfo
    val angley: Float,
    @ColumnInfo
    val anglez: Float
)


