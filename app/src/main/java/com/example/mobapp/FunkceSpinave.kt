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

}