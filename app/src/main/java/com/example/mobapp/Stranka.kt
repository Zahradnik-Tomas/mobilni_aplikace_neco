package com.example.mobapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Stranka(
    var nazev: String,
    var kotvy: Array<Kotva>,
    var extraHodnoty: Array<Hodnota> = emptyArray<Hodnota>()
) {
    var datum = ""
}

@Serializable
class Kotva(var nazev: String, var hodnoty: Array<Hodnota>) {
    @Transient
    var bod: Array<android.graphics.Point>? = null
}

@Serializable
class Hodnota(var nazev: String, var typ: Typy = Typy.CISLO) {
    @Transient
    var bod: Array<android.graphics.Point>? = null

    @Transient
    var bodPred: Array<android.graphics.Point>? = null

    var hodnota = ""

    @Transient
    var confidence = 0.0f

    @Transient
    var vzdalenost: Double? = null
}