package com.enlpot.notix

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.ArrayDeque
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * TTS 播报管理器（v7.20）。
 *
 * 懒加载系统 TextToSpeech 引擎，主线程 Handler 初始化（TextToSpeech 构造需要主线程 Looper）。
 * - Locale.SIMPLIFIED_CHINESE 优先，不可用时回退系统默认语言
 * - AudioAttributes USAGE_NOTIFICATION 播报（跟随通知音量）
 * - QUEUE_ADD 排队播报，避免多条通知互相打断
 * - UtteranceProgressListener / onError 容错，引擎不可用静默降级
 * - Service onDestroy 时 shutdown 释放引擎
 */
object TtsSpeaker {
    private const val TAG = "TtsSpeaker"

    /** 待播报队列上限（v7.23）：防止通知风暴时无限堆积内存 */
    private const val MAX_PENDING = 20

    private var tts: TextToSpeech? = null
    private var ready = false
    /** 待播报项（文本 + 完成回调，阶段 2B 增加回调支持） */
    private class PendingItem(val text: String, val onDone: ((Boolean) -> Unit)? = null)
    private val pending = ArrayDeque<PendingItem>()
    /** utteranceId → 完成回调（onDone/onError 触发，防重复：取走即移除） */
    private val callbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()
    /** utteranceId 自增序列（阶段 4C-C-B：替代毫秒时间戳，消除同毫秒并发碰撞） */
    private val utteranceSeq = AtomicLong(0L)
    private val handler = Handler(Looper.getMainLooper())

    /**
     * 主线程安全入口：初始化未完成时先排队，完成后依次播报。
     * [onDone] 在播报完成（onDone=true）或失败（onError / speak 返回非 SUCCESS / 文本为空 / 排队被丢弃）时回调。
     */
    fun speak(context: Context, text: String, onDone: ((Boolean) -> Unit)? = null) {
        if (text.isBlank()) {
            onDone?.invoke(false)
            return
        }
        handler.post {
            ensureInitialized(context.applicationContext)
            if (ready) {
                doSpeak(text, onDone)
            } else {
                pending.addLast(PendingItem(text, onDone))
                // 队列超限：丢弃最旧的待播文本，避免无限堆积（v7.23）
                if (pending.size > MAX_PENDING) {
                    val dropped = pending.removeFirst()
                    Log.w(TAG, "Pending queue overflow (max=$MAX_PENDING), drop oldest: ${dropped.text}")
                    dropped.onDone?.invoke(false)
                }
            }
        }
    }

    /** 释放引擎（Service onDestroy 时调用） */
    fun shutdown() {
        handler.post {
            ready = false
            // 清空排队：未播报的项按失败回调，避免调用方（ActionFlow）永久挂起
            while (pending.isNotEmpty()) {
                pending.removeFirst().onDone?.invoke(false)
            }
            callbacks.clear()
            try {
                tts?.stop()
                tts?.shutdown()
            } catch (e: Exception) {
                Log.w(TAG, "TTS shutdown failed", e)
            }
            tts = null
        }
    }

    private fun ensureInitialized(context: Context) {
        if (tts != null) return
        ready = false
        tts = TextToSpeech(context) { status ->
            if (status != TextToSpeech.SUCCESS) {
                // v7.23：初始化失败必须释放引擎，否则 tts 非空会导致后续 speak 永不重试
                Log.w(TAG, "TTS init failed, status=$status, releasing engine for retry")
                try {
                    tts?.shutdown()
                } catch (e: Exception) {
                    Log.w(TAG, "TTS shutdown after failed init", e)
                }
                tts = null
                ready = false
                return@TextToSpeech
            }
            try {
                val engine = tts
                if (engine == null) return@TextToSpeech
                val zh = Locale.SIMPLIFIED_CHINESE
                val lang = when {
                    engine.isLanguageAvailable(zh) >= TextToSpeech.LANG_AVAILABLE -> zh
                    engine.isLanguageAvailable(Locale.CHINESE) >= TextToSpeech.LANG_AVAILABLE -> Locale.CHINESE
                    else -> Locale.getDefault()
                }
                engine.language = lang
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) {
                        callbacks.remove(utteranceId)?.invoke(true)
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        Log.w(TAG, "TTS utterance error: $utteranceId")
                        callbacks.remove(utteranceId)?.invoke(false)
                    }
                })
                ready = true
                Log.i(TAG, "TTS initialized, language=$lang")
                // 初始化期间排队的播报依次发出
                while (pending.isNotEmpty()) {
                    val item = pending.removeFirst()
                    doSpeak(item.text, item.onDone)
                }
            } catch (e: Exception) {
                Log.w(TAG, "TTS configure failed", e)
            }
        }
    }

    private fun doSpeak(text: String, onDone: ((Boolean) -> Unit)? = null) {
        // 提升到 try 外：catch 需要按当前 utteranceId 定向移除，而非 clear 全部（阶段 4C-C-B P2-2）
        var utteranceId: String? = null
        try {
            // AtomicLong 自增唯一 ID，不再依赖 System.currentTimeMillis()（阶段 4C-C-B P2-1）
            utteranceId = "tts-" + utteranceSeq.incrementAndGet()
            val id = utteranceId
            if (onDone != null) callbacks[id] = onDone
            val result = tts?.speak(text, TextToSpeech.QUEUE_ADD, null, id) ?: TextToSpeech.ERROR
            if (result != TextToSpeech.SUCCESS) {
                callbacks.remove(id)
                onDone?.invoke(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak failed", e)
            // 只清理当前 utterance 的 callback，不影响其他 Flow 正在等待的 TTS（阶段 4C-C-B P2-2）
            utteranceId?.let { callbacks.remove(it) }
            onDone?.invoke(false)
        }
    }
}
