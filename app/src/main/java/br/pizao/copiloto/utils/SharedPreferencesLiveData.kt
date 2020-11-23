package br.pizao.copiloto.utils

import android.content.SharedPreferences
import androidx.lifecycle.LiveData

abstract class SharedPreferencesLiveData<T>(
    val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defValue: T
) : LiveData<T>() {

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == this.key) {
                value = getValueFromPreferences(key, defValue)
            }
        }

    abstract fun getValueFromPreferences(key: String, defValue: T): T

    override fun onActive() {
        super.onActive()
        value = getValueFromPreferences(key, defValue)
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    override fun onInactive() {
        super.onInactive()
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}

internal class SharedPreferenceBooleanLiveData(
    sharedPrefs: SharedPreferences,
    key: String,
    defValue: Boolean
) :
    SharedPreferencesLiveData<Boolean>(sharedPrefs, key, defValue) {
    override fun getValueFromPreferences(key: String, defValue: Boolean): Boolean =
        sharedPrefs.getBoolean(key, defValue)
}
