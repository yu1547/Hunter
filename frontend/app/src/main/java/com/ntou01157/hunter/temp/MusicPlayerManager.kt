package com.ntou01157.hunter.temp

import android.content.Context
import android.media.MediaPlayer
import com.ntou01157.hunter.R

object MusicPlayerManager {
    private var mediaPlayer: MediaPlayer? = null
    private var isInitialized = false

    fun playMusic(context: Context) {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.backmusic)
            mediaPlayer?.isLooping = true
        }

        if (isInitialized.not()) {
            mediaPlayer?.start()
            isInitialized = true
        } else if (mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
        }
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
    }

    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        isInitialized = false
    }
}