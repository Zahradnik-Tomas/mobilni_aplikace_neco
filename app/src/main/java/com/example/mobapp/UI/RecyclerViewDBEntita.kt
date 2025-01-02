package com.example.mobapp.UI

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DBEntita
import com.example.mobapp.DB.DBHodnota
import com.example.mobapp.DB.DBHodnotaExtra
import com.example.mobapp.DB.DBKotva
import com.example.mobapp.DB.DBStranka
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.Typy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.mobapp.R
import com.example.mobapp.ZpracujDataActivity

class RecyclerViewDBEntita(
    val entita: DBEntita,
    val deti: ArrayList<RecyclerViewDBEntita>? = null,
    val strankaNazev: String = "",
    val kotvaNazev: String = ""
) {
    val barva: Int
    val editable: Boolean
    val deletable: Boolean
    val expandable: Boolean
    val nazev: String
    var hodnota: String?
    var typ: Typy? = null
    var expanded = false
    var selected = false
    var selectable = false

    init {
        if (entita is DBHodnota) {
            barva = Color.GRAY
            if (!strankaNazev.isEmpty()) {
                if (kotvaNazev.isEmpty()) {
                    throw IllegalArgumentException("Nazev kotvy je prazdny")
                }
                editable = false
                hodnota = ""
                selectable = true
            } else {
                editable = true
                hodnota = entita.hodnota
            }
            deletable = false
            expandable = false
            nazev = entita.nazev
            for (Typ in Typy.entries) {
                if (Typ.typ == entita.typ) {
                    this.typ = Typ
                }
            }
        } else if (entita is DBHodnotaExtra) {
            barva = Color.DKGRAY
            editable = true
            deletable = false
            expandable = false
            nazev = entita.nazev
            hodnota = entita.hodnota
            for (Typ in Typy.entries) {
                if (Typ.typ == entita.typ) {
                    typ = Typ
                }
            }
        } else if (entita is DBKotva) {
            barva = Color.CYAN
            editable = false
            deletable = false
            expandable = true
            nazev = entita.nazev
            hodnota = null
        } else {
            barva = R.color.sediva
            editable = false
            nazev = (entita as DBStranka).nazev
            if (!strankaNazev.isEmpty()) {
                deletable = false
                hodnota = ""
            } else {
                deletable = true
                hodnota = Converters.dateToString(entita.datum)
            }
            expandable = true
            typ = Typy.DATUM
        }
    }

    public fun ZpracujView(
        view: View,
        nazev: TextView,
        hodnota: EditText,
        editButton: ImageButton,
        sipkaDolu: ImageView,
        sipkaVpravo: ImageView,
        recyclerViewAdapterDBEntity: RecyclerViewAdapterDBEntity,
        activity: ZpracujDataActivity
    ) {
        view.setBackgroundColor(barva)
        view.setOnClickListener(null)
        view.setOnLongClickListener(null)
        nazev.text = this.nazev
        if (this.hodnota == null) {
            hodnota.visibility = View.GONE
        } else {
            hodnota.visibility = View.VISIBLE
            hodnota.setText(this.hodnota)
        }
        hodnota.focusable = View.NOT_FOCUSABLE
        hodnota.inputType = InputType.TYPE_NULL
        hodnota.setOnClickListener(null)
        setSelected(this.selected, view, activity)
        if (deletable) {
            view.setOnLongClickListener { view ->
                setSelected(true, view, activity)
                if (ActionModeDBEntita.actionMode == null) {
                    ActionModeDBEntita.actionMode =
                        activity.startActionMode(activity.actionModeCallback)
                }
                recyclerViewAdapterDBEntity.addToVybrane(this)
                true
            }
        }
        if (editable) {
            editButton.visibility = View.VISIBLE
        } else {
            editButton.visibility = View.GONE
        }
        sipkaDolu.visibility = View.GONE
        sipkaVpravo.visibility = View.GONE
        if (expandable) {
            view.setOnClickListener {
                if (selected) {
                    setSelected(false, view, activity)
                    recyclerViewAdapterDBEntity.removeFromVybrane(this)
                } else {
                    HandleClickExpand(sipkaDolu, sipkaVpravo, recyclerViewAdapterDBEntity)
                }
            }
            hodnota.setOnClickListener {
                if (selected) {
                    setSelected(false, view, activity)
                    recyclerViewAdapterDBEntity.removeFromVybrane(this)
                } else {
                    HandleClickExpand(sipkaDolu, sipkaVpravo, recyclerViewAdapterDBEntity)
                }
            }
            if (expanded) {
                sipkaDolu.visibility = View.VISIBLE
            } else {
                sipkaVpravo.visibility = View.VISIBLE
            }
        }
        if (selectable && entita is DBHodnota) {
            view.setOnLongClickListener { view ->
                setSelected(true, view, activity)
                recyclerViewAdapterDBEntity.addToVybraneAgr(this)
                if (ActionModeDBEntita.actionMode == null) {
                    ActionModeDBEntita.actionMode =
                        activity.startActionMode(activity.actionModeCallbackAgr)
                }
                true
            }
            view.setOnClickListener {
                if (selected) {
                    setSelected(false, view, activity)
                    recyclerViewAdapterDBEntity.removeFromVybraneAgr(this)
                }
            }
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun setSelected(boolean: Boolean, view: View, activity: ZpracujDataActivity) {
        this.selected = boolean
        if (selected) {
            view.foreground = activity.getDrawable(R.drawable.border)
        } else {
            view.foreground = null
        }
    }

    public fun SetSelected(boolean: Boolean) {
        this.selected = boolean
    }

    public fun HandleClickExpand(
        sipkaDolu: ImageView,
        sipkaVpravo: ImageView,
        recyclerViewAdapterDBEntity: RecyclerViewAdapterDBEntity
    ) {
        expanded = !expanded
        sipkaDolu.visibility = View.GONE
        sipkaVpravo.visibility = View.GONE
        if (expanded) {
            sipkaDolu.visibility = View.VISIBLE
        } else {
            sipkaVpravo.visibility = View.VISIBLE
        }
        recyclerViewAdapterDBEntity.notifyItemChanged(recyclerViewAdapterDBEntity.indexOf(this))
        if (deti == null) {
            return
        }
        if (expanded) {
            recyclerViewAdapterDBEntity.addData(deti, recyclerViewAdapterDBEntity.indexOf(this))
        } else {
            recyclerViewAdapterDBEntity.removeData(deti, recyclerViewAdapterDBEntity.indexOf(this))
        }

    }

    public fun Smaz(strankaDao: StrankaDao) {
        CoroutineScope(Dispatchers.IO).launch {
            if (entita is DBStranka) {
                strankaDao.delete(entita)
            } else if (entita is DBKotva) {
                strankaDao.delete(entita)
            } else if (entita is DBHodnota) {
                strankaDao.delete(entita)
            } else if (entita is DBHodnotaExtra) {
                strankaDao.delete(entita)
            }
        }
        if (deti != null) {
            for (item in deti) {
                item.Smaz(strankaDao)
            }
        }
    }
}