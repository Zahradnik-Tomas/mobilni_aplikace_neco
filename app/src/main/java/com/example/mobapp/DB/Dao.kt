package com.example.mobapp.DB

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.util.Date

@Dao
interface StrankaDao {
    @Upsert
    suspend fun insertStranka(stranka: DBStranka)

    @Upsert
    suspend fun insertKotva(kotva: DBKotva)

    @Upsert
    suspend fun insertHodnota(vararg hodnota: DBHodnota)

    @Upsert
    suspend fun insertHodnotaExtra(vararg hodnota: DBHodnotaExtra)

    @Query("SELECT * FROM stranky")
    fun vratVse(): List<strankaSKotvami>

    @Query("SELECT * FROM stranky WHERE datum BETWEEN :poDatu AND :predDatem")
    fun vratVseMeziDaty(poDatu: Date, predDatem: Date): List<strankaSKotvami>


}