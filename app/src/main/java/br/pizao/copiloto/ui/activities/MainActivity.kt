package br.pizao.copiloto.ui.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import br.pizao.copiloto.MainApplication
import br.pizao.copiloto.R
import br.pizao.copiloto.database.ChatRepository
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.database.model.ConfirmationAction
import br.pizao.copiloto.database.model.MessageType
import br.pizao.copiloto.databinding.MainActivityBinding
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.ui.dialog.CameraDialog
import br.pizao.copiloto.ui.view.ChatMessageAdapter
import br.pizao.copiloto.ui.viewmodel.MainActivityViewModel
import br.pizao.copiloto.utils.Constants.CAMERA_ON_BACKGROUND
import br.pizao.copiloto.utils.Constants.CAMERA_PREVIEW_ACTION
import br.pizao.copiloto.utils.Constants.CAMERA_STATUS
import br.pizao.copiloto.utils.Constants.NEGATIVE_ANSWER_ACTION
import br.pizao.copiloto.utils.Constants.PERMISSION_REQUEST_CODE
import br.pizao.copiloto.utils.Constants.POSITIVE_ANSEWR_ACTION
import br.pizao.copiloto.utils.Constants.TTS_DATA_CHECK_CODE
import br.pizao.copiloto.utils.Constants.TTS_ENABLED
import br.pizao.copiloto.utils.extensions.isCopilotoServiceRunning
import br.pizao.copiloto.utils.helpers.IntentHelper
import br.pizao.copiloto.utils.helpers.Permissions
import br.pizao.copiloto.utils.persistence.Preferences
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(MainActivityViewModel::class.java) }
    private lateinit var binding: MainActivityBinding
    private val chatAdapter = ChatMessageAdapter()
    private var lastIndexAdded = 0
    private val lock = Object()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.main_activity)
        binding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = this@MainActivity.viewModel
            recyclerView.adapter = chatAdapter
        }

        setListeners()
        registerBroadCast()

        val permissions = Permissions(this)

        if (permissions.isNotAllGranted()) {
            permissions.requestMissing()
        }

        binding.cameraSwitch.isEnabled = permissions.isGranted(Manifest.permission.CAMERA)

        checkTTS()

        scheduleInitialConversation()
    }

    override fun onResume() {
        super.onResume()

        if (!isCopilotoServiceRunning()) {
            Preferences.putBoolean(CAMERA_STATUS, false)
            Preferences.putBoolean(CAMERA_ON_BACKGROUND, false)
        }
    }

    override fun onDestroy() {
        unregisterBroadCast()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                permissions.forEachIndexed { index, s ->
                    when (s) {
                        Manifest.permission.CAMERA -> {
                            if (grantResults[index] == PackageManager.PERMISSION_GRANTED) {
                                binding.cameraSwitch.isEnabled = true
                            } else {
                                Toast.makeText(
                                    this,
                                    getString(R.string.camera_permission_not_granted),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                        Manifest.permission.RECORD_AUDIO -> {
                            if (grantResults[index] != PackageManager.PERMISSION_GRANTED) {
                                Toast.makeText(
                                    this,
                                    getString(R.string.audio_record_permission_not_granted),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            TTS_DATA_CHECK_CODE -> {
                if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                    Preferences.putBoolean(TTS_ENABLED, true)
                } else {
                    Intent().apply { action = TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA }.also {
                        startActivity(it)
                    }
                    Preferences.putBoolean(TTS_ENABLED, false)
                }
            }
        }
    }

    private fun setListeners() {
        binding.cameraSwitch.setOnClickListener {
            it as SwitchMaterial
            if (it.isChecked) {
                openCameraDialog()
            } else {
                stopCamera()
            }
        }

        viewModel.isCameraOn.observe(this) { binding.cameraSwitch.isChecked = it }

        viewModel.messages.observe(this) { updateMessageList(it) }
    }

    private fun openCameraDialog() {
        val prev = supportFragmentManager.findFragmentByTag(CAMERA_DIALOG_TAG)
        if (prev == null) {
            val cameraDialog = CameraDialog()
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            cameraDialog.show(fragmentTransaction, CAMERA_DIALOG_TAG)
        }
    }

    private fun stopCamera() {
        if (Preferences.getBoolean(CAMERA_STATUS)) {
            stopService(Intent(this, CopilotoService::class.java))
        }
    }

    private fun checkTTS() {
        Intent().apply { action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA }.also {
            startActivityForResult(it, TTS_DATA_CHECK_CODE)
        }
    }

    private fun updateMessageList(messageList: List<ChatMessage>) {
        synchronized(lock) {
            val subList = messageList.subList(lastIndexAdded, messageList.size)
            subList.forEach { message ->
                lastIndexAdded++
                addMessage(message)
            }
        }
    }

    private fun addMessage(chatMessage: ChatMessage) {
        chatAdapter.addChatMessage(chatMessage)
        binding.recyclerView.smoothScrollToPosition(chatAdapter.lastPosition)
    }

    private fun registerBroadCast() {
        IntentFilter(CAMERA_PREVIEW_ACTION).apply {
            addAction(POSITIVE_ANSEWR_ACTION)
            addAction(NEGATIVE_ANSWER_ACTION)
        }.let {
            registerReceiver(broadcastReceiver, it)
        }
    }

    private fun unregisterBroadCast() {
        unregisterReceiver(broadcastReceiver)
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            CAMERA_PREVIEW_ACTION -> openCameraDialog()
            POSITIVE_ANSEWR_ACTION -> chatAdapter.confirmAction()
            NEGATIVE_ANSWER_ACTION -> chatAdapter.refuseAction()
        }
    }

    private fun scheduleInitialConversation() {
        if (!Preferences.getBoolean(CAMERA_STATUS)) {
            MainScope().launch {
                val text = "Olá ${
                    MainApplication.username.split(" ").first()
                }, Bem-vindo. \nVocê Gostaria que eu ligasse a câmera para você?"
                IntentHelper.startCopilotoService()
                do {
                    delay(700)
                } while (!Preferences.getBoolean(TTS_ENABLED))
                IntentHelper.requestSpeech(text)
                ChatRepository.addMessage(
                    ChatMessage(
                        type = MessageType.BOT.name,
                        text = text
                    )
                )
                ChatRepository.addMessage(
                    ChatMessage(
                        type = MessageType.ANSWER.name,
                        confirmationAction = ConfirmationAction.CAMERA.name
                    )
                )
            }
        }
    }

    companion object {
        const val CAMERA_DIALOG_TAG = "camera_dialog"
    }
}