package com.route.readers.ui.screens.splash
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.route.readers.R
import com.route.readers.ui.theme.ReadersTheme

@Composable
fun SplashScreenUI() {
    Column(        modifier = Modifier
        .fillMaxSize()
        .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.mipmap.readerslogo),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )

    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    ReadersTheme {
        SplashScreenUI()
    }
}
