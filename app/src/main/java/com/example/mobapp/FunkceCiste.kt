package com.example.mobapp

import android.graphics.Point
import kotlin.jvm.Throws
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

        /* Zkontroluje, zda bod patri kotve a vrati jeji index, pripadne vrati index mozne predchozi/nasledujici kotvy */
        public fun VratPatriciKotvu(bod: Point, kotva: Int, kotvy: Array<Kotva>): Int {
            if (kotvy[kotva].bod == null) {
                throw IllegalArgumentException("Kotva ma null bod")
            }
            val vzdalenostDolu = VzdalenostBodu(kotvy[kotva].bod!![3], bod)
            if (VzdalenostBodu(kotvy[kotva].bod!![0], bod) < vzdalenostDolu) {
                if (kotva != 0) {
                    return kotva - 1
                }
                return -1
            } else if (kotvy.size - 1 > kotva && kotvy[kotva].bod != null) {
                val temp = VzdalenostBodu(kotvy[kotva + 1].bod!![3], bod)
                if (temp < vzdalenostDolu && VzdalenostBodu(
                        kotvy[kotva + 1].bod!![0],
                        bod
                    ) > temp
                ) {
                    return kotva + 1
                }
            }
            return kotva
        }

        /* Chci aby to bylo ciste, proto to cache */
        public fun VratMoznouKotvu(
            stranka: Stranka,
            linky: ArrayList<com.google.mlkit.vision.text.Text.Line>,
            cacheHodnot: java.util.ArrayList<ArrayList<ArrayList<String>>>
        ): Kotva? {
            if (stranka.kotvy.isEmpty()) {
                return null
            }
            val body = Array(stranka.kotvy.size) { 0 }
            for (line in linky) {
                val temp = RozparujString(line.text)
                for (kotva in stranka.kotvy.indices) {
                    for (hodnota in stranka.kotvy[kotva].hodnoty.indices) {
                        if (PodobnostStringu(cacheHodnot[kotva][hodnota], temp) > 0.8) {
                            body[kotva] += 1
                        }
                    }
                }
            }
            val temp = VratVhodneIndexy(body)
            if (temp[0] == 0) {
                return null
            } else {
                var maximum = body[temp[0]]
                var index = temp[0]
                for (i in temp) {
                    if (body[i] > maximum) {
                        maximum = body[i]
                        index = i
                    }
                }
                return stranka.kotvy[index]
            }
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