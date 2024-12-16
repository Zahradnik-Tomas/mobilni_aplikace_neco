package com.example.mobapp

import kotlin.math.pow
import kotlin.math.sqrt

class FunkceCiste {
    companion object {
        public fun PodobnostStringu(str1: String, str2: String): Double {
            var par1 = RozparujString(str1.uppercase())
            var par2 = RozparujString(str2.uppercase())
            return PodobnostStringu(par1, par2)
        }

        public fun PodobnostStringu(par1: ArrayList<String>, str2: String): Double {
            var par2 = RozparujString(str2.uppercase())
            return PodobnostStringu(par1, par2)
        }

        public fun PodobnostStringu(par1: ArrayList<String>, Par2: ArrayList<String>): Double {
            var par2 = ArrayList<String>(Par2)
            var intersekce = 0
            var unie = par1.size + par2.size
            for (i in par1.indices) {
                val temp = par1.get(i)
                for (j in par2.indices) {
                    val temp2 = par2.get(j)
                    if (temp.equals(temp2)) {
                        intersekce += 1
                        par2.removeAt(j)
                        break
                    }
                }
            }
            return (2.0 * intersekce) / unie
        }

        public fun RozparujString(str: String): ArrayList<String> {
            val pary = ArrayList<String>()
            val slova = str.split("\\s")
            for (i in slova.indices) {
                for (par in ParyVeSlove(slova[i])) {
                    pary.add(par)
                }
            }
            return pary
        }

        public fun VratMozneStranky(
            bloky: List<com.google.mlkit.vision.text.Text.TextBlock>,
            Stranky: Array<Stranka>
        ): Array<Stranka> {
            if (Stranky.isEmpty()) {
                throw IllegalArgumentException("Stranky nesmi byt prazdne")
            }
            if (Stranky.size == 1) {
                return Stranky
            }
            val listKVraceni = ArrayList<Stranka>()
            val listVhodnosti = Array(Stranky.size) { 0 }
            for (block in bloky) {
                for (line in block.lines) {
                    for (stranka in Stranky.indices) {
                        if (PatriLinkaStrance(line, Stranky[stranka])) {
                            listVhodnosti[stranka] += 1
                        }
                    }
                }
            }
            for (index in VratVhodneIndexy(listVhodnosti)) {
                listKVraceni.add(Stranky[index])
            }
            return listKVraceni.toTypedArray()
        }

        public fun VzdalenostBodu(
            bod1: android.graphics.Point,
            bod2: android.graphics.Point
        ): Double {
            return sqrt((bod1.x - bod2.x).toDouble().pow(2) + (bod1.y - bod2.y).toDouble().pow(2))
        }

        private fun ParyVeSlove(str: String): Array<String> {
            val pocetParu = str.length - 1
            val pary = Array<String>(pocetParu) { "" }
            for (i in 0..<pocetParu) {
                pary[i] = str.substring(i, i + 2)
            }
            return pary
        }

        private fun PatriLinkaStrance(
            line: com.google.mlkit.vision.text.Text.Line,
            stranka: Stranka
        ): Boolean {
            val MIN_PODOBNOST = 0.8
            val rozpLinka = RozparujString(line.text.uppercase())
            if (PodobnostStringu(rozpLinka, stranka.nazev) > MIN_PODOBNOST) {
                return true
            }
            for (kotva in stranka.kotvy) {
                if (PodobnostStringu(rozpLinka, kotva.nazev) > MIN_PODOBNOST) {
                    return true
                }
                for (hodnota in kotva.hodnoty) {
                    if (PodobnostStringu(rozpLinka, hodnota.nazev) > MIN_PODOBNOST) {
                        return true
                    }
                }
            }
            return false
        }

        private fun VratVhodneIndexy(listVhodnosti: Array<Int>): Array<Int> {
            if (listVhodnosti.size < 2) {
                return listVhodnosti
            }
            val tempList = ArrayList<Int>()
            val maxVhodnost = listVhodnosti.max()
            for (cislo in 0..<listVhodnosti.size) {
                if (maxVhodnost * 0.8 < listVhodnosti[cislo]) {
                    tempList.add(cislo)
                }
            }
            return tempList.toTypedArray()
        }
    }
}