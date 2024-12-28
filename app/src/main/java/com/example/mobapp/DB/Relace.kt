package com.example.mobapp.DB

import androidx.room.Embedded
import androidx.room.Relation

data class strankaSKotvami(
    @Embedded
    val stranka: DBStranka,
    @Relation(
        entity = DBKotva::class,
        parentColumn = "id",
        entityColumn = "strankaId"
    ) val kotvy: List<kotvaSHodnotami>,

    @Relation(
        parentColumn = "id",
        entityColumn = "strankaId"
    ) val hodnotyExtra: List<DBHodnotaExtra>
)

data class kotvaSHodnotami(
    @Embedded val kotva: DBKotva,
    @Relation(
        parentColumn = "id",
        entityColumn = "kotvaId"
    ) val hodnoty: List<DBHodnota>
)