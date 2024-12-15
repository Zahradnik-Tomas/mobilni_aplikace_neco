package com.example.mobapp

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
class Stranka(var nazev: String, var kotvy: Array<Kotva>) {
    @Transient
    var bod: Array<android.graphics.Point>? = null
}

@Serializable
class Kotva(var nazev: String, var hodnoty: Array<Hodnota>) {
    @Transient
    var lastSeen = 0

    @Transient
    var bod: Array<android.graphics.Point>? = null
}

@Serializable
class Hodnota(var nazev: String, var typ: Int = 0) {
    @Transient
    var bod: Array<android.graphics.Point>? = null

    var hodnota =
        "0" //TODO def hodnota podle typu.... Jedinacek.ListTypu[typ].VratDefHodnotu <- typ je z interfacu

    @Transient
    var lastSeen = 0

    @Transient
    var confidence = 0.0f

    @Transient
    var hodnotaBod: Array<android.graphics.Point>? = null
}