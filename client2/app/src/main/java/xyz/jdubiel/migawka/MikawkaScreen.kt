package xyz.jdubiel.migawka

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

enum class MigawkaScreen {
    Start,
    Second
}


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = MigawkaScreen.Start.name) {
                Migawka(
                    name = "Android",
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Second.name) }
                )
            }

            composable(route = MigawkaScreen.Second.name) {
                SecondScreen(content = "Second screen! Yay!")
            }
        }
    }
}


@Composable
fun MyButton(onButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onButtonClick,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.settings)
        )
    }
}

@Composable
fun MyText(name: String, counter: Int, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name! x $counter",
        modifier = modifier
    )
}

@Composable
fun Migawka(name: String, onSettingsButtonClick: () -> Unit, modifier: Modifier = Modifier) {
    var counter by remember { mutableIntStateOf(0) }
    Column {
        MyText(name = name, counter = counter, modifier = modifier)
        MyButton(onButtonClick = { counter++ }, modifier = modifier)
        Button(onClick = { onSettingsButtonClick() }) {
            Text(text = stringResource(R.string.settings))
        }
    }

}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Migawka(name = "Android", onSettingsButtonClick = {})
    }
}