package com.smartvoice.assistant.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smartvoice.assistant.data.model.Language
import com.smartvoice.assistant.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Settings screen.
 * Manages user preferences for language, speech rate, gesture mode, etc.
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("smart_voice_prefs", 0)

    private val _language = MutableStateFlow(
        Language.fromCode(prefs.getString(Constants.PREF_LANGUAGE, "en-US") ?: "en-US")
    )
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _autoDetect = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_AUTO_DETECT_LANGUAGE, true)
    )
    val autoDetect: StateFlow<Boolean> = _autoDetect.asStateFlow()

    private val _gestureEnabled = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_GESTURE_ENABLED, false)
    )
    val gestureEnabled: StateFlow<Boolean> = _gestureEnabled.asStateFlow()

    private val _offlineMode = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_OFFLINE_MODE, false)
    )
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _continuousListening = MutableStateFlow(
        prefs.getBoolean(Constants.PREF_CONTINUOUS_LISTENING, false)
    )
    val continuousListening: StateFlow<Boolean> = _continuousListening.asStateFlow()

    private val _speechRate = MutableStateFlow(
        prefs.getFloat(Constants.PREF_SPEECH_RATE, Constants.DEFAULT_SPEECH_RATE)
    )
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    fun setLanguage(language: Language) {
        _language.value = language
        prefs.edit().putString(Constants.PREF_LANGUAGE, language.code).apply()
    }

    fun setAutoDetect(enabled: Boolean) {
        _autoDetect.value = enabled
        prefs.edit().putBoolean(Constants.PREF_AUTO_DETECT_LANGUAGE, enabled).apply()
    }

    fun setGestureEnabled(enabled: Boolean) {
        _gestureEnabled.value = enabled
        prefs.edit().putBoolean(Constants.PREF_GESTURE_ENABLED, enabled).apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean(Constants.PREF_OFFLINE_MODE, enabled).apply()
    }

    fun setContinuousListening(enabled: Boolean) {
        _continuousListening.value = enabled
        prefs.edit().putBoolean(Constants.PREF_CONTINUOUS_LISTENING, enabled).apply()
    }

    fun setSpeechRate(rate: Float) {
        _speechRate.value = rate
        prefs.edit().putFloat(Constants.PREF_SPEECH_RATE, rate).apply()
    }
}
