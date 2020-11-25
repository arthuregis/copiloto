package br.pizao.copiloto.utils.persistence

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    lateinit var sharedPrefs: SharedPreferences

    private const val SHARED_NAME = "br.pizao.copilot.sharedPrefs"

    fun init(context: Context) {
        if(!Preferences::sharedPrefs.isInitialized) {
            sharedPrefs = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getBoolean(key:String, defValue: Boolean = false): Boolean {
        return sharedPrefs.getBoolean(key, defValue)
    }

    fun booleanLiveData(key: String, defValue: Boolean = false): SharedPreferencesLiveData<Boolean> {
        return SharedPreferenceBooleanLiveData(sharedPrefs, key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getString(key:String, defValue: String = ""): String {
        return sharedPrefs.getString(key, defValue) ?: ""
    }

    fun stringLiveData(key: String, defValue: String = ""): SharedPreferencesLiveData<String> {
        return SharedPreferenceStringLiveData(sharedPrefs, key, defValue)
    }

    fun putString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }
}