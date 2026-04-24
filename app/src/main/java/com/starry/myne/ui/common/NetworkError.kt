package com.starry.myne.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionResult
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.starry.myne.R
import custom.AppTheme
import custom.components.Button
import custom.components.Text


@Composable
fun NetworkError(errorMessage: String? = null, onRetryClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colors.background)
            .padding(bottom = 70.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val compositionResult: LottieCompositionResult =
            rememberLottieComposition(
                spec = LottieCompositionSpec.RawRes(R.raw.no_internet_lottie)
            )
        val progressAnimation by animateLottieCompositionAsState(
            compositionResult.value,
            isPlaying = true,
            iterations = LottieConstants.IterateForever,
            speed = 1f
        )

        LottieAnimation(
            composition = compositionResult.value,
            progress = { progressAnimation },
            modifier = Modifier.size(280.dp),
            enableMergePaths = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = errorMessage ?: stringResource(id = R.string.network_error),
            modifier = Modifier
                .padding(top = 10.dp, bottom = 18.dp)
                .fillMaxWidth(),
            color = AppTheme.colors.onBackground,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Button(onClick = onRetryClicked) {
            Text(
                text = stringResource(id = R.string.network_error_retry),
                fontWeight = FontWeight.Medium,
            )
        }
    }
}


@Preview
@ExperimentalComposeUiApi
@Composable
fun NoInternetScreenPreview() {
    NetworkError {}
}


