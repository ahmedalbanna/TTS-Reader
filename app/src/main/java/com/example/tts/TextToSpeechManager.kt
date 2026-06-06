package com.example.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class TextToSpeechManager(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    // Exposed list of current sentences for the active text reading
    private val _sentences = MutableStateFlow<List<String>>(emptyList())
    val sentences: StateFlow<List<String>> = _sentences.asStateFlow()

    private var currentText: String = ""
    private var pitch: Float = 1.0f
    private var speed: Float = 1.0f
    private var activeLocale: Locale = Locale("ar")

    // Coroutine scope to bridge listener callbacks (background threads) to state flows
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            _isInitialized.value = true
            setupProgressListener()
            setLanguage(activeLocale)
            setPitch(pitch)
            setSpeechRate(speed)
        } else {
            Log.e("TTSManager", "Initialization of TTS failed")
            _isInitialized.value = false
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                scope.launch {
                    _isPlaying.value = true
                    utteranceId?.toIntOrNull()?.let { index ->
                        _currentSentenceIndex.value = index
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                scope.launch {
                    val finishedIndex = utteranceId?.toIntOrNull() ?: 0
                    val nextIndex = finishedIndex + 1
                    if (nextIndex < _sentences.value.size) {
                        speakSentenceAt(nextIndex)
                    } else {
                        // All sentences read
                        _isPlaying.value = false
                        _currentSentenceIndex.value = 0
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                scope.launch {
                    _isPlaying.value = false
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e("TTSManager", "Error speaking sentence $utteranceId: error code $errorCode")
                scope.launch {
                    _isPlaying.value = false
                }
            }
        })
    }

    fun setText(text: String) {
        currentText = text
        // Split text by punctuation or line breaks keeping sentence structure
        val list = splitTextToSentences(text)
        _sentences.value = list
        _currentSentenceIndex.value = 0
    }

    fun play() {
        if (!_isInitialized.value || _sentences.value.isEmpty()) return
        speakSentenceAt(_currentSentenceIndex.value)
    }

    fun pause() {
        if (_isPlaying.value) {
            tts?.stop()
            _isPlaying.value = false
        }
    }

    fun stop() {
        tts?.stop()
        _isPlaying.value = false
        _currentSentenceIndex.value = 0
    }

    fun setSentenceIndex(index: Int) {
        if (index >= 0 && index < _sentences.value.size) {
            _currentSentenceIndex.value = index
            if (_isPlaying.value) {
                speakSentenceAt(index)
            }
        }
    }

    private fun speakSentenceAt(index: Int) {
        val list = _sentences.value
        if (index < 0 || index >= list.size) return

        val sentenceToRead = list[index]
        if (sentenceToRead.isBlank()) {
            // Skip blank sentences and go to next
            scope.launch {
                val nextIndex = index + 1
                if (nextIndex < list.size) {
                    speakSentenceAt(nextIndex)
                } else {
                    _isPlaying.value = false
                    _currentSentenceIndex.value = 0
                }
            }
            return
        }

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, index.toString())
        }

        tts?.speak(sentenceToRead, TextToSpeech.QUEUE_FLUSH, params, index.toString())
    }

    fun setPitch(pitchFactor: Float) {
        pitch = pitchFactor
        tts?.setPitch(pitch)
    }

    fun setSpeechRate(rateFactor: Float) {
        speed = rateFactor
        tts?.setSpeechRate(speed)
    }

    fun setLanguage(locale: Locale) {
        activeLocale = locale
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TTSManager", "Language $locale is not supported or missing data")
        }
    }

    fun getAvailableLanguages(): List<Locale> {
        val systemLocales = Locale.getAvailableLocales()
        val supported = mutableListOf<Locale>()
        val defaultLocales = listOf(
            Locale("ar"),
            Locale.US,
            Locale.UK,
            Locale.FRANCE,
            Locale.GERMANY,
            Locale.ITALY,
            Locale.CHINA,
            Locale.JAPAN
        )
        tts?.let { engine ->
            for (locale in defaultLocales) {
                val status = engine.isLanguageAvailable(locale)
                if (status >= TextToSpeech.LANG_AVAILABLE) {
                    supported.add(locale)
                }
            }
            // Add any other system locales that are available to reduce empty sets
            for (locale in systemLocales) {
                if (locale.language !in supported.map { it.language } && locale.language.isNotEmpty()) {
                    val status = engine.isLanguageAvailable(locale)
                    if (status >= TextToSpeech.LANG_AVAILABLE) {
                        supported.add(locale)
                        if (supported.size >= 12) break // Limit to clean set of languages
                    }
                }
            }
        }
        // Ensure Arabic is always available in the selection even if system TTS returns checks oddly
        if (supported.none { it.language == "ar" }) {
            supported.add(0, Locale("ar"))
        }
        return supported.distinctBy { it.language }
    }

    fun shutdown() {
        tts?.shutdown()
        _isInitialized.value = false
    }

    private fun splitTextToSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        // Split by punctuation: period, question mark, exclamation, Arabic comma, Arabic question, newline
        // but positive lookbehind keeps delimiters in sentences to make reading natural
        val rawSentences = text.split(Regex("(?<=[.!؟?»،\n])|(?=\n)"))
        return rawSentences
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
