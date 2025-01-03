package com.example.mobapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.text.isDigitsOnly
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

class VlozDoDBActivity : ComponentActivity() {
    lateinit var db: DB
    private val mutex = Mutex()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(this, DB::class.java, getString(R.string.databaze_nazev)).build()
        val strankaDao = db.strankaDao()
        val stranka = Json.decodeFromString<Stranka>(
            intent.getStringExtra(getString(R.string.klic_json)).orEmpty()
        )
        val viewBinding = VlozDoDbActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        val table = TableLayout(this)
        viewBinding.scrollView.addView(table)
        table.addView(getTableRow(stranka.nazev).first)
        val extraHodnoty = ArrayList<EditText>()
        getTableRow("Datum", stranka.datum, Typy.DATUM).let {
            table.addView(it.first)
            it.second?.let { extraHodnoty.add(it) }
        }
        for (hodnota in stranka.extraHodnoty) {
            val viewPair = getTableRow(hodnota.nazev, hodnota.hodnota, hodnota.typ)
            table.addView(viewPair.first)
            viewPair.second?.let { extraHodnoty.add(it) }
        }
        val hodnoty = ArrayList<EditText>()
        for (kotva in stranka.kotvy) {
            table.addView(getTableRow(kotva.nazev).first)
            for (hodnota in kotva.hodnoty) {
                val viewPair = getTableRow(hodnota.nazev, hodnota.hodnota, hodnota.typ)
                table.addView(viewPair.first)
                viewPair.second?.let { hodnoty.add(it) }
            }
        }
        val button = Button(this)
        table.addView(button)
        button.text = "Posli"
        button.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                mutex.withLock { ->
                    Klik(extraHodnoty, stranka, hodnoty, strankaDao)
                }
            }
        }
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
            finish()
        }
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    private fun getTableRow(
        str1: String,
        str2: String? = null,
        typ: Typy = Typy.CISLO
    ): Pair<TableRow, EditText?> {
        val temp = TableRow(this)
        val textView = TextView(this)
        textView.text = str1
        textView.textSize = 12f
        temp.addView(textView)
        var editText: EditText? = null
        if (str2 != null) {
            editText = typ.instance.VratView(this, str2)
            temp.addView(editText)
        }
        return Pair(temp, editText)
    }
}