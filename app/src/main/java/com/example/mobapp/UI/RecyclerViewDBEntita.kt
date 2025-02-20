package com.example.mobapp.UI

import android.annotation.SuppressLint
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import com.example.mobapp.DB.Converters
import com.example.mobapp.DB.DBEntita
import com.example.mobapp.DB.DBHodnota
import com.example.mobapp.DB.DBHodnotaExtra
import com.example.mobapp.DB.DBKotva
import com.example.mobapp.DB.DBStranka
import com.example.mobapp.DB.StrankaDao
import com.example.mobapp.FunkceSpinave
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
    var editing = false

    init {
        if (entita is DBHodnota) {
            barva = R.color.md_theme_primary.toInt()
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
                    break
                }
            }
        } else if (entita is DBHodnotaExtra) {
            barva = R.color.md_theme_onPrimaryContainer.toInt()
            editable = true
            deletable = false
            expandable = false
            nazev = entita.nazev
            hodnota = entita.hodnota
            for (Typ in Typy.entries) {
                if (Typ.typ == entita.typ) {
                    typ = Typ
                    break
                }
            }
        } else if (entita is DBKotva) {
            barva = R.color.md_theme_primaryContainer
            editable = false
            deletable = false
            expandable = true
            nazev = entita.nazev
            hodnota = null
        } else {
            barva = R.color.md_theme_tertiaryContainer.toInt()
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
        view.setBackgroundColor(activity.resources.getColor(barva, null))
        nazev.setTextColor(VratOpakBarvy(activity.resources.getColor(barva, null)).toArgb())
        hodnota.setTextColor(VratOpakBarvy(activity.resources.getColor(barva, null)).toArgb())
        view.setOnClickListener(null)
        view.setOnLongClickListener(null)
        editButton.setOnClickListener(null)
        nazev.text = this.nazev
        if (this.hodnota == null) {
            hodnota.visibility = View.GONE
        } else {
            hodnota.visibility = View.VISIBLE
            hodnota.setText(this.hodnota)
        }
        hodnota.hint = null
        hodnota.paintFlags = 0
        hodnota.focusable = View.NOT_FOCUSABLE
        hodnota.inputType = InputType.TYPE_NULL
        hodnota.setError(null)
        hodnota.setOnClickListener(null)
        setSelected(this.selected, view, activity)
        if (deletable) {
            view.setOnLongClickListener { view ->
                if (selected) {
                    return@setOnLongClickListener true
                }
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
            if (editing) {
                if (typ != null) {
                    typ!!.instance.ZpracujView(hodnota, activity)
                    hodnota.hint = typ!!.instance.VratDefHodnotu()
                    if (hodnota.hint.length < 8) {
                        hodnota.hint =
                            hodnota.hint.toString() + (" ".repeat(8 - hodnota.hint.length))
                    }
                    hodnota.paintFlags = Paint.UNDERLINE_TEXT_FLAG
                }
                editButton.setImageResource(R.drawable.baseline_done)
                editButton.setOnClickListener {
                    if (typ != null && FunkceSpinave.JeVyplnenoSpravne(hodnota, typ!!)) {
                        if (typ == Typy.PROCENTO || typ == Typy.DECIMAL) {
                            val temp = hodnota.text.toString().replace(",", ".").removeSuffix("%")
                            this.hodnota = temp
                            if (entita is DBHodnota) {
                                entita.hodnota = temp

                            } else if (entita is DBHodnotaExtra) {
                                entita.hodnota = temp
                            }
                        } else {
                            this.hodnota = hodnota.text.toString()
                            if (entita is DBHodnota) {
                                entita.hodnota = hodnota.text.toString()
                            } else if (entita is DBHodnotaExtra) {
                                entita.hodnota = hodnota.text.toString()
                            }
                        }
                        CoroutineScope(Dispatchers.IO).launch {
                            if (entita is DBHodnota) {
                                activity.strankaDao.insertHodnota(entita)
                            } else if (entita is DBHodnotaExtra) {
                                activity.strankaDao.insertHodnotaExtra(entita)
                            }
                        }
                        editing = false
                        recyclerViewAdapterDBEntity.notifikujZmenu(
                            recyclerViewAdapterDBEntity.indexOf(
                                this
                            )
                        )
                    }
                }
            } else {
                editButton.setImageResource(R.drawable.baseline_build)
                editButton.setOnClickListener {
                    editing = true
                    recyclerViewAdapterDBEntity.notifikujZmenu(
                        recyclerViewAdapterDBEntity.indexOf(
                            this
                        )
                    )
                }
            }
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
                if (selected) {
                    return@setOnLongClickListener true
                }
                setSelected(true, view, activity)
                if (ActionModeDBEntita.actionMode == null) {
                    ActionModeDBEntita.actionMode =
                        activity.startActionMode(activity.actionModeCallbackAgr)
                }
                recyclerViewAdapterDBEntity.addToVybraneAgr(this)
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

    private fun VratOpakBarvy(barva: Int): Color {
        return Color(255 - barva.red, 255 - barva.green, 255 - barva.blue, 255)
    }
}