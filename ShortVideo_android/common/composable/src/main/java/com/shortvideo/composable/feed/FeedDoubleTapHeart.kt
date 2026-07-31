package com.shortvideo.composable.feed

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.shortvideo.theme.PrimaryColor
import kotlin.math.roundToInt

data class HeartBurst(val id: Long, val offset: Offset)

@Composable
fun FeedDoubleTapHeartOverlay(
    bursts: List<HeartBurst>,
    onBurstFinished: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        bursts.forEach { burst ->
            key(burst.id) {
                HeartPulse(
                    offset = burst.offset,
                    onFinished = { onBurstFinished(burst.id) },
                )
            }
        }
    }
}

@Composable
private fun HeartPulse(
    offset: Offset,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val scale = remember { Animatable(0.6f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        scale.animateTo(1.4f, tween(300))
        alpha.animateTo(0f, tween(200))
        onFinished()
    }

    Icon(
        imageVector = Icons.Default.Favorite,
        contentDescription = null,
        tint = PrimaryColor,
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (offset.x - with(density) { 40.dp.toPx() }).roundToInt(),
                    y = (offset.y - with(density) { 40.dp.toPx() }).roundToInt(),
                )
            }
            .size(80.dp)
            .scale(scale.value)
            .alpha(alpha.value),
    )
}

@Composable
fun rememberHeartBurstController(): HeartBurstController {
    return remember { HeartBurstController() }
}

class HeartBurstController {
    var bursts by mutableStateOf<List<HeartBurst>>(emptyList())
        private set

    fun spawn(x: Float, y: Float) {
        val id = System.currentTimeMillis()
        bursts = bursts + HeartBurst(id, Offset(x, y))
    }

    fun remove(id: Long) {
        bursts = bursts.filterNot { it.id == id }
    }
}
