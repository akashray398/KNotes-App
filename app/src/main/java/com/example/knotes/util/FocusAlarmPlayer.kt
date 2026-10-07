package com.example.knotes.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log

object FocusAlarmPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var stopHandler: Handler? = null
    private const val TAG = "FocusAlarmPlayer"

    fun playCalmAlarm(context: Context, durationMs: Long = 10_000L) {
        stopAlarm() // Stop any previous playback
        try {
            var alertUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            Log.d(TAG, "Calm focus alarm started for ${durationMs / 1000} seconds")

            // Automatically stop playback after durationMs (10 seconds)
            stopHandler = Handler(Looper.getMainLooper())
            stopHandler?.postDelayed({
                stopAlarm()
            }, durationMs)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to play calm alarm tone", e)
        }
    }

    fun stopAlarm() {
        try {
            stopHandler?.removeCallbacksAndMessages(null)
            stopHandler = null
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            Log.d(TAG, "Calm focus alarm stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping calm focus alarm", e)
        }
    }
}
