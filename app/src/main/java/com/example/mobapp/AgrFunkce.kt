package com.example.mobapp

enum class AgrFunkce(val kompTypy: List<Typy>, val nazev: String) {
    SUM(listOf(Typy.CISLO, Typy.DECIMAL), "SUM"),
    AVG(listOf(Typy.CISLO, Typy.DECIMAL, Typy.PROCENTO), "AVG")
}