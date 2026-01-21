package xyz.jdubiel.migawka.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.BuildConfig
import xyz.jdubiel.migawka.R

@Composable
fun AboutScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.about),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(stringResource(R.string.about_content))
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))
    }
}