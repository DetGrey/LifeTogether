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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun DisplayVideoFromUri(
    videoUri: Uri,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    useController: Boolean = true,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
) {
    val viewModel: VideoPlayerViewModel = hiltViewModel()

    val playerState by viewModel.playerState.collectAsState()
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
                    this.player = viewModel.exoPlayer // Get ExoPlayer from ViewModel
                    this.useController = useController
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    this.resizeMode = resizeMode
                }
            },
            modifier = Modifier.fillMaxSize(),
            // No update block needed usually if the player instance doesn't change
            // and PlayerView correctly observes the player's state.
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
