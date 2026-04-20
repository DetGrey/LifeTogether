package com.example.lifetogether.ui

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application(), ImageLoaderFactory {
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Limit memory cache to 15% of available memory to prevent OOM
                    .maxSizePercent(0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Limit disk cache to 100MB
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            // Prefer disk cache over network to reduce memory usage
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            // Enable bitmap pooling for better memory reuse
            .allowHardware(true) // Use hardware bitmaps when possible (less memory)
            .crossfade(true)
            .build()
    }
}
