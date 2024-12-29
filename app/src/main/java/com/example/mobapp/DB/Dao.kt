package com.example.mobapp.DB

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.util.Date

@Dao
interface StrankaDao {
    @Upsert
    fun insertStranka(stranka: DBStranka): Long

    @Upsert
    fun insertKotva(kotva: DBKotva): Long

    @Upsert
    fun insertHodnota(vararg hodnota: DBHodnota)

    @Upsert
    fun insertHodnotaExtra(vararg hodnota: DBHodnotaExtra)

    @Transaction
    @Query("SELECT * FROM stranky")
    fun vratVse(): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE datum BETWEEN :poDatu AND :predDatem")
    fun vratVseMeziDaty(poDatu: Date, predDatem: Date): List<strankaSKotvami>


}