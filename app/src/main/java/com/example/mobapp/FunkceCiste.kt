package com.example.mobapp

import android.graphics.Point
import kotlin.math.pow
import kotlin.math.sqrt

class FunkceCiste private constructor() {
    companion object {
        public fun PodobnostStringu(str1: String, str2: String): Double {
            var par1 = RozparujString(str1)
            var par2 = RozparujString(str2)
            return PodobnostStringu(par1, par2)
        }

        public fun PodobnostStringu(par1: ArrayList<String>, str2: String): Double {
            var par2 = RozparujString(str2)
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

        public fun RozparujString(string: String): ArrayList<String> {
            val str = string.uppercase()
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
            val temp = listVhodnosti.maxOrNull()
            if(temp == null || temp == 0){
                return emptyArray()
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

        public fun VratPatriciKotvu(
            str: String,
            bod: Point,
            kotvy: Array<Kotva>,
            cacheHodnot: java.util.ArrayList<ArrayList<ArrayList<String>>>
        ): Kotva? {
            val tempArrayKotev = ArrayList<Kotva>()
            val tempArrayVzdalenosti = ArrayList<Double>()
            val temp = RozparujString(str)
            for (kotva in kotvy.indices) {
                if (kotvy[kotva].bod == null) {
                    continue
                }
                var nalezeno = false
                for (hodnota in kotvy[kotva].hodnoty.indices) {
                    if (PodobnostStringu(cacheHodnot[kotva][hodnota], temp) > 0.8) {
                        nalezeno = true
                        break
                    }
                }
                if (!nalezeno) {
                    continue
                }
                val temp2 = VzdalenostBodu(kotvy[kotva].bod!![3], bod)
                if (VzdalenostBodu(kotvy[kotva].bod!![0], bod) < temp2) {
                    continue
                }
                tempArrayVzdalenosti.add(temp2)
                tempArrayKotev.add(kotvy[kotva])
            }
            if (tempArrayKotev.size > 0) {
                return tempArrayKotev[tempArrayVzdalenosti.indexOf(tempArrayVzdalenosti.minOrNull())]
            }
            return null
        }

        public fun VratPatriciKotvu(bod: Point, kotvy: Array<Kotva>): Kotva? {
            val tempArrayKotev = ArrayList<Kotva>()
            val tempArrayVzdalenosti = ArrayList<Double>()
            for (kotva in kotvy) {
                if (kotva.bod == null) {
                    continue
                }
                val temp = VzdalenostBodu(kotva.bod!![3], bod)
                if (VzdalenostBodu(kotva.bod!![0], bod) < temp) {
                    continue
                }
                tempArrayKotev.add(kotva)
                tempArrayVzdalenosti.add(temp)
            }
            if (tempArrayKotev.size > 0) {
                return tempArrayKotev[tempArrayVzdalenosti.indexOf(tempArrayVzdalenosti.minOrNull())]
            }
            return null
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
            if (temp.isEmpty() || body[temp[0]] == 0) {
                return null /* Respektive druha podminka muze byt pravdiva pouze v pripade jedne kotvy, nebot s 0 body to vrati prazdne pole */
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
                return listVhodnosti.indices.toList().toTypedArray()
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