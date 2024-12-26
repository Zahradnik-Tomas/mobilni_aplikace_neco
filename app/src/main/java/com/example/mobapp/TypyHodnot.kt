package com.example.mobapp

import androidx.core.text.isDigitsOnly
import java.util.Calendar

/* Pouziji to k odfiltrovani spatnych hodnot a databazovani, ne k zlepseni/zrychleni prirazovani */
interface TypyHodnot {
    fun VratDefHodnotu(): String
    fun JeTimtoTypem(str: String): Boolean
}

object DrzecTypu {
    /*inb4 enum*/
    val ListTypu = arrayOf(
        TypCislo(), TypText(), TypDatum(), TypDecimal(), TypProcento(),
        TypFrakce()
    )
}

class TypText : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return ""
    }

    override fun JeTimtoTypem(str: String): Boolean {
        return true
    }
}

class TypCislo : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        return !str.isEmpty() && str.isDigitsOnly()
    }
}

class TypDecimal : TypyHodnot {
    override fun VratDefHodnotu(): String {
        return "0.0"
    }

    override fun JeTimtoTypem(str: String): Boolean {
        val temp = str.split(",")
        if (temp.size != 2) {
            return false
        }
        return temp[0].isDigitsOnly() && temp[1].isDigitsOnly()
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
        return temp[0].length == 2 && temp[1].length == 2 && temp[2].length == 4 && temp[0].isDigitsOnly() && temp[1].isDigitsOnly() && temp[2].isDigitsOnly()
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

}