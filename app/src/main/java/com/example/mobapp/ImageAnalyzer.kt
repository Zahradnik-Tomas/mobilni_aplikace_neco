package com.example.mobapp

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ImageAnalyzerCam : ImageAnalysis.Analyzer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var stranky: Array<Stranka>
    private var stranka: Stranka? = null
    val strankaMutex = Mutex()
    private var strankaZmenena = false

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
                    NastavStranku(FunkceSpinave.VratDetekovanouStranku(text, stranky))
                }
                CoroutineScope(Dispatchers.Main).launch {
                    strankaMutex.lock()
                    if (stranka != null) {
                        val tempStranka = stranka!!.copy()
                        strankaZmenena = false
                        strankaMutex.unlock()
                        FunkceSpinave.ZpracujText(text, tempStranka)
                        strankaMutex.withLock { ->
                            if (!strankaZmenena) {
                                stranka = tempStranka
                            }
                        }
                    } else {
                        strankaMutex.unlock()
                    }
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
        NastavStranku(null)
    }

    public fun NastavStranku(stranka: Stranka?) {
        CoroutineScope(Dispatchers.Main).launch {
            strankaMutex.withLock { ->
                strankaZmenena = true
                this@ImageAnalyzerCam.stranka = stranka
                if (stranka != null) {
                    CacheRozparani.NastavCache(stranka)
                } else {
                    FunkceSpinave.NastavStranky(stranky)
                }
            }
        }
    }

    public fun Stranka(): Stranka? {
        return this.stranka
    }
}