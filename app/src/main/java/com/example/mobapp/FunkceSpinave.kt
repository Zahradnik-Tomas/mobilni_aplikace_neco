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
                hodnota.hodnotaBod = null
            }
        }
        val zbyvajiciKotvy = ArrayList(stranka.kotvy.indices.toList())
        val zbyvajiciLinky = ArrayList<com.google.mlkit.vision.text.Text.Line>()
        var nalezenaKotva = false
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val temp = FunkceCiste.RozparujString(line.text)
                var podobnostMax = 0.0
                var indexPodobne = -1
                for (kotva in zbyvajiciKotvy) {
                    val temp2 =
                        FunkceCiste.PodobnostStringu(CacheRozparani.KotvyCache()[kotva], temp)
                    if (temp2 > 0.8 && temp2 > podobnostMax) {
                        podobnostMax = temp2
                        indexPodobne = kotva
                    }
                }
                if (indexPodobne != -1) {
                    nalezenaKotva = true
                    zbyvajiciKotvy.remove(indexPodobne)
                    stranka.kotvy[indexPodobne].bod = line.cornerPoints
                } else {
                    zbyvajiciLinky.add(line)
                }
            }
        }
        zbyvajiciKotvy.clear()
        val mozneKotvy = ArrayList<Kotva>()
        val mozneHodnoty = ArrayList<Hodnota>()
        if (nalezenaKotva) {
            for (line in zbyvajiciLinky.toTypedArray()) {
                for (kotva in stranka.kotvy.indices) {
                    if (stranka.kotvy[kotva].bod != null) {
                        val temp =
                            FunkceCiste.VratPatriciKotvu(
                                line.cornerPoints!![0],
                                kotva,
                                stranka.kotvy
                            )
                        if (temp != -1) {
                            if (!mozneKotvy.contains(stranka.kotvy[temp])) {
                                mozneKotvy.add(stranka.kotvy[temp])
                            }
                            ZpracujHodnoty(
                                line,
                                stranka.kotvy[temp],
                                temp,
                                zbyvajiciLinky,
                                mozneHodnoty
                            )
                        } else {
                            zbyvajiciLinky.remove(line)
                        }
                    }
                }
            }
        } else {
            val kotva =
                FunkceCiste.VratMoznouKotvu(stranka, zbyvajiciLinky, CacheRozparani.HodnotyCache())
            if (kotva == null) {
                return
            }
            mozneKotvy.add(kotva)
            val index = stranka.kotvy.indexOf(kotva)
            for (line in zbyvajiciLinky.toTypedArray()) {
                ZpracujHodnoty(line, kotva, index, zbyvajiciLinky, mozneHodnoty)
            }
        }
        for (line in zbyvajiciLinky) {
            if (line.confidence < 0.5) {
                continue
            }
            val vzdalenosti = ArrayList<Double>()
            val hodnoty = ArrayList<Hodnota>()
            for (hodnota in mozneHodnoty) {
                val temp = FunkceCiste.VzdalenostBodu(hodnota.bod!![3], line.cornerPoints!![0])
                if (FunkceCiste.VzdalenostBodu(hodnota.bod!![0], line.cornerPoints!![0]) < temp) {
                    continue
                }
                vzdalenosti.add(temp)
                hodnoty.add(hodnota)
            }
            val temp = vzdalenosti.minOrNull()
            if (temp != null) {
                val index = vzdalenosti.indexOf(temp)
                hodnoty[index].hodnota = line.text
                mozneHodnoty.remove(hodnoty[index])
            }
        }
    }

    private fun ZpracujHodnoty(
        line: com.google.mlkit.vision.text.Text.Line,
        kotva: Kotva,
        indexKotvy: Int,
        zbyvajiciLinky: ArrayList<com.google.mlkit.vision.text.Text.Line>,
        mozneHodnoty: ArrayList<Hodnota>
    ) {
        val rozparTemp = FunkceCiste.RozparujString(line.text)
        var podobnostMax = 0.0
        var indexPodobne = -1
        val zbyvajiciHodnoty = ArrayList(kotva.hodnoty.indices.toList())
        for (hodnota in zbyvajiciHodnoty) {
            val temp2 = FunkceCiste.PodobnostStringu(
                CacheRozparani.HodnotyCache()[indexKotvy][hodnota],
                rozparTemp
            )
            if (temp2 > 0.8 && temp2 > podobnostMax) {
                podobnostMax = temp2
                indexPodobne = hodnota
            }
        }
        if (indexPodobne != -1) {
            zbyvajiciLinky.remove(line)
            kotva.hodnoty[indexPodobne].bod = line.cornerPoints
            zbyvajiciHodnoty.remove(indexPodobne)
            mozneHodnoty.add(kotva.hodnoty[indexPodobne])
        }
    }

}