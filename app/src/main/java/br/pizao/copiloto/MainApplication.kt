package br.pizao.copiloto

import android.app.Application
import br.pizao.copiloto.manager.CopilotoAudioManager
import br.pizao.copiloto.utils.Preferences

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Preferences.init(this)
        CopilotoAudioManager.init(this)
    }
}