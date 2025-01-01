package com.example.mobapp

import android.app.DatePickerDialog
import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.EditText
import androidx.core.text.isDigitsOnly
import com.example.mobapp.DB.Converters
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Calendar

/* Pouziji to k odfiltrovani spatnych hodnot a databazovani, ne k zlepseni/zrychleni prirazovani */
interface TypyHodnot {
    fun VratDefHodnotu(): String
    fun JeTimtoTypem(str: String): Boolean
    fun VratView(context: Context, hodnota: String): EditText
    fun ZpracujView(view: EditText, context: Context)
}

@Serializable
enum class Typy(val instance: TypyHodnot, val typ: Int) {
    @SerialName("text")
    TEXT(TypText(), 1),

    @SerialName("cislo")
    CISLO(TypCislo(), 0),

    @SerialName("decimal")
    DECIMAL(TypDecimal(), 3),

    @SerialName("procento")
    PROCENTO(TypProcento(), 4),

    @SerialName("datum")
    DATUM(TypDatum(), 2),

    @SerialName("frakce")
    FRAKCE(TypFrakce(), 5)
}

class TypText : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return ""
    }

    override fun JeTimtoTypem(str: String): Boolean {
        return true
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        temp.inputType = InputType.TYPE_CLASS_TEXT
        if (hodnota.isEmpty()) {
            temp.setText(VratDefHodnotu())
        } else {
            temp.setText(hodnota)
        }
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.FOCUSABLE
        view.inputType = InputType.TYPE_CLASS_TEXT
    }
}

class TypCislo : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        return !str.isEmpty() && str.isDigitsOnly()
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        temp.inputType = InputType.TYPE_CLASS_NUMBER
        if (hodnota.isEmpty()) {
            temp.setText(VratDefHodnotu())
        } else {
            temp.setText(hodnota)
        }
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.FOCUSABLE
        view.inputType = InputType.TYPE_CLASS_NUMBER
    }
}

class TypDecimal : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0,0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        val temp = str.split(",")
        if (temp.size != 2) {
            return false
        }
        return temp[0].isDigitsOnly() && temp[1].isDigitsOnly()
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        temp.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (hodnota.isEmpty()) {
            temp.setText(VratDefHodnotu())
        } else {
            temp.setText(hodnota)
        }
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.FOCUSABLE
        view.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
}

class TypProcento : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0.0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        val temp = str.split(".")
        if (temp.size != 2) {
            return false
        }
        return temp[0].isDigitsOnly() && temp[1].removeSuffix("%").isDigitsOnly()
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        temp.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
        if (hodnota.isEmpty()) {
            temp.setText(VratDefHodnotu())
        } else {
            temp.setText(hodnota)
        }
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.FOCUSABLE
        view.inputType = InputType.TYPE_NUMBER_FLAG_DECIMAL
    }
}

class TypDatum : TypyHodnot {
    override fun VratDefHodnotu(): String {
        val calendar = Calendar.getInstance()
        var den = calendar.get(Calendar.DAY_OF_MONTH).toString()
        if (den.length == 1) {
            den = "0${den}"
        }
        var mesic = (calendar.get(Calendar.MONTH) + 1).toString()
        if (mesic.length == 1) {
            mesic = "0${mesic}"
        }
        return "${den}.${mesic}.${calendar.get(Calendar.YEAR)}"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        val temp = str.split(".")
        if (temp.size != 3) {
            return false
        }
        if (temp[0].length == 2 && temp[1].length == 2 && temp[2].length == 4 && temp[0].isDigitsOnly() && temp[1].isDigitsOnly() && temp[2].isDigitsOnly()) {
            val rok = temp[2].toInt()
            val mesic = temp[1].toInt()
            val den = temp[0].toInt()
            if (rok < 0) {
                return false
            }
            if (mesic < 1 || mesic > 12) {
                return false
            }
            if (den < 1 || den > 31) {
                return false
            }
            if (mesic == 2 && den == 29) {
                if ((rok % 4 == 0 && rok % 100 != 0) || rok % 400 == 0) {
                    return true
                }
                return false
            }
            if (mesic == 2 && den > 28) {
                return false
            }
            if (mesic == 4 || mesic == 6 || mesic == 9 || mesic == 11) {
                return den < 31
            }
            return true
        }
        return false
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        if (JeTimtoTypem(hodnota)) {
            temp.setText(hodnota)
        } else {
            temp.setText(VratDefHodnotu())
        }
        setniDatumPopUp(temp, context)
        temp.focusable = View.NOT_FOCUSABLE
        temp.inputType = InputType.TYPE_NULL
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.NOT_FOCUSABLE
        view.inputType = InputType.TYPE_NULL
        setniDatumPopUp(view, context)
    }

    private fun setniDatumPopUp(view: EditText, context: Context) {
        val listener = DatePickerDialog.OnDateSetListener { datePicker, year, month, day ->
            val calendar = Calendar.getInstance()
            calendar.set(year, month, day)
            view.setText(Converters.dateToString(calendar.time))
        }
        view.setOnClickListener {
            val hodnoty: List<String>
            if (JeTimtoTypem(view.text.toString())) {
                hodnoty = view.text.toString().split(".")

            } else {
                hodnoty = VratDefHodnotu().split(".")
            }
            DatePickerDialog(
                context,
                listener,
                hodnoty[2].toInt(),
                hodnoty[1].toInt() - 1,
                hodnoty[0].toInt()
            ).show()
        }
    }
}

class TypFrakce : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0/0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        val temp = str.split("/")
        if (temp.size != 2) {
            return false
        }
        return temp[0].isDigitsOnly() && temp[1].isDigitsOnly()
    }

    override fun VratView(context: Context, hodnota: String): EditText {
        val temp = EditText(context)
        temp.inputType = InputType.TYPE_CLASS_TEXT
        if (hodnota.isEmpty()) {
            temp.setText(VratDefHodnotu())
        } else {
            temp.setText(hodnota)
        }
        return temp
    }

    override fun ZpracujView(view: EditText, context: Context) {
        view.focusable = View.FOCUSABLE
        view.inputType = InputType.TYPE_CLASS_TEXT
    }
}