package com.example.lifeapp.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * 通用 Composable Effect：當 App 切回前景、解鎖或頁面 Resume 時觸發 onResume 動作
 */
@Composable
fun OnLifecycleResume(onResume: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                onResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * 🎯 全站頁面通用的生命週期與輪詢處理器
 */
@Composable
fun AutoRefreshLifecycleHandler(
    onStartRefresh: () -> Unit,
    onStopRefresh: () -> Unit,
    onResumeFetch: () -> Unit
) {
    OnLifecycleResume {
        onResumeFetch()
        onStartRefresh()
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopRefresh()
        }
    }
}
