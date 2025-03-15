package com.example.mobapp

import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DB
import com.example.mobapp.DB.DBHodnota
import com.example.mobapp.DB.DBHodnotaExtra
import com.example.mobapp.DB.DBKotva
import com.example.mobapp.DB.DBStranka
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.databinding.VlozDoDbActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

class VlozDoDBActivity : AppCompatActivity() {
    lateinit var db: DB
    private val mutex = Mutex()
    private lateinit var viewBinding: VlozDoDbActivityBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(this, DB::class.java, getString(R.string.databaze_nazev)).build()
        val strankaDao = db.strankaDao()
        val stranka = Json.decodeFromString<Stranka>(
            intent.getStringExtra(getString(R.string.klic_json)).orEmpty()
        )
        viewBinding = VlozDoDbActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val table = findViewById<TableLayout>(R.id.vlozTableLayout)
        val extraHodnoty = ArrayList<EditText>()
        getHodnotaRow("Datum", stranka.datum, Typy.DATUM).let { par ->
            table.addView(par.first)
            extraHodnoty.add(par.second)
        }
        for (hodnota in stranka.extraHodnoty) {
            getHodnotaRow(hodnota.nazev, hodnota.hodnota, hodnota.typ).let { par ->
                table.addView(par.first)
                extraHodnoty.add(par.second)
            }
        }
        val hodnoty = ArrayList<EditText>()
        for (kotva in stranka.kotvy) {
            table.addView(getKotvaRow(kotva.nazev))
            for (hodnota in kotva.hodnoty) {
                getHodnotaRow(hodnota.nazev, hodnota.hodnota, hodnota.typ).let { par ->
                    table.addView(par.first)
                    hodnoty.add(par.second)
                }
            }
        }
        viewBinding.odesliButton.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                mutex.withLock { ->
                    Klik(extraHodnoty, stranka, hodnoty, strankaDao)
                }
            }
        }
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowCustomEnabled(true)
        supportActionBar?.setCustomView(R.layout.vloz_stranka)
        supportActionBar?.customView?.findViewById<TextView>(R.id.textViewStranka)?.text =
            stranka.nazev
    }

    private suspend fun Klik(
        extraHodnoty: ArrayList<EditText>,
        stranka: Stranka,
        hodnoty: ArrayList<EditText>,
        strankaDao: StrankaDao
    ) {
        var pokracuj = true
        var index = 1
        if (!FunkceSpinave.JeVyplnenoSpravne(extraHodnoty[0], Typy.DATUM)) {
            pokracuj = false
        }
        for (hodnotaExtra in stranka.extraHodnoty) {
            if (!FunkceSpinave.JeVyplnenoSpravne(extraHodnoty[index], hodnotaExtra.typ)) {
                pokracuj = false
            }
            index += 1
        }
        index = 0
        for (kotva in stranka.kotvy) {
            for (hodnota in kotva.hodnoty) {
                if (!FunkceSpinave.JeVyplnenoSpravne(hodnoty[index], hodnota.typ)) {
                    pokracuj = false
                }
                index += 1
            }
        }
        if (pokracuj) {
            val strankaId = strankaDao.insertStranka(
                DBStranka(
                    stranka.nazev,
                    Converters.fromString(extraHodnoty[0].text.toString())!!
                )
            )
            index = 1
            for (hodnotaExtra in stranka.extraHodnoty) {
                var hodnota = ""
                if (hodnotaExtra.typ == Typy.PROCENTO || hodnotaExtra.typ == Typy.DECIMAL) {
                    hodnota = extraHodnoty[index].text.toString().replace(",", ".")
                        .removeSuffix("%")
                } else {
                    hodnota = extraHodnoty[index].text.toString()
                }
                index += 1
                strankaDao.insertHodnotaExtra(
                    DBHodnotaExtra(
                        strankaId,
                        hodnotaExtra.nazev,
                        hodnota,
                        hodnotaExtra.typ.typ
                    )
                )
            }
            index = 0
            for (kotva in stranka.kotvy) {
                val kotvaId = strankaDao.insertKotva(DBKotva(strankaId, kotva.nazev))
                for (hodnota in kotva.hodnoty) {
                    var item = ""
                    if (hodnota.typ == Typy.PROCENTO || hodnota.typ == Typy.DECIMAL) {
                        item = hodnoty[index].text.toString().replace(",", ".")
                            .removeSuffix("%")
                    } else {
                        item = hodnoty[index].text.toString()
                    }
                    index += 1
                    strankaDao.insertHodnota(
                        DBHodnota(
                            kotvaId,
                            hodnota.nazev,
                            item,
                            hodnota.typ.typ
                        )
                    )
                }
            }
            val dataKVraceni = Intent()
            dataKVraceni.putExtra(getString(R.string.klic_json), "OK")
            setResult(RESULT_OK, dataKVraceni)
            finish()
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

    private fun getHodnotaRow(nazev: String, hodnota: String, typ: Typy): Pair<View, EditText> {
        val row = layoutInflater.inflate(R.layout.vloz_hodnota, viewBinding.root, false)
        row.findViewById<TextView>(R.id.textViewHodnota).text = nazev
        val pole = row.findViewById<EditText>(R.id.editTextHodnota)
        if (hodnota.isEmpty()) {
            pole.setText(typ.instance.VratDefHodnotu())
        } else {
            pole.setText(hodnota)
        }
        typ.instance.ZpracujView(pole, this)
        pole.hint = typ.instance.VratDefHodnotu()
        if (pole.hint.isEmpty()) {
            pole.hint = " "
        }
        pole.paintFlags = Paint.UNDERLINE_TEXT_FLAG
        return Pair(row, pole)
    }

    private fun getKotvaRow(nazev: String): View {
        val row = layoutInflater.inflate(R.layout.table_row_kotva, viewBinding.root, false)
        row.findViewById<TextView>(R.id.textViewKotva).text = nazev
        return row
    }
}