package br.pizao.copiloto.activities

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
import br.pizao.copiloto.databinding.MainActivityBinding
import br.pizao.copiloto.dialog.CameraDialog
import br.pizao.copiloto.extensions.isCameraServiceRunning
import br.pizao.copiloto.utils.Constants.PERMISSION_REQUEST_CODE
import br.pizao.copiloto.utils.Constants.STT_SHARED_KEY
import br.pizao.copiloto.utils.Constants.TTS_DATA_CHECK_CODE
import br.pizao.copiloto.utils.Permissions
import br.pizao.copiloto.utils.Preferences
import br.pizao.copiloto.utils.Preferences.TTS_ENABLED
import br.pizao.copiloto.viewmodel.MainActivityViewModel


class MainActivity : AppCompatActivity() {

    private val viewModel by lazy { ViewModelProvider(this).get(MainActivityViewModel::class.java) }
    private lateinit var binding: MainActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = setContentView(this, R.layout.main_activity)
        binding.apply {
            lifecycleOwner = this@MainActivity
            viewModel = this@MainActivity.viewModel
        }

        viewModel.openCameraTrigger.observe(this) {
            if (it) {
                openCameraDialog()
            }
        }

        val permissions = Permissions(this)

        if (permissions.isNotAllGranted()) {
            permissions.requestMissing()
        }

        binding.cameraButton.isEnabled = permissions.isGranted(Manifest.permission.CAMERA)

        checkTTS()
    }

    override fun onResume() {
        super.onResume()

        if (isCameraServiceRunning()) {
            Preferences.putBoolean(Preferences.CAMERA_STATUS, true)
        } else {
            Preferences.putBoolean(Preferences.CAMERA_STATUS, false)
        }
    }

    override fun onDestroy() {
        Preferences.putString(STT_SHARED_KEY, "")
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
                                binding.cameraButton.isEnabled = true
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

    private fun openCameraDialog() {
        val prev = supportFragmentManager.findFragmentByTag(CAMERA_DIALOG_TAG)
        if (prev == null) {
            val cameraDialog = CameraDialog()
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            cameraDialog.show(fragmentTransaction, CAMERA_DIALOG_TAG)
        }
        viewModel.openCameraCompleted()
    }

    private fun checkTTS() {
        Intent().apply { action = TextToSpeech.Engine.ACTION_CHECK_TTS_DATA }.also {
            startActivityForResult(it, TTS_DATA_CHECK_CODE)
        }
    }

    companion object {
        const val CAMERA_DIALOG_TAG = "camera_dialog"
    }
}