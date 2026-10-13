package com.openwispr.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outFile: File? = null

    fun start(): File? {
        try {
            val f = File(context.cacheDir, "wispr_${System.currentTimeMillis()}.m4a")
            @Suppress("DEPRECATION")
            val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
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
        val r = recorder ?: return outFile
        return try { r.stop(); r.reset(); r.release(); recorder = null; outFile } catch (e: Exception) {
            e.printStackTrace(); release(); outFile
        }
    }

    fun cancelAndDelete() {
        try { recorder?.stop() } catch (_: Exception) {}
        try { recorder?.reset(); recorder?.release() } catch (_: Exception) {}
        recorder = null
        outFile?.delete()
        outFile = null
    }

    fun getMaxAmplitude(): Int = try { recorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }

    private fun release() {
        try { recorder?.release() } catch (_: Exception) {}
        recorder = null
    }
}