package xyz.jdubiel.migawka.ui.menu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import xyz.jdubiel.migawka.R

@Composable
fun MenuScreen(
    onSecondScreenButtonClick: () -> Unit,
    onSettingsButtonClick: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        Column() {
            Text("Menu")
            Button(onClick = { onSettingsButtonClick() }) {
                Text(text = stringResource(R.string.settings))
            }
            Button(onClick = { onSecondScreenButtonClick() }) {
                Text(text = "Second screen")
            }
        }
    }
}