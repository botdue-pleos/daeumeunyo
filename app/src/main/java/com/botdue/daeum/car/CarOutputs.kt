package com.botdue.daeum.car

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import com.botdue.daeum.R
import java.util.Locale

/**
 * 엔진이 시킨 일을 차에서 실제로 하는 쪽.
 * 지금은 안드로이드 기본 TTS·MediaPlayer로 돌고, 공조는 화면 표시만 한다.
 * Pleos 차량에서는 TTS를 Pleos TextToSpeech, 공조를 Vehicle HVAC(CONTROL_CAR_CLIMATE)로 바꿔 끼운다.
 */
interface CarControls {
    fun setRecirculation(on: Boolean)
}

/** 시연용: 실제 공조 대신 상태만 기억한다 */
class DemoCarControls(private val onChange: (Boolean) -> Unit) : CarControls {
    override fun setRecirculation(on: Boolean) = onChange(on)
}

class Speaker(context: Context) {
    private var ready = false
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
    }.apply { language = Locale.KOREAN }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_ADD, null, text.hashCode().toString())
    }

    fun release() = tts.shutdown()
}

/**
 * 보호자가 "표시해"라고 했을 때 내는 짧은 소리 하나. 말(TTS)로 되읽지 않는다:
 * 스피커는 차 안 모두에게 들리므로 동승자가 "자기 얘기"를 듣지 않게 한다.
 */
class MarkTone {
    private val tone = runCatching { ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60) }.getOrNull()

    fun play() {
        tone?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
    }

    fun release() {
        tone?.release()
    }
}

/**
 * 터널 앞에서 트는 노래. 앱 안에 넣어 둔 음원을 직접 재생한다 (기획서 A 방식).
 * 오디오 포커스를 잠깐 빌리는 방식(GAIN_TRANSIENT)이라 끝나면 원래 듣던 소리가 돌아오고,
 * 내비 안내 음성이 나올 때는 차가 이 노래 소리를 줄인다(덕킹).
 */
class TunnelMusic(context: Context) {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .setWillPauseWhenDucked(false)
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS) stop()
        }
        .build()
    private var player: MediaPlayer? = null

    val playing: Boolean get() = player?.isPlaying == true

    fun play() {
        if (playing) return
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) return
        player = MediaPlayer.create(app, R.raw.calm_song, attributes, audio.generateAudioSessionId())?.apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        player?.run { stop(); release() }
        player = null
        audio.abandonAudioFocusRequest(focus)
    }
}
