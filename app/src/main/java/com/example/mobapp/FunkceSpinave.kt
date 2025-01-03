package com.example.mobapp

import android.widget.EditText
import androidx.core.text.isDigitsOnly

object FunkceSpinave {
    private lateinit var mozneStranky: Array<Stranka>

    public fun VratDetekovanouStranku(
        text: com.google.mlkit.vision.text.Text,
        mozStranky: Array<Stranka>
    ): Stranka? {
        if (!::mozneStranky.isInitialized) { /*Lepsi spadnout drive, nez pozdeji*/
            throw IllegalStateException("mozneStranky nejsou inicializovane")
        }
        if (mozStranky.isEmpty()) {
            return null
        }
        if (mozneStranky.isEmpty()) {
            mozneStranky = mozStranky.clone()
        }
        mozneStranky = FunkceCiste.VratMozneStranky(text.textBlocks, mozneStranky)
        if (mozneStranky.size == 1) {
            return mozneStranky[0]
        }
        return null
    }

    public fun NastavStranky(stranky: Array<Stranka>) {
        mozneStranky = stranky
    }

    public fun JeVyplnenoSpravne(editText: EditText, typ: Typy): Boolean {
        if (editText.text.toString().isEmpty() && typ != Typy.TEXT) {
            editText.setError("Pole je prázdné")
            return false
        }
        if (typ != Typy.DECIMAL && typ != Typy.PROCENTO) {
            if (!typ.instance.JeTimtoTypem(editText.text.toString())) {
                editText.setError("Pole má špatnou hodnotu")
                return false
            }
        } else {
            if (!editText.text.toString().isDigitsOnly()) {
                if (typ == Typy.PROCENTO && editText.text.toString().removeSuffix("%")
                        .isDigitsOnly()
                ) {
                    return true
                }
                if (typ == Typy.PROCENTO) {
                    if (!typ.instance.JeTimtoTypem(editText.text.toString().replace(",", "."))) {
                        editText.setError("Pole má špatnou hodnotu")
                        return false
                    }
                } else {
                    if (!typ.instance.JeTimtoTypem(editText.text.toString().replace(".", ","))) {
                        editText.setError("Pole má špatnou hodnotu")
                        return false
                    }
                }
            }
        }
        return true
    }

