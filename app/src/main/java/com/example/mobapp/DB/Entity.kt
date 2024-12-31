package com.example.mobapp.DB

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

abstract class DBEntita()

@Entity(tableName = "stranky")
data class DBStranka(
    val nazev: String,
    var datum: Date,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : DBEntita()

@Entity(tableName = "kotvy")
data class DBKotva(
    val strankaId: Long,
    val nazev: String,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : DBEntita()

@Entity(tableName = "hodnoty")
data class DBHodnota(
    val kotvaId: Long,
    val nazev: String,
    var hodnota: String,
    val typ: Int,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : DBEntita()

@Entity(tableName = "hodnoty_extra")
data class DBHodnotaExtra(
    val strankaId: Long,
    val nazev: String,
    var hodnota: String,
    val typ: Int,
    @PrimaryKey(autoGenerate = true) val id: Long = 0
) : DBEntita()