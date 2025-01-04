package com.example.mobapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mobapp.databinding.InternetZiskejStrankyBinding
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class InternetZiskejStranky : AppCompatActivity() {
    private lateinit var viewBinding: InternetZiskejStrankyBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = InternetZiskejStrankyBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.button.setOnClickListener { Klik() }
    }

    private fun Klik() {
        val (data, error, kod) = ZiskejDataZeServeru()
        if (!error.isEmpty()) {
            NapisChybu(error, kod)
        } else if (ZkontrolujData(data)) {
            val dataKVraceni = Intent()
            dataKVraceni.putExtra(getString(R.string.klic_json), data)
            setResult(RESULT_OK, dataKVraceni)
            finish()
        }
    }

    private fun NapisChybu(error: String, kod: Int) {
        var strTemp = ""
        if (kod == 500) {
            strTemp = "Chyba na serveru:"
        } else {
            strTemp = "Selhání:"
        }
        Toast.makeText(this, "${strTemp} ${error}", Toast.LENGTH_LONG).show()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun VratHash(str: String): String {
        return MessageDigest.getInstance("SHA-256").digest(str.toByteArray()).toHexString()
    }

    private fun ZiskejDataZeServeru(str: String = ""): Triple<String, String, Int> {
        var stringKVraceni = ""
        var errorKVraceni = ""
        var intKVraceni = 0
        /* Tohle je absolutne retardovane */
        val temp = Thread {
            val url = URL(
                "http",
                viewBinding.textView.text.toString(),
                viewBinding.editTextNumberSigned.text.toString().toInt(),
                "/${str}"
            )
            val connection = url.openConnection() as HttpURLConnection
            try {

                if (connection.responseCode == 200) {
                    stringKVraceni = connection.inputStream.bufferedReader().readLine()
                    stringKVraceni = stringKVraceni.removePrefix("\"")
                        .removeSuffix("\"") /* Ten string zacina a konci s " ... kvuli tomu jsem debugoval hashovaci funkci kolem 2 hodin, heh */
                } else if (connection.responseCode == 500) {
                    intKVraceni = 500
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                } else {
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                }
            } finally {
                connection.disconnect()
            }
        }
        temp.start()
        temp.join()
        return Triple(stringKVraceni, errorKVraceni, intKVraceni)
    }

    private fun ZkontrolujData(data: String): Boolean {
        val (_, error, kod) = ZiskejDataZeServeru("check/${VratHash(data)}")
        if (!error.isEmpty()) {
            NapisChybu(error, kod)
            return false
        }
        return true
    }
}