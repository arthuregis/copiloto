package br.pizao.copiloto.utils

import android.content.Context
import android.content.SharedPreferences

object Preferences {
    lateinit var sharedPrefs: SharedPreferences

    const val SHARED_NAME = "br.pizao.copilot.sharedPrefs"
    const val CAMERA_STATUS = "camera_status"
    const val TTS_ENABLED = "tts_status"

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
}