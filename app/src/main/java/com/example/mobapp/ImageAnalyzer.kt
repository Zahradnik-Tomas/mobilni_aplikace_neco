package com.example.mobapp

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class ImageAnalyzerCam : ImageAnalysis.Analyzer {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val mediaImage = image.image
        if (mediaImage != null) {
            recognizer.process(
                InputImage.fromMediaImage(
                    mediaImage,
                    image.imageInfo.rotationDegrees
                )
            ).addOnSuccessListener { Text ->
                //TODO FunkceCiste.ZpracujTentononc
            }
        }
    }
}