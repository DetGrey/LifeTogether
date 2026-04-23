package com.example.lifetogether.ui.common.image

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun DisplayVideoFromUri(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = false, // Default to false for gallery viewing
    useController: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    keepScreenOn: Boolean = true, // Keep screen on during video playback
) {
    val viewModel: VideoPlayerViewModel = hiltViewModel()

    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Pass the media URI to the ViewModel
    LaunchedEffect(videoUri, autoPlay) {
        viewModel.setMediaUri(videoUri, autoPlay)
    }

    // Register lifecycle observer for the ViewModel
    DisposableEffect(lifecycleOwner, viewModel) {
        viewModel.registerLifecycleObserver(lifecycleOwner.lifecycle)
        onDispose {
            // ViewModel's onCleared will handle the primary release.
            // Optionally, remove observer if needed, but onCleared should suffice.
            // lifecycleOwner.lifecycle.removeObserver(viewModel.lifecycleObserver) // ViewModel manages its observer internally
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth(),
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = viewModel.exoPlayer
                    this.useController = useController
                    this.keepScreenOn = keepScreenOn
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.resizeMode = resizeMode
                    // Enable gesture controls
                    this.controllerShowTimeoutMs = 3000
                    this.controllerHideOnTouch = true
                    // Show buffering indicator
                    this.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { playerView ->
                // Update properties that might change
                playerView.resizeMode = resizeMode
                playerView.useController = useController
                playerView.keepScreenOn = keepScreenOn
            }
        )

        // Example: Show loading indicator or error message based on ViewModel state
        if (playerState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        playerState.error?.let {
            Text(
                text = "Error: ${it.message ?: "Unknown error"}",
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}
