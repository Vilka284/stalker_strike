package com.project.stalker_strike

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Handler
import android.os.Looper

class SoundManager private constructor(private val context: Context) {
    private var isPlayingSoundEffect: Boolean = false
    private var soundPool: SoundPool? = null
    private var soundMap: HashMap<String, Int> = HashMap()
    private var soundDurationMap: HashMap<String, Int> = HashMap()
    private val handler = Handler(Looper.getMainLooper())
    private val loadCompleteMap: HashMap<Int, Boolean> = HashMap()

    init {
        soundPool = SoundPool.Builder().setMaxStreams(10).build()
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                // Sound is loaded successfully
                loadCompleteMap[sampleId] = true
            }
        }
        loadSounds()
    }

    private fun loadSounds() {
        soundMap["anomaly_beep"] = soundPool?.load(context, R.raw.anomaly_beep, 1) ?: 0
        soundDurationMap["anomaly_beep"] = 1000

        soundMap["radiation_strong"] = soundPool?.load(context, R.raw.geiger_strong, 1) ?: 0
        soundDurationMap["radiation_strong"] = 6000

        soundMap["radiation_weak"] = soundPool?.load(context, R.raw.geiger, 1) ?: 0
        soundDurationMap["radiation_weak"] = 6000

        soundMap["item_scan"] = soundPool?.load(context, R.raw.pda_ejection, 1) ?: 0
        soundDurationMap["item_scan"] = 1000
    }

    fun isSoundOnPhoneEnabled(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return volume > 0
    }

    fun playSoundEffect(type: String) {
        if (isSoundOnPhoneEnabled() && !isPlayingSoundEffect) {
            val soundId = soundMap[type]
            val duration = soundDurationMap[type]
            if (soundId != null) {
                if (loadCompleteMap[soundId] == true) {
                    playSound(soundId, duration?.toLong() ?: 10000)
                } else {
                    handler.postDelayed({
                        if (loadCompleteMap[soundId] == true) {
                            playSound(soundId, duration?.toLong() ?: 10000)
                        }
                    }, 500)
                }
            }
        }
    }

    private fun playSound(soundId: Int, duration: Long) {
        isPlayingSoundEffect = true
        soundPool?.play(soundId, 1f, 1f, 1, 0, 1f)

        handler.postDelayed({
            isPlayingSoundEffect = false
        }, duration)
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: SoundManager? = null

        fun getInstance(context: Context): SoundManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: SoundManager(context).also { INSTANCE = it }
            }
    }

    enum class SoundType {
        ANOMALY,
        RADIATION,
        RADIATION_STRONG
    }

    fun releaseResources() {
        soundPool?.release()
        soundPool = null
        handler.removeCallbacksAndMessages(null)
    }
}
