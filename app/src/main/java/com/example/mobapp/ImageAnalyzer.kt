package com.example.mobapp

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
                if (stranka == null) {
                    stranka = FunkceSpinave.VratDetekovanouStranku(text, stranky)
                }
                if (stranka != null) {
                    //TODO FunkceCiste.ZpracujTentononc
                }
            }
        }
    }

    public fun NastavStranky(stranky: Array<Stranka>) {
        this.stranky = stranky
        stranka = null
    }
}