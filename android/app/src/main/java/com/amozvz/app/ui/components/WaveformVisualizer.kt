package com.amozvz.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.amozvz.app.ui.theme.MicRecordingRed

@Composable
fun WaveformVisualizer(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barCount = 12
        val infiniteTransition = rememberInfiniteTransition(label = "waveform")

        for (i in 0 until barCount) {
            val animatedHeight by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = if (isRecording) (20f + (i % 5) * 6f) else 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 350 + (i * 40), easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar_$i"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .background(
                        color = if (isRecording) MicRecordingRed else MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
