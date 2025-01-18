package com.example.mobapp

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mobapp.databinding.MainActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: MainActivityBinding
    private lateinit var cameraExecutor: ExecutorService
    private val imageAnalyzerCam = ImageAnalyzerCam()
    private val intentLauncherInternet =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                if (intent != null && intent.extras != null) {
                    val jsonString = intent.extras!!.getString(
                        getString(R.string.klic_json)
                    ).toString()
                    var JdeDeserializovat = true
                    try {
                        imageAnalyzerCam.NastavStranky(
                            Json.decodeFromString<Array<Stranka>>(
                                jsonString
                            )
                        )
                    } catch (e: Exception) {
                        JdeDeserializovat = false
                        Toast.makeText(
                            this, "Objekt získaný ze serveru neodpovídá správnému templatu",
                            Toast.LENGTH_LONG
                        ).show()
                        e.printStackTrace()
                    }
                    if (JdeDeserializovat) {
                        val temp = this.openFileOutput(
                            getString(R.string.soubor_templatu),
                            Context.MODE_PRIVATE
                        )
                        temp.write(jsonString.toByteArray())
                        temp.close()
                    }
                }
            }
        }

    private var zobrazuji = false
    private var photoMode = false
    private val imageCapture =
        ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).build()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = MainActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.odesliButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                imageAnalyzerCam.strankaMutex.lock()
                if (imageAnalyzerCam.Stranka() != null) {
                    val intent = Intent(this@MainActivity, VlozDoDBActivity::class.java).putExtra(
                        getString(R.string.klic_json), Json.encodeToString(
                            Stranka.serializer(), imageAnalyzerCam.Stranka()!!
                        )
                    )
                    imageAnalyzerCam.strankaMutex.unlock()
                    startActivity(intent)
                } else {
                    imageAnalyzerCam.strankaMutex.unlock()
                }
            }
        }
        viewBinding.dataButton.setOnClickListener {
            startActivity(Intent(this, ZpracujDataActivity::class.java))
        }
        viewBinding.vycistiStrankuButton.setOnClickListener {
            imageAnalyzerCam.NastavStranku(null)
        }
        viewBinding.zobrazButton.setOnClickListener {
            zobrazuji = true
            val dialog = Dialog(this)
            val view = layoutInflater.inflate(R.layout.main_zobraz_stranku, viewBinding.root, false)
            dialog.setContentView(view)
            val table = view.findViewById<TableLayout>(R.id.tableMain)
            val button = view.findViewById<Button>(R.id.zavriZobrazeniMain)
            button.setOnClickListener {
                dialog.dismiss()
            }
            dialog.setOnDismissListener {
                zobrazuji = false
            }
            ColorDrawable(Color.WHITE).let { color ->
                color.alpha = 180
                dialog.window!!.setBackgroundDrawable(color)
            }
            dialog.window!!.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                (resources.displayMetrics.heightPixels * 0.8).toInt()
            )
            dialog.show()
            CoroutineScope(Dispatchers.Main).launch {
                while (zobrazuji) {
                    val stranka: Stranka?
                    imageAnalyzerCam.strankaMutex.withLock {
                        stranka = imageAnalyzerCam.Stranka()
                    }
                    table.removeAllViews()
                    val strankaRow =
                        layoutInflater.inflate(R.layout.main_row_stranka, viewBinding.root, false)
                    val strankaText = strankaRow.findViewById<TextView>(R.id.textViewStranka)
                    table.addView(strankaRow)
                    if (stranka == null) {
                        strankaText.text = "Nic"
                    } else {
                        strankaText.text = stranka.nazev
                        for (kotva in stranka.kotvy) {
                            val kotvaRow = layoutInflater.inflate(
                                R.layout.table_row_kotva,
                                viewBinding.root,
                                false
                            )
                            val kotvaText = kotvaRow.findViewById<TextView>(R.id.textViewKotva)
                            kotvaText.text = kotva.nazev
                            table.addView(kotvaRow)
                            for (hodnota in kotva.hodnoty) {
                                val hodnotaRow = layoutInflater.inflate(
                                    R.layout.table_row_hodnota,
                                    viewBinding.root,
                                    false
                                )
                                val hodnotaText =
                                    hodnotaRow.findViewById<TextView>(R.id.textViewHodnota)
                                hodnotaText.text = "${hodnota.nazev} - ${hodnota.hodnota}"
                                table.addView(hodnotaRow)
                            }
                        }
                    }
                    delay(100)
                }
            }
        }
        viewBinding.vyfotButton.setOnClickListener {
            imageCapture.takePicture(
                cameraExecutor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        imageAnalyzerCam.analyze(image)
                    }
                })
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            obstarejStranky()
            startCamera()
        } else {
            requestPermissions()
            return
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
        fotoMod()
    }

    override fun onPause() {
        super.onPause()
        cameraExecutor.shutdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build()
                .also { it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider) }
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                        .build()
                ).build().also { it.setAnalyzer(cameraExecutor, imageAnalyzerCam) }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                if (photoMode) {
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer
                    )
                }
            } catch (e: Exception) {
                Log.e("START_CAMERA", "bind cameraProvideru hodil chybu", e)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun fotoMod() {
        if (photoMode) {
            viewBinding.vyfotButton.visibility = View.VISIBLE
            viewBinding.fotoModButton.setImageResource(R.drawable.baseline_videocam)
            viewBinding.fotoModButton.setOnClickListener {
                photoMode = !photoMode
                fotoMod()
                startCamera()
            }
        } else {
            viewBinding.vyfotButton.visibility = View.GONE
            viewBinding.fotoModButton.setImageResource(R.drawable.baseline_photo_camera)
            viewBinding.fotoModButton.setOnClickListener {
                photoMode = !photoMode
                fotoMod()
                startCamera()
            }
        }
    }

    private fun obstarejStranky() {
        if (imageAnalyzerCam.Stranka() != null) {
            return
        }
        if (this.fileList().contains("templates.json")) {
            val temp = this.openFileInput(getString(R.string.soubor_templatu)).bufferedReader()
            imageAnalyzerCam.NastavStranky(
                Json.decodeFromString<Array<Stranka>>(temp.readLine())
            )
            temp.close()
            return
        }

        intentLauncherInternet.launch(Intent(this, InternetZiskejStranky::class.java))
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE
            ).toTypedArray()
    }

    private val activityResultLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    permissionGranted = false
            }
            startCamera()
        }
}