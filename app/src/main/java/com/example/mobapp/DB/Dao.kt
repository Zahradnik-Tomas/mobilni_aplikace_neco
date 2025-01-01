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
    @Query("SELECT * FROM stranky WHERE nazev LIKE :jako ORDER BY CASE WHEN :desc = 1 THEN datum END DESC, CASE WHEN :desc = 0 THEN datum END ASC")
    suspend fun vratVse(jako: String = "%", desc: Boolean = true): List<strankaSKotvami>


    @Transaction
    @Query("SELECT * FROM stranky WHERE datum BETWEEN :poDatu AND :predDatem AND nazev LIKE :jako ORDER BY CASE WHEN :desc = 1 THEN datum END DESC, CASE WHEN :desc = 0 THEN datum END ASC")
    suspend fun vratVseMeziDaty(
        poDatu: Date,
        predDatem: Date,
        jako: String = "%",
        desc: Boolean = true
    ): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE datum >= :poDatu AND nazev LIKE :jako ORDER BY CASE WHEN :desc = 1 THEN datum END DESC, CASE WHEN :desc = 0 THEN datum END ASC")
    suspend fun vratVsePoDatu(
        poDatu: Date,
        jako: String = "%",
        desc: Boolean = true
    ): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE datum <= :predDatem AND nazev LIKE :jako ORDER BY CASE WHEN :desc = 1 THEN datum END DESC, CASE WHEN :desc = 0 THEN datum END ASC")
    suspend fun vratVsePredDatem(
        predDatem: Date,
        jako: String = "%",
        desc: Boolean = true
    ): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE id IN (SELECT MAX(id) FROM stranky GROUP BY nazev)")
    suspend fun vratNejnovKostryStranek(): List<strankaSKotvami>

    @Transaction
    @Query("DELETE FROM sqlite_sequence")
    suspend fun smazSekvence()

}