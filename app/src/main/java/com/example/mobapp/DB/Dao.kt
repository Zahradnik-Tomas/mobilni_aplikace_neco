package com.example.mobapp.DB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import java.util.Date

@Dao
interface StrankaDao {
    @Upsert
    suspend fun insertStranka(stranka: DBStranka): Long

    @Upsert
    suspend fun insertKotva(kotva: DBKotva): Long

    @Upsert
    suspend fun insertHodnota(vararg hodnota: DBHodnota)

    @Upsert
    suspend fun insertHodnotaExtra(vararg hodnota: DBHodnotaExtra)

    @Delete
    suspend fun delete(entita: DBStranka)

    @Delete
    suspend fun delete(entita: DBKotva)

    @Delete
    suspend fun delete(entita: DBHodnota)

    @Delete
    suspend fun delete(entita: DBHodnotaExtra)

    @Transaction
    @Query("SELECT * FROM stranky ORDER BY datum DESC")
    suspend fun vratVse(): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE datum BETWEEN :poDatu AND :predDatem ORDER BY datum DESC")
    suspend fun vratVseMeziDaty(poDatu: Date, predDatem: Date): List<strankaSKotvami>

    @Transaction
    @Query("DELETE FROM sqlite_sequence")
    suspend fun smazSekvence()

}