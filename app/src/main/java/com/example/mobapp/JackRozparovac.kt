package com.example.mobapp

object CacheRozparani {
    private var strankaCache = ArrayList<String>()
    private val kotvyCache = ArrayList<ArrayList<String>>()
    private val hodnotyCache = ArrayList<ArrayList<ArrayList<String>>>()

    public fun StrankaCache(): ArrayList<String> {
        if (strankaCache.isEmpty()) {
            throw IllegalStateException("strankaCache je prazdna")
        }
        return strankaCache
    }

    public fun KotvyCache(): ArrayList<ArrayList<String>> {
        if (kotvyCache.isEmpty()) {
            throw IllegalStateException("kotvyCache je prazdne")
        }
        return kotvyCache
    }

    public fun HodnotyCache(): ArrayList<ArrayList<ArrayList<String>>> {
        if (hodnotyCache.isEmpty()) {
            throw IllegalStateException("hodnotyCache jsou prazdne")
        }
        return hodnotyCache
    }

    public fun NastavCache(stranka: Stranka) {
        if (stranka.nazev.isEmpty()) {
            throw IllegalArgumentException("stranka nema nazev")
        }
        strankaCache = FunkceCiste.RozparujString(stranka.nazev)
        kotvyCache.clear()
        hodnotyCache.clear()
        for (kotva in stranka.kotvy.indices) {
            if (stranka.kotvy[kotva].nazev.isEmpty()) {
                throw IllegalArgumentException("kotva nema nazev")
            }
            kotvyCache.add(FunkceCiste.RozparujString(stranka.kotvy[kotva].nazev))
            hodnotyCache.add(ArrayList<ArrayList<String>>())
            for (hodnota in stranka.kotvy[kotva].hodnoty) {
                if (hodnota.nazev.isEmpty()) {
                    throw IllegalArgumentException("hodnota nema nazev")
                }
                hodnotyCache[kotva].add(FunkceCiste.RozparujString(hodnota.nazev))
            }
        }
    }

}