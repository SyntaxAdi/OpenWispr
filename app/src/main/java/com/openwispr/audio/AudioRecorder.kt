package com.openwispr.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    fun cleanOrphanRecordings() {
        try {
            val dir = context.cacheDir ?: return
            dir.listFiles { file ->
                file.name.startsWith("wispr_") && file.name.endsWith(".m4a")
            }?.forEach { file ->
                if (file != outFile) {
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    fun start(): File? {
        try {
            cleanOrphanRecordings()
            val f = File(context.cacheDir, "wispr_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(SAMPLE_RATE_HZ)
                setAudioEncodingBitRate(BIT_RATE)
                setAudioChannels(1)
                setOutputFile(f.absolutePath)
                prepare()
                start()
            }
            recorder = r
            outFile = f
            return f
        } catch (e: Exception) {
            e.printStackTrace()
            release()
            return null
        }
    }

    fun stop(): File? {
        val r = recorder
        val f = outFile
        recorder = null
        if (r == null) {
            return if (f != null && f.exists() && f.length() >= MIN_VALID_AUDIO_BYTES) f else null
        }
        return try {
            r.stop()
            safeRelease(r)
            if (f != null && f.exists() && f.length() >= MIN_VALID_AUDIO_BYTES) {
                f
            } else {
                f?.delete()
                outFile = null
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            safeRelease(r)
            f?.delete()
            outFile = null
            null
        }
    }

    fun cancelAndDelete() {
        try { recorder?.stop() } catch (_: Exception) {}
        release()
        outFile?.delete()
        outFile = null
    }

    fun getMaxAmplitude(): Int = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }

    private fun release() {
        val r = recorder
        recorder = null
        safeRelease(r)
    }

    private fun safeRelease(r: MediaRecorder?) {
        try { r?.reset() } catch (_: Exception) {}
        try { r?.release() } catch (_: Exception) {}
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16000
        const val BIT_RATE = 64000
        const val MIN_VALID_AUDIO_BYTES = 512L
    }
}