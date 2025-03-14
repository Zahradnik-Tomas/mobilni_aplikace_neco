package com.example.mobapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.isDigitsOnly
import com.example.mobapp.databinding.InternetZiskejStrankyBinding
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class InternetZiskejStranky : AppCompatActivity() {
    private lateinit var viewBinding: InternetZiskejStrankyBinding
    private val CONNECTION_TIMEOUT = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = InternetZiskejStrankyBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.button.setOnClickListener { Klik() }
    }

    private fun Klik() {
        if (!ZkontrolujInputy()) {
            return
        }
        if (viewBinding.registrace.isChecked) {
            val (error, kod) = RegistrujSe("register")
            if (!error.isEmpty()) {
                NapisChybu(error, kod)
                return
            }
            Toast.makeText(this, "Úspěšně zaregistrován", Toast.LENGTH_LONG).show()
        }
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
        val tajnyKod = viewBinding.tajnyKod.text.toString()
        val temp = Thread {
            val url = URL(
                "http",
                viewBinding.textView.text.toString(),
                viewBinding.editTextNumberSigned.text.toString().toInt(),
                "/${str}"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            if (str == "") {
                val encoder = Base64.getEncoder()
                val cipher = Cipher.getInstance("AES_256/ECB/PKCS5Padding")
                cipher.init(
                    Cipher.ENCRYPT_MODE,
                    SecretKeySpec(
                        MessageDigest.getInstance("SHA-256").digest(tajnyKod.toByteArray()), "AES"
                    )
                )
                connection.setRequestProperty(
                    "Username",
                    encoder.encodeToString(
                        cipher.doFinal(
                            viewBinding.username.text.toString().toByteArray()
                        )
                    )
                )
                connection.setRequestProperty(
                    "Password",
                    encoder.encodeToString(
                        cipher.doFinal(
                            viewBinding.heslo.text.toString().toByteArray()
                        )
                    )
                )
                connection.setRequestProperty(
                    "Token",
                    encoder.encodeToString(cipher.doFinal("OK".toByteArray()))
                )
            }
            try {

                if (connection.responseCode == 200) {
                    stringKVraceni = connection.inputStream.bufferedReader().readLine()
                    stringKVraceni = stringKVraceni.removePrefix("\"")
                        .removeSuffix("\"") /* Ten string zacina a konci s " ... kvuli tomu jsem debugoval hashovaci funkci kolem 2 hodin, heh */
                    if (str == "") {
                        val cipher = Cipher.getInstance("AES_256/ECB/PKCS5Padding")
                        cipher.init(
                            Cipher.DECRYPT_MODE,
                            SecretKeySpec(
                                MessageDigest.getInstance("SHA-256").digest(tajnyKod.toByteArray()),
                                "AES"
                            )
                        )
                        stringKVraceni = cipher.doFinal(Base64.getDecoder().decode(stringKVraceni))
                            .decodeToString()
                    }
                } else if (connection.responseCode == 500) {
                    intKVraceni = 500
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                } else {
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                }
            } catch (e: Exception) {
                errorKVraceni = e.message.toString()
            } finally {
                connection.disconnect()
            }
        }
        temp.start()
        temp.join()
        return Triple(stringKVraceni, errorKVraceni, intKVraceni)
    }

    private fun RegistrujSe(str: String = ""): Pair<String, Int> {
        var errorKVraceni = ""
        var intKVraceni = 0
        val tajnyKod = viewBinding.tajnyKod.text.toString()
        val temp = Thread {
            val cipher = Cipher.getInstance("AES_256/ECB/PKCS5Padding")
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(
                    MessageDigest.getInstance("SHA-256").digest(tajnyKod.toByteArray()),
                    "AES"
                )
            )
            val encoder = Base64.getEncoder()
            val url = URL(
                "http",
                viewBinding.textView.text.toString(),
                viewBinding.editTextNumberSigned.text.toString().toInt(),
                "/${str}"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECTION_TIMEOUT
            connection.requestMethod = "POST"
            connection.setRequestProperty(
                "Username",
                encoder.encodeToString(
                    cipher.doFinal(
                        viewBinding.username.text.toString().toByteArray()
                    )
                )
            )
            connection.setRequestProperty(
                "Password",
                encoder.encodeToString(
                    cipher.doFinal(
                        viewBinding.heslo.text.toString().toByteArray()
                    )
                )
            )
            connection.setRequestProperty(
                "Token",
                encoder.encodeToString(cipher.doFinal("OK".toByteArray()))
            )
            try {
                connection.connect()
                if (connection.responseCode == 200) {
                    null
                } else if (connection.responseCode == 500) {
                    intKVraceni = 500
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                } else {
                    errorKVraceni = connection.errorStream.bufferedReader().readLine()
                }
            } catch (e: Exception) {
                errorKVraceni = e.message.toString()
            } finally {
                connection.disconnect()
            }
        }
        temp.start()
        temp.join()
        return Pair(errorKVraceni, intKVraceni)
    }

    private fun ZkontrolujData(data: String): Boolean {
        val (_, error, kod) = ZiskejDataZeServeru("check/${VratHash(data)}")
        if (!error.isEmpty()) {
            NapisChybu(error, kod)
            return false
        }
        return true
    }

    private fun ZkontrolujInputy(): Boolean {
        var vratit = true
        if (viewBinding.editTextNumberSigned.text.toString()
                .isEmpty() || !viewBinding.editTextNumberSigned.text.toString().isDigitsOnly()
        ) {
            viewBinding.editTextNumberSigned.setError("Špatná hodnota")
            vratit = false
        }
        return vratit
    }
}