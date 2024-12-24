package com.example.mobapp

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ImageAnalyzerCam : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var stranky: Array<Stranka>
    private var stranka: Stranka? = null

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (!::stranky.isInitialized) {
            return
        }
        val mediaImage = image.image
        if (mediaImage != null) {
            recognizer.process(
                InputImage.fromMediaImage(
                    mediaImage,
                    image.imageInfo.rotationDegrees
                )
            ).addOnSuccessListener { text ->
                Log.i("ANALYZER", "Uspech")
                if (stranka == null) {
                    stranka = FunkceSpinave.VratDetekovanouStranku(text, stranky)
                    if (stranka != null) {
                        CacheRozparani.NastavCache(stranka!!)
                    }
                }
                if (stranka != null) {
                    FunkceSpinave.ZpracujText(text, stranka!!)
                    var temp = StringBuilder()
                    temp.appendLine(stranka!!.nazev)
                    for (kotva in stranka!!.kotvy) {
                        temp.appendLine(kotva.nazev)
                        for (hodnota in kotva.hodnoty) {
                            temp.appendLine("${hodnota.nazev} ${hodnota.hodnota}")
                        }
                    }
                    Log.i("ANALYZER", temp.toString())
                } else {
                    Log.i("ANALYZER", "Stranka je null")
                }
            }
                .addOnCompleteListener { /* image.close v tomto Listeneru, misto mimo nej je VELMI DULEZITE */
                    image.close()
                }
        }
    }

    public fun NastavStranky(stranky: Array<Stranka>) {
        this.stranky = stranky
        FunkceSpinave.NastavStranky(stranky)
        stranka = null
    }

    public fun NastavStranku(stranka: Stranka?) {
        this.stranka = stranka
        if (stranka != null) {
            CacheRozparani.NastavCache(stranka)
        }
    }
}