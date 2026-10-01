package com.neonmusic.player.service

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

    // Equalizer and BassBoost are disabled by default to ensure pure, crisp, unmuffled sound
    private var isEqEnabled: Boolean = false
    private var isBassEnabled: Boolean = false
    private var savedBassStrength: Short = 0
    private var savedPreset: Short = -1
    private val savedBandLevels = mutableMapOf<Short, Short>()

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId <= 0 || audioSessionId == currentSessionId) return
        release()
        currentSessionId = audioSessionId

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = isEqEnabled
                if (isEqEnabled) {
                    if (savedPreset >= 0 && savedPreset < numberOfPresets) {
                        try { usePreset(savedPreset) } catch (e: Exception) { /* ignore */ }
                    }
                    for ((band, level) in savedBandLevels) {
                        try { setBandLevel(band, level) } catch (e: Exception) { /* ignore */ }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("AudioEffectManager", "Equalizer not supported on this device/session: ${e.message}")
            equalizer = null
        }

        try {
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = isBassEnabled && savedBassStrength > 0
                if (enabled && strengthSupported) {
                    try { setStrength(savedBassStrength) } catch (e: Exception) { /* ignore */ }
                }
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
        savedBandLevels[band] = level
        isEqEnabled = true
        try {
            equalizer?.let {
                if (!it.enabled) it.enabled = true
                it.setBandLevel(band, level)
            }
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "setBandLevel failed: ${e.message}")
        }
    }

    fun usePreset(preset: Short) {
        savedPreset = preset
        isEqEnabled = true
        try {
            equalizer?.let {
                if (!it.enabled) it.enabled = true
                it.usePreset(preset)
            }
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "usePreset failed: ${e.message}")
        }
    }

    fun setBassStrength(strength: Short) {
        savedBassStrength = strength
        isBassEnabled = strength > 0
        try {
            bassBoost?.let {
                it.enabled = isBassEnabled
                if (isBassEnabled && it.strengthSupported) {
                    it.setStrength(strength)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioEffectManager", "setBassStrength failed: ${e.message}")
        }
    }
}
