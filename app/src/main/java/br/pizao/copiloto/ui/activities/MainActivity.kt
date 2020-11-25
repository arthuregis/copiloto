package br.pizao.copiloto.ui.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import br.pizao.copiloto.R
import br.pizao.copiloto.database.model.ChatMessage
import br.pizao.copiloto.databinding.MainActivityBinding
import br.pizao.copiloto.service.CopilotoService
import br.pizao.copiloto.ui.dialog.CameraDialog
import br.pizao.copiloto.ui.view.ChatMessageAdapter
import br.pizao.copiloto.ui.viewmodel.MainActivityViewModel
import br.pizao.copiloto.utils.Constants.CAMERA_STATUS
import br.pizao.copiloto.utils.Constants.PERMISSION_REQUEST_CODE
import br.pizao.copiloto.utils.Constants.TTS_DATA_CHECK_CODE
import br.pizao.copiloto.utils.Constants.TTS_ENABLED
import br.pizao.copiloto.utils.extensions.isCameraServiceRunning
import br.pizao.copiloto.utils.helpers.Permissions
import br.pizao.copiloto.utils.persistence.Preferences
import com.google.android.material.switchmaterial.SwitchMaterial


class MainActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(MainActivityViewModel::class.java) }
    private lateinit var binding: MainActivityBinding
    private val chatAdapter = ChatMessageAdapter()
    private var lastIndexAdded = 0
    private val lock = Object()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.main_activity)
        binding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = this@MainActivity.viewModel
            recyclerView.adapter = chatAdapter
        }

        setListeners()

        val permissions = Permissions(this)

        if (permissions.isNotAllGranted()) {
            permissions.requestMissing()
        }

        binding.cameraSwitch.isEnabled = permissions.isGranted(Manifest.permission.CAMERA)

        checkTTS()
    }

    override fun onResume() {
        super.onResume()

        if (isCameraServiceRunning()) {
            Preferences.putBoolean(CAMERA_STATUS, true)
        } else {
            Preferences.putBoolean(CAMERA_STATUS, false)
        }
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
                                finish()
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
        binding.recyclerView.scrollToPosition(chatAdapter.lastPosition)
    }

    companion object {
        const val CAMERA_DIALOG_TAG = "camera_dialog"
    }
}