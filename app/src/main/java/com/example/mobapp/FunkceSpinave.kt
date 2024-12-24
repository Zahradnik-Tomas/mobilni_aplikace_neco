package com.example.mobapp

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

    public fun ZpracujText(text: com.google.mlkit.vision.text.Text, stranka: Stranka) {
        for (kotva in stranka.kotvy) {
            kotva.bod = null
            for (hodnota in kotva.hodnoty) {
                hodnota.bod = null
            }
        }
        val zbyvajiciLinky = ArrayList<com.google.mlkit.vision.text.Text.Line>()
        val linkyHodnot = ArrayList<com.google.mlkit.vision.text.Text.Line>()
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
        for (line in linkyHodnot) {
            val kotva = FunkceCiste.VratPatriciKotvu(line.cornerPoints!![0], stranka.kotvy)
            var chybiHodnota = false
            val vzdalenosti = ArrayList<Double>()
            val hodnoty = ArrayList<Hodnota>()
            if (kotva == null) {
                for (kotva in stranka.kotvy) {
                    for (hodnota in kotva.hodnoty) {
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
                }
            } else {
                for (hodnota in kotva.hodnoty) {
                    if (hodnota.bod == null) {
                        chybiHodnota = true
                        continue
                    }
                    val temp = FunkceCiste.VzdalenostBodu(hodnota.bod!![3], line.cornerPoints!![0])
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
            }
            if (vzdalenosti.isEmpty()) {
                continue
            }
            val temp = vzdalenosti.minOrNull() as Double
            val hodnota = hodnoty[vzdalenosti.indexOf(temp)]
            if (FunkceCiste.VzdalenostBodu(
                    hodnota.bod!![0],
                    hodnota.bod!![1]
                ) > temp && (chybiHodnota || hodnota.confidence < line.confidence)
            ) {
                hodnota.hodnota = line.text
                hodnota.confidence = line.confidence
                if (chybiHodnota) {
                    hodnota.confidence = 0.0f
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
}