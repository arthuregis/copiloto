package br.pizao.copiloto.utils.persistence

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import br.pizao.copiloto.utils.Constants.SERVICE_MESSAGE_INDEX

object Preferences {
    private lateinit var sharedPrefs: SharedPreferences

    private const val SHARED_NAME = "br.pizao.copilot.sharedPrefs"

    fun init(context: Context) {
        if (!Preferences::sharedPrefs.isInitialized) {
            sharedPrefs = context.getSharedPreferences(SHARED_NAME, Context.MODE_PRIVATE)
        }
        resetInt(SERVICE_MESSAGE_INDEX)
    }

    fun stringLiveData(
        key: String,
        defValue: String = ""
    ): SharedPreferencesLiveData<String> {
        return SharedPreferenceStringLiveData(sharedPrefs, key, defValue)
    }

    fun putString(key: String, value: String) {
        sharedPrefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, defValue: Boolean = false): Boolean {
        return sharedPrefs.getBoolean(key, defValue)
    }

    fun booleanLiveData(
        key: String,
        defValue: Boolean = false
    ): SharedPreferencesLiveData<Boolean> {
        return SharedPreferenceBooleanLiveData(sharedPrefs, key, defValue)
    }

    fun putBoolean(key: String, value: Boolean) {
        sharedPrefs.edit().putBoolean(key, value).apply()
    }

    fun getInt(key: String, defValue: Int = 0): Int {
        return sharedPrefs.getInt(key, defValue)
    }

    @SuppressLint("ApplySharedPref")
    fun resetInt(key: String) {
        sharedPrefs.edit().putInt(key, 0).commit()
    }

    @SuppressLint("ApplySharedPref")
    fun incrementInt(key: String) {
        sharedPrefs.edit().putInt(key, getInt(key).plus(1)).commit()
    }
}