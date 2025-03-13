package com.example.mobapp

import android.graphics.Paint
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doBeforeTextChanged
import androidx.room.Room
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DB
import com.example.mobapp.databinding.ZobrazMesicActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

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
        val temp = this.openFileInput(getString(R.string.soubor_templatu)).bufferedReader()
        val Stranky = Json.decodeFromString<Array<Stranka>>(temp.readLine())
        temp.close()
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
                    var vypocty = emptyArray<Vypocet>()
                    for (dalsiStranka in Stranky) {
                        if (dalsiStranka.nazev == stranka.stranka.nazev) {
                            vypocty = dalsiStranka.vypocty
                            break
                        }
                    }
                    stranky.add(
                        Stranka(
                            stranka.stranka.nazev,
                            kotvy.toTypedArray(),
                            vypocty = vypocty
                        )
                    )
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
                for (vypocet in stranka.vypocty) {
                    for (view in vratVypoctyRows(vypocet, stranka)) {
                        table.addView(view)
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

    private fun vratVypoctyRows(vypocet: Vypocet, stranka: Stranka): ArrayList<View> {
        val vypocty = ArrayList<View>()
        val vysledekView =
            layoutInflater.inflate(R.layout.table_row_hodnota2, viewBinding.root, false)
        vysledekView.findViewById<TextView>(R.id.textViewNazev).text = vypocet.nazevVysledku
        val vysledek = vysledekView.findViewById<TextView>(R.id.textViewHodnota)
        for (nasobitel in vypocet.nasobitele) {
            val nasobitelView =
                layoutInflater.inflate(R.layout.vloz_hodnota, viewBinding.root, false)
            nasobitelView.findViewById<TextView>(R.id.textViewHodnota).text = nasobitel.nazevVypoctu
            val nasobitelNasobic = nasobitelView.findViewById<EditText>(R.id.editTextHodnota)
            Typy.DECIMAL.instance.ZpracujView(nasobitelNasobic, this)
            nasobitelNasobic.doBeforeTextChanged { s, st, c, a ->
                if (Typy.DECIMAL.instance.JeTimtoTypem(
                        nasobitelNasobic.text.toString().replace(".", ",")
                    ) || Typy.CISLO.instance.JeTimtoTypem(nasobitelNasobic.text.toString())
                ) {
                    if (Typy.DECIMAL.instance.JeTimtoTypem(
                            vysledek.text.toString().replace(".", ",")
                        )
                    ) {
                        vysledek.text =
                            (vysledek.text.toString().replace(",", ".").toDouble() - VratSUMHodnotu(
                                nasobitel.kotvaNazev,
                                nasobitel.hodnotaNazev,
                                stranka
                            ) * nasobitelNasobic.text.toString().replace(",", ".")
                                .toDouble()).toString()
                    }
                }
            }
            nasobitelNasobic.doAfterTextChanged { e ->
                if (Typy.DECIMAL.instance.JeTimtoTypem(
                        nasobitelNasobic.text.toString().replace(".", ",")
                    ) || Typy.CISLO.instance.JeTimtoTypem(nasobitelNasobic.text.toString())
                ) {
                    if (Typy.DECIMAL.instance.JeTimtoTypem(
                            vysledek.text.toString().replace(".", ",")
                        )
                    ) {
                        vysledek.text =
                            (vysledek.text.toString().replace(",", ".").toDouble() + VratSUMHodnotu(
                                nasobitel.kotvaNazev,
                                nasobitel.hodnotaNazev,
                                stranka
                            ) * nasobitelNasobic.text.toString().replace(",", ".")
                                .toDouble()).toString()
                    } else {
                        vysledek.text =
                            (VratSUMHodnotu(
                                nasobitel.kotvaNazev,
                                nasobitel.hodnotaNazev,
                                stranka
                            ) * nasobitelNasobic.text.toString().replace(",", ".")
                                .toDouble()).toString()
                    }
                } else {
                    nasobitelNasobic.error = "Není platná hodnota"
                }
            }
            nasobitelNasobic.setText(nasobitel.nasobic.toString())
            nasobitelNasobic.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            vypocty.add(nasobitelView)
        }
        for (pricitac in vypocet.pricitaci) {
            val pricitacViewHodnota =
                layoutInflater.inflate(R.layout.vloz_hodnota, viewBinding.root, false)
            val pricitacViewPocet =
                layoutInflater.inflate(R.layout.vloz_hodnota, viewBinding.root, false)
            pricitacViewHodnota.findViewById<TextView>(R.id.textViewHodnota).text =
                pricitac.nazevVypoctu
            pricitacViewPocet.findViewById<TextView>(R.id.textViewHodnota).text =
                pricitac.nazevMultiplikatoru
            val pricitacHodnota = pricitacViewHodnota.findViewById<EditText>(R.id.editTextHodnota)
            val pricitacMultiplikator =
                pricitacViewPocet.findViewById<EditText>(R.id.editTextHodnota)
            for (view in arrayOf(pricitacHodnota, pricitacMultiplikator)) {
                Typy.DECIMAL.instance.ZpracujView(view, this)
                view.doBeforeTextChanged { s, st, c, a ->
                    if ((Typy.DECIMAL.instance.JeTimtoTypem(
                            pricitacHodnota.text.toString().replace(".", ",")
                        ) || Typy.CISLO.instance.JeTimtoTypem(pricitacHodnota.text.toString())
                                ) && (Typy.DECIMAL.instance.JeTimtoTypem(
                            pricitacMultiplikator.text.toString().replace(".", ",")
                        ) || Typy.CISLO.instance.JeTimtoTypem(pricitacMultiplikator.text.toString()))
                    ) {
                        if (Typy.DECIMAL.instance.JeTimtoTypem(
                                vysledek.text.toString().replace(".", ",")
                            )
                        ) {
                            vysledek.text =
                                (vysledek.text.toString().replace(",", ".")
                                    .toDouble() - pricitacHodnota.text.toString().replace(",", ".")
                                    .toDouble() * pricitacMultiplikator.text.toString()
                                    .replace(",", ".")
                                    .toDouble()).toString()
                        }
                    }
                }
                view.doAfterTextChanged { e ->
                    if (Typy.DECIMAL.instance.JeTimtoTypem(
                            view.text.toString().replace(".", ",")
                        ) || Typy.CISLO.instance.JeTimtoTypem(view.text.toString())
                    ) {
                        if ((Typy.DECIMAL.instance.JeTimtoTypem(
                                pricitacHodnota.text.toString().replace(".", ",")
                            ) || Typy.CISLO.instance.JeTimtoTypem(pricitacHodnota.text.toString())
                                    ) && (Typy.DECIMAL.instance.JeTimtoTypem(
                                pricitacMultiplikator.text.toString().replace(".", ",")
                            ) || Typy.CISLO.instance.JeTimtoTypem(pricitacMultiplikator.text.toString()))
                        ) {
                            if (Typy.DECIMAL.instance.JeTimtoTypem(
                                    vysledek.text.toString().replace(".", ",")
                                )
                            ) {
                                vysledek.text =
                                    (vysledek.text.toString().replace(",", ".")
                                        .toDouble() + pricitacHodnota.text.toString()
                                        .replace(",", ".")
                                        .toDouble() * pricitacMultiplikator.text.toString()
                                        .replace(",", ".").toDouble()).toString()
                            } else {
                                vysledek.text =
                                    (pricitacHodnota.text.toString().replace(",", ".")
                                        .toDouble() * pricitacMultiplikator.text.toString()
                                        .replace(",", ".").toDouble()).toString()
                            }
                        }

                    } else {
                        view.error = "Není platná hodnota"
                    }
                }
            }
            pricitacHodnota.setText(pricitac.pricteno.toString())
            pricitacMultiplikator.setText("0.0")
            pricitacHodnota.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            pricitacMultiplikator.paintFlags = Paint.UNDERLINE_TEXT_FLAG
            vypocty.add(pricitacViewHodnota)
            vypocty.add(pricitacViewPocet)
        }
        if (vypocty.isNotEmpty()) {
            vypocty.add(vysledekView)
        }
        return vypocty
    }

    private fun VratSUMHodnotu(kotvaNazev: String, hodnotaNazev: String, stranka: Stranka): Double {
        for (kotva in stranka.kotvy) {
            if (kotva.nazev != kotvaNazev) {
                continue
            }
            for (hodnota in kotva.hodnoty) {
                if (hodnota.nazev != hodnotaNazev) {
                    continue
                }
                return hodnota.hodnota.toDouble()
            }
        }
        return 0.0
    }
}