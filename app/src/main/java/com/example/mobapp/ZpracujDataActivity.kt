package com.example.mobapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DB
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.DB.strankaSKotvami
import com.example.mobapp.UI.ActionModeDBEntita
import com.example.mobapp.UI.RecyclerViewAdapterDBEntity
import com.example.mobapp.UI.RecyclerViewDBEntita
import com.example.mobapp.databinding.ZpracujDataActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Date

class ZpracujDataActivity : AppCompatActivity() {
    private lateinit var viewBinding: ZpracujDataActivityBinding
    private lateinit var db: DB
    lateinit var strankaDao: StrankaDao
    private lateinit var recyclerView: RecyclerView
    private lateinit var delDatumButton: ImageButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = Room.databaseBuilder(this, DB::class.java, getString(R.string.databaze_nazev)).build()
        strankaDao = db.strankaDao()
        viewBinding = ZpracujDataActivityBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        recyclerView = viewBinding.recycler
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter =
            RecyclerViewAdapterDBEntity(ArrayList(), viewBinding, this, strankaDao)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val datumOdEditText = findViewById<EditText>(R.id.datumOd)
        val datumDoEditText = findViewById<EditText>(R.id.datumDo)
        Typy.DATUM.instance.ZpracujView(datumOdEditText, this)
        Typy.DATUM.instance.ZpracujView(datumDoEditText, this)
        delDatumButton = findViewById<ImageButton>(R.id.buttonDelDatum)
        delDatumButton.setOnClickListener {
            datumOdEditText.text.clear()
            datumDoEditText.text.clear()
        }
        datumOdEditText.doAfterTextChanged { editable ->
            datumOd = editable.toString()
            typAgr = null
            ActionModeDBEntita.actionMode?.finish()
            zobrazVsechnyData(
                Converters.fromString(datumDo),
                Converters.fromString(datumOd),
                desc = desc
            )
        }
        datumDoEditText.doAfterTextChanged { editable ->
            datumDo = editable.toString()
            typAgr = null
            ActionModeDBEntita.actionMode?.finish()
            zobrazVsechnyData(
                Converters.fromString(datumDo),
                Converters.fromString(datumOd),
                desc = desc
            )
        }
        title = ""
        zobrazVsechnyData(
            Converters.fromString(datumDo),
            Converters.fromString(datumOd),
            desc = this.desc
        )
    }

    private var datumOd = ""
    private var datumDo = ""
    private var desc = true
    private var typAgr: AgrFunkce? = null

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.zpracuj_data_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.smazdb -> {
                AlertDialog.Builder(this).setMessage("Doopravdy chcete smazat celou databázi?")
                    .setPositiveButton("Ano") { dialog, id ->
                        CoroutineScope(Dispatchers.IO).launch {
                            db.clearAllTables()
                            strankaDao.smazSekvence()
                            zobrazVsechnyData()
                        }
                    }.setNegativeButton("Ne") { dialog, id -> }.create().show()
                return true
            }

            R.id.agregujdb -> {
                val view =
                    layoutInflater.inflate(R.layout.db_entita_agregace, viewBinding.root, false)
                val dialog = Dialog(this)
                dialog.setContentView(view)
                val spinner = view.findViewById<Spinner>(R.id.spinnerSumFuncke)
                val okButton = view.findViewById<Button>(R.id.agregaceOkButton)
                val hodnotySpinneru = Array<String>(AgrFunkce.entries.size) { "" }
                for (i in AgrFunkce.entries.indices) {
                    hodnotySpinneru[i] = AgrFunkce.entries[i].nazev
                }
                ArrayAdapter(
                    this,
                    android.R.layout.simple_spinner_item,
                    hodnotySpinneru
                ).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
                    spinner.adapter = adapter
                }
                spinner.setSelection(0)
                okButton.setOnClickListener {
                    dialog.dismiss()
                    typAgr = AgrFunkce.entries[spinner.selectedItemPosition]
                    ActionModeDBEntita.actionMode?.finish()
                    zobrazKostruAgregacni(typAgr!!.kompTypy)
                    ActionModeDBEntita.actionMode = startActionMode(actionModeCallbackAgr)
                }
                dialog.show()
                return true
            }

            R.id.reloaddb -> {
                if (datumOd.isEmpty() && datumDo.isEmpty()) {
                    typAgr = null
                    ActionModeDBEntita.actionMode?.finish()
                    zobrazVsechnyData(desc = desc)
                } else {
                    delDatumButton.callOnClick()
                }
                return true
            }

            R.id.zmenserazeni -> {
                desc = !desc
                if (desc) {
                    item.setIcon(R.drawable.baseline_arrow_downward)
                } else {
                    item.setIcon(R.drawable.baseline_arrow_upward)
                }
                if (ActionModeDBEntita.actionMode == null) {
                    zobrazVsechnyData(
                        Converters.fromString(datumDo),
                        Converters.fromString(datumOd),
                        desc = this.desc
                    )
                }
                return true
            }

            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun zobrazVsechnyData(jako: String = "%", desc: Boolean = true) {
        val data = CoroutineScope(Dispatchers.IO).async {
            val stranky = strankaDao.vratVse(jako, desc)
            zpracujData(stranky)
        }
        CoroutineScope(Dispatchers.Main).launch {
            val set = data.await()
            title = set.size.toString()
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).setDataSet(set)
        }
    }

    private fun zobrazVsechnyData(
        pred: Date?,
        po: Date?,
        jako: String = "%",
        desc: Boolean = true
    ) {
        val data = CoroutineScope(Dispatchers.IO).async {
            val stranky: List<strankaSKotvami>
            if (pred != null && po != null) {
                stranky = strankaDao.vratVseMeziDaty(po, pred, jako, desc)
            } else if (pred != null) {
                stranky = strankaDao.vratVsePredDatem(pred, jako, desc)
            } else if (po != null) {
                stranky = strankaDao.vratVsePoDatu(po, jako, desc)
            } else {
                stranky = strankaDao.vratVse(jako, desc)
            }
            zpracujData(stranky)
        }
        CoroutineScope(Dispatchers.Main).launch {
            val set = data.await()
            title = set.size.toString()
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).setDataSet(set)
        }
    }

    private fun zobrazKostruAgregacni(typy: List<Typy>) {
        val temp = CoroutineScope(Dispatchers.IO).async {
            if (Typy.DATUM.instance.JeTimtoTypem(datumDo) && Typy.DATUM.instance.JeTimtoTypem(
                    datumOd
                )
            ) {
                strankaDao.vratNejnovKostryStranekMeziDaty(
                    Converters.fromString(datumOd)!!,
                    Converters.fromString(datumDo)!!
                )
            } else if (Typy.DATUM.instance.JeTimtoTypem(datumDo)) {
                strankaDao.vratNejnovKostryStranekPredDatem(Converters.fromString(datumDo)!!)
            } else if (Typy.DATUM.instance.JeTimtoTypem(datumOd)) {
                strankaDao.vratNejnovKostryStranekPoDatu(Converters.fromString(datumOd)!!)
            } else {
                strankaDao.vratNejnovKostryStranek()
            }
        }
        CoroutineScope(Dispatchers.Main).launch {
            val kostry = temp.await()
            title = kostry.size.toString()
            val dataSet = ArrayList<RecyclerViewDBEntita>()
            for (stranka in kostry) {
                val stran = ArrayList<RecyclerViewDBEntita>()
                for (kotva in stranka.kotvy) {
                    val kot = ArrayList<RecyclerViewDBEntita>()
                    for (hodnota in kotva.hodnoty) {
                        for (typ in typy) {
                            if (hodnota.typ == typ.typ) {
                                kot.add(
                                    RecyclerViewDBEntita(
                                        hodnota,
                                        strankaNazev = stranka.stranka.nazev,
                                        kotvaNazev = kotva.kotva.nazev
                                    )
                                )
                                break
                            }
                        }
                    }
                    if (!kot.isEmpty()) {
                        stran.add(RecyclerViewDBEntita(kotva.kotva, kot))
                    }
                }
                if (!stran.isEmpty()) {
                    dataSet.add(RecyclerViewDBEntita(stranka.stranka, stran, stranka.stranka.nazev))
                }
            }
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).setDataSet(dataSet)
        }
    }

    private fun zpracujData(data: List<strankaSKotvami>): ArrayList<RecyclerViewDBEntita> {
        val dataSet = ArrayList<RecyclerViewDBEntita>()
        for (stranka in data) {
            val stran = ArrayList<RecyclerViewDBEntita>()
            for (kotva in stranka.kotvy) {
                val kot = ArrayList<RecyclerViewDBEntita>()
                for (hodnota in kotva.hodnoty) {
                    kot.add(RecyclerViewDBEntita(hodnota))
                }
                stran.add(RecyclerViewDBEntita(kotva.kotva, kot))
            }
            for (hodnotaExtra in stranka.hodnotyExtra) {
                stran.add(RecyclerViewDBEntita(hodnotaExtra))
            }
            dataSet.add(RecyclerViewDBEntita(stranka.stranka, stran))
        }
        return dataSet
    }

    val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            mode?.menuInflater?.inflate(R.menu.zpracuj_data_action_mode_menu, menu)
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            return false
        }

        override fun onActionItemClicked(
            mode: ActionMode?,
            item: MenuItem?
        ): Boolean {
            return when (item?.itemId) {
                R.id.smaz -> {
                    AlertDialog.Builder(this@ZpracujDataActivity)
                        .setMessage("Doopravdy chcete smazat vybrané položky?")
                        .setPositiveButton("Ano") { dialog, id ->
                            (recyclerView.adapter as RecyclerViewAdapterDBEntity).smazVybrane()
                            mode?.finish()
                        }.setNegativeButton("Ne") { dialog, id -> mode?.finish() }.create().show()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            ActionModeDBEntita.actionMode = null
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).vycistiVybrane()
        }

    }

    val actionModeCallbackAgr = object : ActionMode.Callback {
        override fun onCreateActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            if (typAgr == null) {
                mode?.finish()
                return false
            }
            mode?.menuInflater?.inflate(R.menu.zpracuj_data_action_mode_agr_menu, menu)
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).vybraneAgr.clear()
            return true
        }

        override fun onPrepareActionMode(
            mode: ActionMode?,
            menu: Menu?
        ): Boolean {
            return false
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onActionItemClicked(
            mode: ActionMode?,
            item: MenuItem?
        ): Boolean {
            return when (item?.itemId) {
                R.id.agregujEntity -> {
                    title = ""
                    (recyclerView.adapter as RecyclerViewAdapterDBEntity).vycistiNevybraneAgr()
                    val temp = CoroutineScope(Dispatchers.IO).async {
                        for (item in (recyclerView.adapter as RecyclerViewAdapterDBEntity).vybraneAgr) {
                            if (typAgr == null) {
                                break
                            }
                            val hodnota: String
                            if (Typy.DATUM.instance.JeTimtoTypem(datumDo) && Typy.DATUM.instance.JeTimtoTypem(
                                    datumOd
                                )
                            ) {
                                if (typAgr == AgrFunkce.SUM) {
                                    hodnota = strankaDao.vratSUMMeziDaty(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumOd)!!,
                                        Converters.fromString(datumDo)!!
                                    ).toString()
                                } else {
                                    hodnota = strankaDao.vratAVGMeziDaty(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumOd)!!,
                                        Converters.fromString(datumDo)!!
                                    ).toString()
                                }
                            } else if (Typy.DATUM.instance.JeTimtoTypem(datumDo)) {
                                if (typAgr == AgrFunkce.SUM) {
                                    hodnota = strankaDao.vratSUMPredDatem(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumDo)!!
                                    ).toString()
                                } else {
                                    hodnota = strankaDao.vratAVGPredDatem(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumDo)!!
                                    ).toString()
                                }
                            } else if (Typy.DATUM.instance.JeTimtoTypem(datumOd)) {
                                if (typAgr == AgrFunkce.SUM) {
                                    hodnota = strankaDao.vratSUMPoDatu(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumOd)!!
                                    ).toString()
                                } else {
                                    hodnota = strankaDao.vratAVGPoDatu(
                                        item.strankaNazev, item.kotvaNazev, item.nazev,
                                        Converters.fromString(datumOd)!!
                                    ).toString()
                                }
                            } else {
                                if (typAgr == AgrFunkce.SUM) {
                                    hodnota = strankaDao.vratSUM(
                                        item.strankaNazev,
                                        item.kotvaNazev,
                                        item.nazev
                                    ).toString()
                                } else {
                                    hodnota = strankaDao.vratAVG(
                                        item.strankaNazev,
                                        item.kotvaNazev,
                                        item.nazev
                                    ).toString()
                                }
                            }
                            item.hodnota = hodnota
                        }
                    }
                    CoroutineScope(Dispatchers.Main).launch {
                        temp.await()
                        recyclerView.adapter?.notifyDataSetChanged()
                        ActionModeDBEntita.actionMode?.finish()
                        typAgr = null
                    }
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode?) {
            ActionModeDBEntita.actionMode = null
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).vycistiVybraneAgr()
        }

    }

}