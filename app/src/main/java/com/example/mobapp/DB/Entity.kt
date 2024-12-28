package com.example.mobapp.DB

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "stranky")
data class DBStranka(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val nazev: String,
    val datum: Date
)

@Entity(tableName = "kotvy")
data class DBKotva(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val strankaId: Int,
    val nazev: String
)

@Entity(tableName = "hodnoty")
data class DBHodnota(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val kotvaId: Int,
    val nazev: String,
    val hodnota: String,
    val typ: Int
)

@Entity(tableName = "hodnoty_extra")
data class DBHodnotaExtra(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val strankaId: Int,
    val nazev: String,
    val hodnota: String,
    val typ: Int
)