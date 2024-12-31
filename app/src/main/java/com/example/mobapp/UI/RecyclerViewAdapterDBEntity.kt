package com.example.mobapp.UI

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mobapp.R
import com.example.mobapp.databinding.ZpracujDataActivityBinding

class RecyclerViewAdapterDBEntity(
    private var dataSet: ArrayList<RecyclerViewDBEntita>,
    private val viewBinding: ZpracujDataActivityBinding
) :
    RecyclerView.Adapter<RecyclerViewAdapterDBEntity.ViewHolder>() {

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
            position
        )
    }

    override fun getItemCount(): Int {
        return dataSet.size
    }

    @SuppressLint("NotifyDataSetChanged")
    public fun setDataSet(dataSet: ArrayList<RecyclerViewDBEntita>) {
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