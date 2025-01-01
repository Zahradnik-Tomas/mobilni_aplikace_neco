package com.example.mobapp.DB

import androidx.room.TypeConverter
import com.example.mobapp.Typy
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time?.toLong()
    }

    companion object {
        fun fromString(value: String): Date? {
            if (!Typy.DATUM.instance.JeTimtoTypem(value)) {
                return null
            }
            val temp = value.split(".")
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("GMT")
            calendar.clear()
            calendar.set(temp[2].toInt(), temp[1].toInt() - 1, temp[0].toInt())
            return calendar.time
        }

        fun dateToString(date: Date): String {
            val calendar = Calendar.getInstance()
            calendar.timeZone = TimeZone.getTimeZone("GMT")
            calendar.time = date
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
    }
}