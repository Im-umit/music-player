package com.example.service

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

class AudioEffectManager(private val context: Context) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Equalizer not supported on this device/session: ${e.message}")
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "BassBoost not supported: ${e.message}")
            bassBoost = null
        }
    }

    fun release() {
        try {
            equalizer?.release()
        } catch (e: Exception) { /* ignore */ }
        equalizer = null

        try {
            bassBoost?.release()
        } catch (e: Exception) { /* ignore */ }
        bassBoost = null
        currentSessionId = 0
    }

    fun getEqualizerData(): JSONObject {
        val json = JSONObject()
        val eq = equalizer

        if (eq == null) {
            json.put("supported", false)
            json.put("presets", JSONArray())
            json.put("bands", JSONArray())
            json.put("bassStrength", 0)
            return json
        }

        try {
            json.put("supported", true)
            json.put("enabled", eq.enabled)

            // Presets
            val presetsArray = JSONArray()
            val numPresets = eq.numberOfPresets
            for (i in 0 until numPresets) {
                val presetName = try { eq.getPresetName(i.toShort()) } catch (e: Exception) { "Preset $i" }
                presetsArray.put(presetName)
            }
            json.put("presets", presetsArray)
            json.put("currentPreset", eq.currentPreset.toInt())

            // Bands
            val bandsArray = JSONArray()
            val numBands = eq.numberOfBands
            val minBandLevel = eq.bandLevelRange[0]
            val maxBandLevel = eq.bandLevelRange[1]
            json.put("minLevel", minBandLevel.toInt())
            json.put("maxLevel", maxBandLevel.toInt())

            for (i in 0 until numBands) {
                val bandObj = JSONObject()
                bandObj.put("index", i)
                val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // in Hz
                bandObj.put("centerFreq", centerFreq)
                val currentLevel = eq.getBandLevel(i.toShort())
                bandObj.put("level", currentLevel.toInt())
                bandsArray.put(bandObj)
            }
            json.put("bands", bandsArray)

            // Bass boost
            val bass = bassBoost
            val bassStrength = if (bass != null && bass.strengthSupported) {
                bass.roundedStrength.toInt()
            } else 0
            json.put("bassStrength", bassStrength)
            json.put("bassSupported", bass?.strengthSupported ?: false)

        } catch (e: Exception) {
            Log.e("AudioEffectManager", "Error reading equalizer data: ${e.message}")
            json.put("supported", false)
        }

        return json
    }

    fun setBandLevel(band: Short, level: Short) {
        try {
            equalizer?.setBandLevel(band, level)
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "setBandLevel failed: ${e.message}")
        }
    }

    fun usePreset(preset: Short) {
        try {
            equalizer?.usePreset(preset)
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "usePreset failed: ${e.message}")
        }
    }

    fun setBassStrength(strength: Short) {
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(strength)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "setBassStrength failed: ${e.message}")
        }
    }
}
