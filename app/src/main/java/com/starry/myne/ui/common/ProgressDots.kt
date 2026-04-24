package com.starry.myne.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import custom.AppTheme
import kotlinx.coroutines.delay


@Composable
fun ProgressDots(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.primary
) {
    val animatables = List(3) { remember { Animatable(0f) } }

    // launch animations staggered
    animatables.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 120L) // stagger start
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0f at 0 using LinearOutSlowInEasing
                        1f at 200 using LinearOutSlowInEasing
                        0f at 400 using LinearOutSlowInEasing
                        0f at 1200
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Row(modifier) {
        animatables.forEachIndexed { index, animatable ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer {
                        translationY = -animatable.value * 8.dp.toPx()
                    }
                    .background(color = color, shape = CircleShape)
            )

            if (index != animatables.lastIndex) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProgressDotsPreview() {
    ProgressDots()
}

//@Preview
//@Composable
//fun ProgressDots(
//    modifier: Modifier = Modifier.padding(4.dp),
//    color: Color = AppTheme.colors.primary
//) {
//
//    val dots = listOf(
//        remember { Animatable(0f) },
//        remember { Animatable(0f) },
//        remember { Animatable(0f) },
//    )
//
//    dots.forEachIndexed { index, animatable ->
//        LaunchedEffect(animatable) {
//            delay(index * 100L)
//            animatable.animateTo(
//                targetValue = 1f, animationSpec = infiniteRepeatable(
//                    animation = keyframes {
//                        durationMillis = 2000
//                        0.0f at 0 using LinearOutSlowInEasing
//                        1.0f at 200 using LinearOutSlowInEasing
//                        0.0f at 400 using LinearOutSlowInEasing
//                        0.0f at 2000
//                    },
//                    repeatMode = RepeatMode.Restart,
//                )
//            )
//        }
//    }
//
//    val dys = dots.map { it.value }
//
//    val travelDistance = with(LocalDensity.current) { 15.dp.toPx() }
//
//    Row(modifier) {
//        dys.forEachIndexed { _, dy ->
//            Box(
//                Modifier
//                    .size(25.dp)
//                    .graphicsLayer {
//                        translationY = -dy * travelDistance
//                    },
//            ) {
//                Row(modifier) {
//                    dots.forEachIndexed { index, _ ->
//                        Box(
//                            Modifier.size(25.dp)
//                        ) {
//                            Box(
//                                Modifier
//                                    .fillMaxSize()
//                                    .background(color = color, shape = CircleShape)
//                            )
//                        }
//
//                        if (index != dys.size - 1) {
//                            Spacer(modifier = Modifier.width(10.dp))
//                        }
//                    }
//                }
//            }
//        }
//
//    }
//}

