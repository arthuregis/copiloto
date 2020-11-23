package br.pizao.copiloto.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil.setContentView
import androidx.lifecycle.ViewModelProvider
import br.pizao.copiloto.R
import br.pizao.copiloto.databinding.MainActivityBinding
import br.pizao.copiloto.dialog.CameraDialog
import br.pizao.copiloto.extensions.isCameraServiceRunning
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

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            binding.cameraButton.isEnabled = true
            checkTTS()
        } else {
            binding.cameraButton.isEnabled = false
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (isCameraServiceRunning()) {
            Preferences.putBoolean(Preferences.CAMERA_STATUS, true)
        } else {
            Preferences.putBoolean(Preferences.CAMERA_STATUS, false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_PERMISSION_CODE -> {
                if (grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                    binding.cameraButton.isEnabled = true
                    checkTTS()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.camera_permission_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
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
        const val CAMERA_PERMISSION_CODE = 100
        const val TTS_DATA_CHECK_CODE = 888
    }
}