package br.pizao.copiloto

import android.app.Application
import br.pizao.copiloto.manager.AudioManager
import br.pizao.copiloto.utils.Preferences

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Preferences.init(this)
        AudioManager.init(this)
    }
}