package com.example.mobapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.mobapp.databinding.MainActivityBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = MainActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.odesliButton.setOnClickListener {
            if (imageAnalyzerCam.Stranka() != null) {
                val intent = Intent(this, VlozDoDBActivity::class.java).putExtra(
                    getString(R.string.klic_json), Json.encodeToString(
                        Stranka.serializer(), imageAnalyzerCam.Stranka()!!
                    )
                )
                startActivity(intent)
            }
        }
        viewBinding.dataButton.setOnClickListener {
            startActivity(Intent(this, ZpracujDataActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            obstarejStranky()
            startCamera()
        } else {
            requestPermissions()
        }
        cameraExecutor = Executors.newSingleThreadExecutor()
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
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e("START_CAMERA", "bind cameraProvideru hodil chybu", e)
            }
        }, ContextCompat.getMainExecutor(this))
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
            var cameraGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && it.value == false)
                    if (it.key == Manifest.permission.CAMERA) {
                        cameraGranted = false
                    } else {
                        permissionGranted = false
                    }
            }
            if (permissionGranted && cameraGranted) {
                startCamera()
            } else if (permissionGranted) {
                obstarejStranky()
                // prace bez kamery - z obrazku?
            } else {
                // prace bez ostatnich prvku - je template uz stazen?
            }
        }
}