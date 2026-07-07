package com.example.topbooks.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromStringMap(value: Map<String, List<String>>?): String = gson.toJson(value ?: emptyMap<String, List<String>>())

    @TypeConverter
    fun toStringMap(value: String): Map<String, List<String>> {
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromBooleanMap(value: Map<String, Boolean>?): String = gson.toJson(value ?: emptyMap<String, Boolean>())

    @TypeConverter
    fun toBooleanMap(value: String): Map<String, Boolean> {
        val type = object : TypeToken<Map<String, Boolean>>() {}.type
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time
}
