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
    @Query("SELECT * FROM stranky WHERE id IN (SELECT MAX(id) FROM stranky WHERE datum BETWEEN :poDatu AND :predDatem GROUP BY nazev)")
    suspend fun vratNejnovKostryStranekMeziDaty(
        poDatu: Date,
        predDatem: Date
    ): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE id IN (SELECT MAX(id) FROM stranky WHERE datum >= :poDatu GROUP BY nazev)")
    suspend fun vratNejnovKostryStranekPoDatu(poDatu: Date): List<strankaSKotvami>

    @Transaction
    @Query("SELECT * FROM stranky WHERE id IN (SELECT MAX(id) FROM stranky WHERE datum <= :predDatem GROUP BY nazev)")
    suspend fun vratNejnovKostryStranekPredDatem(predDatem: Date): List<strankaSKotvami>

    @Transaction
    @Query("SELECT SUM(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratSUM(strankaNazev: String, kotvaNazev: String, hodnotaNazev: String): Long

    @Transaction
    @Query("SELECT SUM(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum BETWEEN :poDatu AND :predDatem AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratSUMMeziDaty(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        poDatu: Date,
        predDatem: Date
    ): Long

    @Transaction
    @Query("SELECT SUM(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum >= :poDatu AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratSUMPoDatu(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        poDatu: Date
    ): Long

    @Transaction
    @Query("SELECT SUM(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum <= :predDatem AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratSUMPredDatem(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        predDatem: Date
    ): Long

    @Transaction
    @Query("SELECT AVG(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratAVG(strankaNazev: String, kotvaNazev: String, hodnotaNazev: String): Long

    @Transaction
    @Query("SELECT AVG(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum BETWEEN :poDatu AND :predDatem AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratAVGMeziDaty(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        poDatu: Date,
        predDatem: Date
    ): Long

    @Transaction
    @Query("SELECT AVG(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum >= :poDatu AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratAVGPoDatu(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        poDatu: Date
    ): Long

    @Transaction
    @Query("SELECT AVG(CAST(hodnota as NUMERIC)) FROM stranky INNER JOIN kotvy ON stranky.id = kotvy.strankaId INNER JOIN hodnoty ON kotvy.id = hodnoty.kotvaId WHERE datum <= :predDatem AND stranky.nazev = :strankaNazev AND kotvy.nazev = :kotvaNazev AND hodnoty.nazev = :hodnotaNazev GROUP BY stranky.nazev")
    suspend fun vratAVGPredDatem(
        strankaNazev: String,
        kotvaNazev: String,
        hodnotaNazev: String,
        predDatem: Date
    ): Long

    /* RAW query? Nah, proc to mit jednoduche, kdyz mohu delat ctrl+c ctrl+v donekonecna */

    @Transaction
    @Query("DELETE FROM sqlite_sequence")
    suspend fun smazSekvence()

}