package com.example.lifetogether.ui.common.image

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.util.EventLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

// Data class to hold player state if needed (e.g., for custom UI)
data class VideoPlayerState(
    val isPlaying: Boolean = false,
    val isLoading: Boolean = true,
    val error: PlaybackException? = null,
    // Add other states like currentPosition, duration, etc., if needed
)

@HiltViewModel
class VideoPlayerViewModel @Inject constructor(
    application: Application,
) : AndroidViewModel(application) {

    // Keep ExoPlayer instance within the ViewModel
    val exoPlayer: ExoPlayer = ExoPlayer.Builder(application).build()

    private val _playerState = MutableStateFlow(VideoPlayerState())
    val playerState: StateFlow<VideoPlayerState> = _playerState.asStateFlow()

    private var currentUri: Uri? = null
    private var playWhenReady = false

    init {
        exoPlayer.addAnalyticsListener(EventLogger())

        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.value = _playerState.value.copy(isPlaying = isPlaying)
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                _playerState.value = _playerState.value.copy(
                    isLoading = playbackState == Player.STATE_BUFFERING,
                    // You can add more detailed state handling here
                )
                if (playbackState == Player.STATE_ENDED) {
                    // Handle video end, e.g., seek to start, show replay button
                    exoPlayer.seekTo(0)
                    exoPlayer.playWhenReady = false // Stop playing
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                _playerState.value = _playerState.value.copy(error = error, isLoading = false)
                // Log the error or show a message to the user
                android.util.Log.e("VideoPlayerVM", "Player Error: ", error)
            }
        })
    }

    fun setMediaUri(uri: Uri, autoPlay: Boolean = false) {
        if (uri == currentUri) return // Avoid re-initializing if URI is the same

        currentUri = uri
        this.playWhenReady = autoPlay

        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = autoPlay
        _playerState.value = VideoPlayerState(isLoading = true, error = null) // Reset state
    }

    fun play() {
        exoPlayer.play()
    }

    fun pause() {
        exoPlayer.pause()
    }

    // LifecycleObserver to manage player state based on Activity/Fragment lifecycle
    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onPause(owner: LifecycleOwner) {
            pause() // Pause player when lifecycle owner is paused
        }

        override fun onResume(owner: LifecycleOwner) {
            // Optionally, resume playback if it was playing before pause and autoPlay is desired
            // Or if you have a specific state indicating it should resume.
            // if (playWhenReady && currentUri != null) { // Be careful with auto-resume logic
            //     exoPlayer.playWhenReady = true
            // }
        }

        override fun onStop(owner: LifecycleOwner) {
            // More aggressive pause if needed, or even partial release for background apps
            // exoPlayer.playWhenReady = false // Ensure it stops
        }

        override fun onDestroy(owner: LifecycleOwner) {
            // This is crucial. However, onCleared() in ViewModel is the primary release point for ViewModel-owned resources.
            // releasePlayer()
        }
    }

    fun registerLifecycleObserver(lifecycle: androidx.lifecycle.Lifecycle) {
        lifecycle.addObserver(lifecycleObserver)
    }

    private fun releasePlayer() {
        exoPlayer.release()
        currentUri = null
        _playerState.value = VideoPlayerState() // Reset state
    }

    // This is called when the ViewModel is about to be destroyed.
    override fun onCleared() {
        super.onCleared()
        releasePlayer()
        android.util.Log.d("VideoPlayerVM", "ViewModel cleared, player released.")
    }
}
