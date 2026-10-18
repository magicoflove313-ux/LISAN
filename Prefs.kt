package com.lisan.translator
import android.content.Context
object Prefs {
 private const val FILE = "lisan_settings"
 const val TARGET_LANGUAGE = "target_language"
 const val LAST_SOURCE_LANGUAGE = "last_source_language"
 fun get(c: Context, k: String, d: String = "") = c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString(k,d) ?: d
 fun put(c: Context, k: String, v: String) { c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(k,v).apply() }
}
