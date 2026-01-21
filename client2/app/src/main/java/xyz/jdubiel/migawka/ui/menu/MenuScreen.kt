package xyz.jdubiel.migawka.ui.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
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
    onNavigateToAbout: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.menu),
                style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
            )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings)) },
            leadingContent = { Icon(Icons.Default.Settings, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { onSettingsButtonClick() }
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.about)) },
            leadingContent = { Icon(Icons.Default.Info, contentDescription = null) },
            trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
            modifier = Modifier.clickable { onNavigateToAbout() }
        )

        // Used for development purposes
//        Button(onClick = { onSecondScreenButtonClick() }) {
//            Text(text = "Second screen")
//        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuScreenPreview() {
    MigawkaTheme {
        MenuScreen(
            onSecondScreenButtonClick = {},
            onSettingsButtonClick = {},
            onNavigateToAbout = {},
            modifier = Modifier
        )
    }
}