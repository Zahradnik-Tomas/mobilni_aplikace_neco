package com.example.mobapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class Stranka(
    var nazev: String,
    var kotvy: Array<Kotva>,
    var extraHodnoty: Array<Hodnota> = emptyArray<Hodnota>(),
    var datum: String = "",
    var vypocty: Array<Vypocet> = emptyArray<Vypocet>()
)

@Serializable
data class Kotva(
    var nazev: String, var hodnoty: Array<Hodnota>,

    @Transient
    var bod: Array<android.graphics.Point>? = null,

    @Transient
    var predPodob: Double? = null
)

@Serializable
data class Hodnota(
    var nazev: String, var typ: Typy = Typy.CISLO,

    @Transient
    var bod: Array<android.graphics.Point>? = null,

    @Transient
    var bodPred: Array<android.graphics.Point>? = null,
    var hodnota: String = "",

    @Transient
    var confidence: Float = 0.0f,

    @Transient
    var vzdalenost: Double? = null,

    @Transient
    var predPodob: Double? = null
)

@Serializable
data class Vypocet(
    var nazevVysledku: String,
    var nasobitele: Array<Nasobitel> = emptyArray<Nasobitel>(),
    var pricitaci: Array<Pricitac> = emptyArray<Pricitac>()
)

@Serializable
data class Nasobitel(
    var kotvaNazev: String,
    var hodnotaNazev: String,
    var nasobic: Double,
    var nazevVypoctu: String
)

@Serializable
data class Pricitac(
    var pricteno: Double,
    var nazevVypoctu: String,
    var nazevMultiplikatoru: String
)