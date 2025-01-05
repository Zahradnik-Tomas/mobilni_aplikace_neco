package com.example.mobapp.UI

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.R
import com.example.mobapp.ZpracujDataActivity
import com.example.mobapp.databinding.ZpracujDataActivityBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RecyclerViewAdapterDBEntity(
    private var dataSet: ArrayList<RecyclerViewDBEntita>,
    private val viewBinding: ZpracujDataActivityBinding,
    private val activity: ZpracujDataActivity,
    private val strankaDao: StrankaDao
) :
    RecyclerView.Adapter<RecyclerViewAdapterDBEntity.ViewHolder>() {

    private val vybrane = ArrayList<RecyclerViewDBEntita>()
    public val vybraneAgr = ArrayList<RecyclerViewDBEntita>()

    val mutex = Mutex()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.db_entita_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {
        dataSet[position].ZpracujView(
            holder.view,
            holder.nazev,
            holder.hodnota,
            holder.editButton,
            holder.sipkaDolu,
            holder.sipkaVpravo,
            this,
            activity
        )
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    public fun setDataSet(dataSet: ArrayList<RecyclerViewDBEntita>) {
        viewBinding.recycler.post {
            SetDataSet(dataSet)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun SetDataSet(dataSet: ArrayList<RecyclerViewDBEntita>) {
        this.dataSet = dataSet
        notifyDataSetChanged()
    }

    public fun removeData(data: ArrayList<RecyclerViewDBEntita>, position: Int) {
        viewBinding.recycler.post {
            RemoveData(data, position)
        }
    }

    private fun RemoveData(
        data: ArrayList<RecyclerViewDBEntita>,
        position: Int,
        dite: Boolean = false
    ): Int {
        var removed = 0
        for (item in data.indices.reversed()) {
            if (data[item].deti != null && data[item].expanded) {
                removed += RemoveData(data[item].deti!!, position + item + 1, true)
            }
        }
        if (dataSet.removeAll(data)) {
            if (dite) {
                return data.size
            }
            notifyItemRangeRemoved(position + 1, data.size + removed)
        }
        return 0
    }

    public fun addData(data: ArrayList<RecyclerViewDBEntita>, position: Int) {
        viewBinding.recycler.post {
            AddData(data, position)
        }
    }

    public fun smazVybrane() {
        CoroutineScope(Dispatchers.IO).launch {
            mutex.lock()
            viewBinding.recycler.post {
                SmazVybrane()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun SmazVybrane() {
        if (activity.title.isDigitsOnly()) {
            activity.title = (activity.title.toString().toInt() - vybrane.size).toString()
        }
        for (item in vybrane) {
            item.Smaz(strankaDao)
            if (item.expanded && item.deti != null) {
                RemoveData(item.deti, indexOf(item))
            }
        }
        dataSet.removeAll(vybrane)
        vybrane.clear()
        if (mutex.isLocked) {
            mutex.unlock()
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    public fun vycistiVybrane() {
        CoroutineScope(Dispatchers.Main).launch {
            mutex.withLock { ->
                for (item in vybrane) {
                    item.SetSelected(false)
                }
                notifyDataSetChanged()
                vybrane.clear()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public fun vycistiVybraneAgr() {
        for (item in vybraneAgr) {
            item.SetSelected(false)
        }
        notifyDataSetChanged()
        vybraneAgr.clear()
    }

    @SuppressLint("NotifyDataSetChanged")
    public fun vycistiNevybraneAgr() {
        for (item in dataSet.toList()) {
            if (!item.strankaNazev.isEmpty() && item.kotvaNazev.isEmpty()) {
                if (item.deti == null) {
                    dataSet.remove(item)
                    continue
                }
                var selected = false
                for (kotva in item.deti.toList()) {
                    var selectedKotv = false
                    kotva.deti?.let {
                        for (hodnota in kotva.deti.toList()) {
                            if (hodnota.selected) {
                                selected = true
                                selectedKotv = true
                                hodnota.SetSelected(false)
                                hodnota.selectable = false
                            } else {
                                vybraneAgr.remove(hodnota)
                                dataSet.remove(hodnota)
                                kotva.deti.remove(hodnota)
                            }
                        }
                        if (!selectedKotv) {
                            removeData(kotva.deti, indexOf(kotva))
                        }
                    }
                    if (!selectedKotv) {
                        dataSet.remove(kotva)
                        item.deti.remove(kotva)
                    }
                }
                if (!selected) {
                    removeData(item.deti, indexOf(item))
                    dataSet.remove(item)
                }
            }
        }
        notifyDataSetChanged()
    }

    public fun addToVybrane(item: RecyclerViewDBEntita) {
        if (ActionModeDBEntita.actionMode == null) {
            return
        }
        vybrane.add(item)
        aktualizujTitle()
    }

    public fun addToVybraneAgr(item: RecyclerViewDBEntita) {
        if (ActionModeDBEntita.actionMode == null) {
            return
        }
        vybraneAgr.add(item)
        aktualizujTitle(vybraneAgr, false)
    }

    public fun removeFromVybrane(item: RecyclerViewDBEntita) {
        if (ActionModeDBEntita.actionMode == null) {
            return
        }
        vybrane.remove(item)
        aktualizujTitle()
    }

    public fun removeFromVybraneAgr(item: RecyclerViewDBEntita) {
        if (ActionModeDBEntita.actionMode == null) {
            return
        }
        vybraneAgr.remove(item)
        aktualizujTitle(vybraneAgr, false)
    }

    public fun notifikujZmenu(pozice: Int) {
        viewBinding.recycler.post {
            notifyItemChanged(pozice)
        }
    }

    private fun aktualizujTitle(
        listVybr: ArrayList<RecyclerViewDBEntita> = this.vybrane,
        konciSNulou: Boolean = true
    ) {
        if (listVybr.isEmpty() && konciSNulou) {
            ActionModeDBEntita.actionMode?.finish()
            return
        }
        ActionModeDBEntita.actionMode?.title = listVybr.size.toString()
    }

    private fun AddData(
        data: ArrayList<RecyclerViewDBEntita>,
        position: Int,
        dite: Boolean = false
    ): Int {
        var added = 0
        for (item in data.indices.reversed()) {
            dataSet.add(position + 1, data[item])
            added += 1
            if (data[item].expanded && data[item].deti != null) {
                added += AddData(data[item].deti!!, position + 1, true)
            }
        }
        if (dite) {
            return added
        }
        notifyItemRangeInserted(position + 1, added)
        return added
    }

    public fun indexOf(item: RecyclerViewDBEntita): Int {
        return dataSet.indexOf(item)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nazev: TextView
        val hodnota: EditText
        val editButton: ImageButton
        val sipkaDolu: ImageView
        val sipkaVpravo: ImageView
        val view: View

        init {
            nazev = view.findViewById(R.id.entita_nazev)
            hodnota = view.findViewById(R.id.entita_hodnota)
            editButton = view.findViewById(R.id.entita_edit)
            sipkaDolu = view.findViewById(R.id.entita_sipka_dolu)
            sipkaVpravo = view.findViewById(R.id.entita_sipka_vpravo)
            this.view = view
        }
    }
}