package com.example.mobapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.mobapp.DB.DB
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.UI.ActionModeDBEntita
import com.example.mobapp.UI.RecyclerViewAdapterDBEntity
import com.example.mobapp.UI.RecyclerViewDBEntita
import com.example.mobapp.databinding.ZpracujDataActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ZpracujDataActivity : ComponentActivity() {
    private lateinit var viewBinding: ZpracujDataActivityBinding
    private lateinit var db: DB
    private lateinit var strankaDao: StrankaDao
    private lateinit var recyclerView: RecyclerView
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
        zobrazVsechnyData()
    }

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

            R.id.filterdb -> {
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun zobrazVsechnyData() {
        val data = CoroutineScope(Dispatchers.IO).async {
            val stranky = strankaDao.vratVse()
            val dataSet = ArrayList<RecyclerViewDBEntita>()
            for (stranka in stranky) {
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
            dataSet
        }
        CoroutineScope(Dispatchers.Main).launch {
            (recyclerView.adapter as RecyclerViewAdapterDBEntity).setDataSet(data.await())
        }
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
}