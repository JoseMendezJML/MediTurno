package com.example.mediturno.shared.data

import android.content.Context

class DatabaseUrlStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences("mediturno_config", Context.MODE_PRIVATE)

    fun getUrl(): String = preferences.getString(KEY_DATABASE_URL, "")?.trim().orEmpty()

    fun saveUrl(url: String) {
        preferences.edit()
            .putString(KEY_DATABASE_URL, normalize(url))
            .apply()
    }

    private fun normalize(value: String): String =
        value.trim().removeSuffix("/")

    companion object {
        private const val KEY_DATABASE_URL = "database_url"
    }
}
