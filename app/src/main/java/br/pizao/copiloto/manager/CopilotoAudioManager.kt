package br.pizao.copiloto.manager

import android.content.Context
import android.content.res.AssetManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.text.format.DateUtils
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random


object CopilotoAudioManager {
    private lateinit var assetManager: AssetManager
    private lateinit var audioManager: AudioManager
    private val random = Random(System.currentTimeMillis())
    private var isPlaying = false
    private var originalVolume = 0

    fun init(context: Context) {
        if (!::assetManager.isInitialized) {
            assetManager = context.assets
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

    }

    fun horn() {
        if (!isPlaying) {
            synchronized(this) {
                isPlaying = true

                val hornSound = assetManager.openFd("horn_audio${random.nextInt(0, 2)}.wav")
                val mediaPlayer = MediaPlayer().apply {
                    setDataSource(hornSound)
                    isLooping = false
                    prepare()
                }

                MainScope().launch {
                    setVolumetoMax()
                    mediaPlayer.start()
                    delay(3 * DateUtils.SECOND_IN_MILLIS)
                    resetVolume()
                    mediaPlayer.release()
                    isPlaying = false
                }
            }
        }
    }

    fun setVolumetoMax() {
        originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
            0
        )
    }

    fun resetVolume() {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
    }
}