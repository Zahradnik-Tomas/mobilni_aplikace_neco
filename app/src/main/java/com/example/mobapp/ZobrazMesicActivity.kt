package com.example.mobapp

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DB
import com.example.mobapp.databinding.ZobrazMesicActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ZobrazMesicActivity : AppCompatActivity() {
    private lateinit var db: DB
    private lateinit var viewBinding: ZobrazMesicActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(this, DB::class.java, getString(R.string.databaze_nazev)).build()
        val strankaDao = db.strankaDao()
        viewBinding = ZobrazMesicActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val datumy = intent.getStringArrayExtra(getString(R.string.klic_json))
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.vloz_stranka)
        val textView = supportActionBar?.customView?.findViewById<TextView>(R.id.textViewStranka)
        textView?.text = "${datumy!![0]} - ${datumy[1]}"
        textView?.textSize = 25f
        val table = viewBinding.zobrazTableLayout
        CoroutineScope(Dispatchers.Main).launch {
            val kostry = strankaDao.vratNejnovKostryStranekMeziDaty(
                Converters.fromString(datumy!![0])!!,
                Converters.fromString(datumy[1])!!
            )
            val stranky = ArrayList<Stranka>()
            for (stranka in kostry) {
                val kotvy = ArrayList<Kotva>()
                for (kotva in stranka.kotvy) {
                    val hodnoty = ArrayList<Hodnota>()
                    for (hodnota in kotva.hodnoty) {
                        for (typ in AgrFunkce.SUM.kompTypy) {
                            if (hodnota.typ == typ.typ) {
                                hodnoty.add(
                                    Hodnota(
                                        hodnota.nazev, hodnota = strankaDao.vratSUMMeziDaty(
                                            stranka.stranka.nazev, kotva.kotva.nazev, hodnota.nazev,
                                            Converters.fromString(datumy[0])!!,
                                            Converters.fromString(datumy[1])!!
                                        ).toString()
                                    )
                                )
                                break
                            }
                        }
                    }
                    if (hodnoty.isNotEmpty()) {
                        kotvy.add(Kotva(kotva.kotva.nazev, hodnoty.toTypedArray()))
                    }
                }
                if (kotvy.isNotEmpty()) {
                    stranky.add(Stranka(stranka.stranka.nazev, kotvy.toTypedArray()))
                }
            }
            for (stranka in stranky) {
                table.addView(getStrankaRow(stranka.nazev))
                for (kotva in stranka.kotvy) {
                    table.addView(getKotvaRow(kotva.nazev))
                    for (hodnota in kotva.hodnoty) {
                        table.addView(getHodnotaRow(hodnota.nazev, hodnota.hodnota))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
        }
        return false
    }

    private fun getHodnotaRow(nazev: String, hodnota: String): View {
        val row = layoutInflater.inflate(R.layout.table_row_hodnota2, viewBinding.root, false)
        row.findViewById<TextView>(R.id.textViewNazev).text = nazev
        row.findViewById<TextView>(R.id.textViewHodnota).text = hodnota
        return row
    }

    private fun getKotvaRow(nazev: String): View {
        val row = layoutInflater.inflate(R.layout.table_row_kotva, viewBinding.root, false)
        row.findViewById<TextView>(R.id.textViewKotva).text = nazev
        return row
    }

    private fun getStrankaRow(nazev: String): View {
        val row = layoutInflater.inflate(R.layout.table_row_stranka, viewBinding.root, false)
        row.findViewById<TextView>(R.id.textViewStranka).text = nazev
        return row
    }
}