    public fun ZpracujText(text: com.google.mlkit.vision.text.Text, stranka: Stranka) {
        for (kotva in stranka.kotvy) {
            kotva.bod = null
            for (hodnota in kotva.hodnoty) {
                hodnota.bod = null
            }
        }
        val zbyvajiciLinky = ArrayList<com.google.mlkit.vision.text.Text.Line>()
        val linkyHodnot = ArrayList<com.google.mlkit.vision.text.Text.Line>()
        /* Hledani kotev */
        var nalezenaKotva = false
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val temp = FunkceCiste.RozparujString(line.text)
                var podobnostMax = 0.0
                var indexPodobne = -1
                for (kotva in stranka.kotvy.indices) {
                    val temp2 =
                        FunkceCiste.PodobnostStringu(CacheRozparani.KotvyCache()[kotva], temp)
                    if (temp2 > 0.8 && temp2 > podobnostMax) {
                        podobnostMax = temp2
                        indexPodobne = kotva
                    }
                }
                if (indexPodobne != -1) {
                    nalezenaKotva = true
                    stranka.kotvy[indexPodobne].bod = line.cornerPoints
                } else {
                    zbyvajiciLinky.add(line)
                }
            }
        }
        /* Hledani hodnot */
        if (nalezenaKotva) {
            for (line in zbyvajiciLinky) {
                val kotva =
                    FunkceCiste.VratPatriciKotvu(
                        line.text, line.cornerPoints!![0], stranka.kotvy,
                        CacheRozparani.HodnotyCache()
                    )
                if (kotva == null) {
                    linkyHodnot.add(line)
                    continue
                }
                ZpracujHodnoty(line, kotva, linkyHodnot)
            }
        } else {
            val kotva =
                FunkceCiste.VratMoznouKotvu(stranka, zbyvajiciLinky, CacheRozparani.HodnotyCache())
            if (kotva == null) {
                for (line in zbyvajiciLinky) {
                    linkyHodnot.add(line)
                }
                return
            }
            for (line in zbyvajiciLinky) {
                ZpracujHodnoty(line, kotva, linkyHodnot)
            }
        }
        NajdiExtraHodnoty(zbyvajiciLinky, stranka)
        /* Prirazovani hodnot */
        for (line in linkyHodnot) {
            if (line.confidence < 0.5) {
                continue
            }
            val kotvy: Array<Kotva>
            var chybiHodnota = false
            val vzdalenosti = ArrayList<Double>()
            val hodnoty = ArrayList<Hodnota>()
            val tempKotva = FunkceCiste.VratPatriciKotvu(line.cornerPoints!![0], stranka.kotvy)
            if (tempKotva == null) {
                kotvy = stranka.kotvy
            } else {
                kotvy = arrayOf(tempKotva)
            }
            val listHodnot = ArrayList<Hodnota>()
            for (kotva in kotvy) {
                for (hodnota in kotva.hodnoty) {
                    listHodnot.add(hodnota)
                }
            }
            for (hodnota in stranka.extraHodnoty) {
                if (hodnota.bod != null && !hodnota.nazev.isEmpty()) {
                    listHodnot.add(hodnota)
                }
            }
            for (hodnota in listHodnot) {
                if (hodnota.bod == null) {
                    chybiHodnota = true
                    continue
                }
                val temp =
                    FunkceCiste.VzdalenostBodu(hodnota.bod!![3], line.cornerPoints!![0])
                if (temp > FunkceCiste.VzdalenostBodu(
                        hodnota.bod!![0],
                        line.cornerPoints!![0]
                    )
                ) {
                    continue
                }
                hodnoty.add(hodnota)
                vzdalenosti.add(temp)
            }
            if (vzdalenosti.isEmpty()) {
                if (Typy.DATUM.instance.JeTimtoTypem(line.text)) {
                    stranka.datum = line.text
                } else if (!stranka.extraHodnoty.isEmpty()) {
                    for (hodnota in stranka.extraHodnoty) {
                        if (hodnota.nazev == "" && hodnota.typ.instance.JeTimtoTypem(line.text)) {
                            hodnota.hodnota = line.text
                            break
                        }
                    }
                }
                continue
            }
            val temp = vzdalenosti.minOrNull() as Double
            val hodnota = hodnoty[vzdalenosti.indexOf(temp)]
            val temp2 = FunkceCiste.VzdalenostBodu(hodnota.bod!![0], hodnota.bod!![1])
            if (hodnota.typ.instance.JeTimtoTypem(line.text) && temp2 > temp && (hodnota.vzdalenost == null || hodnota.bodPred == null || hodnota.vzdalenost!! * (FunkceCiste.VzdalenostBodu(
                    hodnota.bodPred!![0],
                    hodnota.bodPred!![1]
                ) / temp2) > temp) && (chybiHodnota || hodnota.confidence < line.confidence)
            ) {
                hodnota.hodnota = line.text
                hodnota.confidence = line.confidence
                hodnota.vzdalenost = temp
                hodnota.bodPred = hodnota.bod?.clone()
                if (chybiHodnota) {
                    hodnota.confidence = 0.0f
                }
            } else {
                if (Typy.DATUM.instance.JeTimtoTypem(line.text)) {
                    stranka.datum = line.text
                } else if (!stranka.extraHodnoty.isEmpty()) {
                    for (hodnota in stranka.extraHodnoty) {
                        if (hodnota.nazev == "" && hodnota.typ.instance.JeTimtoTypem(line.text)) {
                            hodnota.hodnota = line.text
                            break
                        }
                    }
                }
            }
        }
    }

    private fun ZpracujHodnoty(
        line: com.google.mlkit.vision.text.Text.Line,
        kotva: Kotva,
        zbyvajiciLinky: ArrayList<com.google.mlkit.vision.text.Text.Line>,
    ) {
        var index = -1
        var maxPodobnost = -1.0
        val rozpar = FunkceCiste.RozparujString(line.text)
        for (hodnota in kotva.hodnoty.indices) {
            val temp = FunkceCiste.PodobnostStringu(rozpar, kotva.hodnoty[hodnota].nazev)
            if (temp > 0.8 && temp > maxPodobnost) {
                index = hodnota
                maxPodobnost = temp
            }
        }
        if (index != -1) {
            kotva.hodnoty[index].bod = line.cornerPoints
        } else {
            zbyvajiciLinky.add(line)
        }
    }

    private fun NajdiExtraHodnoty(
        linky: ArrayList<com.google.mlkit.vision.text.Text.Line>,
        stranka: Stranka
    ) {
        for (hodnota in stranka.extraHodnoty) {
            if (hodnota.nazev.isEmpty()) {
                continue
            }
            val temp = FunkceCiste.RozparujString(hodnota.nazev)
            for (line in linky) {
                if (FunkceCiste.PodobnostStringu(temp, line.text) > 0.8) {
                    hodnota.bod = line.cornerPoints
                    break
                }
            }
        }
    }
}