package br.pizao.copiloto

import android.app.Application
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.manager.CopilotoAudioManager
import br.pizao.copiloto.utils.helpers.IntentHelper
import br.pizao.copiloto.utils.persistence.Preferences
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Preferences.init(this)
        CopilotoAudioManager.init(this)
        IntentHelper.init(this)
        ChatRepository.init(this)

        GlobalScope.launch { ChatRepository.clearDatabase() }
    }
}