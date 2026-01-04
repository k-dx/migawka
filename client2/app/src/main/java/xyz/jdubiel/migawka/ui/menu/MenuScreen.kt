package xyz.jdubiel.migawka.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

@Composable
fun MenuScreen(
    onSecondScreenButtonClick: () -> Unit,
    onSettingsButtonClick: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier.padding(horizontal = 8.dp)) {
        Column() {
            Text(
                stringResource(R.string.menu),
                    style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
                )
            Button(onClick = { onSettingsButtonClick() }) {
                Text(text = stringResource(R.string.settings))
            }
            Button(onClick = { onSecondScreenButtonClick() }) {
                Text(text = "Second screen")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    MigawkaTheme {
        MenuScreen(
            onSecondScreenButtonClick = {},
            onSettingsButtonClick = {},
            modifier = Modifier
        )
    }
}