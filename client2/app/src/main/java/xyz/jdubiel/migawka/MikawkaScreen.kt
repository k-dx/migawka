package xyz.jdubiel.migawka

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme
import androidx.core.net.toUri

enum class MigawkaScreen {
    Start,
    Second,
    Gallery,
    SingleMediaView
}


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController()
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val uriArg = "imageUri"

        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Start.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = MigawkaScreen.Start.name) {
                Migawka(
                    name = "Android",
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                    onGalleryButtonClick = { navController.navigate(MigawkaScreen.Gallery.name) }
                )
            }

            composable(route = MigawkaScreen.Second.name) {
                SecondScreen(content = "Second screen! Yay!")
            }

            composable(route = MigawkaScreen.Gallery.name) {
                GalleryPermissionWrapper(
                    onImageClick = { imageUri ->
                        // URL-encode the URI string to handle special characters safely
                        val encodedUri = Uri.encode(imageUri.toString())
                        navController.navigate("${MigawkaScreen.SingleMediaView.name}/$encodedUri")
                    }
                )
            }

            composable(
                route = "${MigawkaScreen.SingleMediaView.name}/{$uriArg}",
                arguments = listOf(navArgument(uriArg) { type = NavType.StringType })
            ) { backStackEntry ->
                // Extract the argument, decode it, and pass it to the screen
                val imageUriString = backStackEntry.arguments?.getString(uriArg)
                val decodedUri = imageUriString?.toUri()
                if (decodedUri != null) {
                    SingleMediaViewScreen(imageUri = decodedUri)
                } else {
                    Text("Error: image not found.")
                }
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
fun Migawka(
    name: String,
    onGalleryButtonClick: () -> Unit,
    onSettingsButtonClick: () -> Unit,
    modifier: Modifier = Modifier) {
    var counter by remember { mutableIntStateOf(0) }
    Column {
        MyText(name = name, counter = counter, modifier = modifier)
        MyButton(onButtonClick = { counter++ }, modifier = modifier)
        Button(onClick = { onSettingsButtonClick() }) {
            Text(text = stringResource(R.string.settings))
        }
        Button(onClick = { onGalleryButtonClick() }) {
            Text(text = "Gallery")
        }
    }

}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Migawka(name = "Android", onSettingsButtonClick = {}, onGalleryButtonClick = {})
    }
}