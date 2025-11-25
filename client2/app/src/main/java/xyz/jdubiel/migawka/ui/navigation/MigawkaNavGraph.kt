package xyz.jdubiel.migawka.ui.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.jdubiel.migawka.Migawka
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.data.Sha256
import xyz.jdubiel.migawka.ui.SecondScreen
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import xyz.jdubiel.migawka.ui.settings.SettingsScreen
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewScreen

enum class MigawkaScreen {
    Second,
    Gallery,
    SingleMediaView,
    Settings
}

@Composable
fun MigawkaNavHost(
    navController: NavHostController,
    imageGalleryViewModel: ImageGalleryViewModel,
    modifier: Modifier,
) {
    val initialImageIdArg = "initialImageId"

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding -> // TODO: this is a bit ugly
        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Gallery.name,
        ) {
            composable(route = MigawkaScreen.Gallery.name) {
                Migawka(
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Settings.name) },
                    onSecondScreenButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                    viewModel = imageGalleryViewModel,
                    onImageClick = { imageId: String ->
                        Log.d(TAG, "onImageClick, imageId = $imageId")
                        navController.navigate("${MigawkaScreen.SingleMediaView.name}/$imageId")
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(route = MigawkaScreen.Second.name) {
                SecondScreen(
                    modifier = Modifier.padding(innerPadding),
                    content = "Second screen! Yay!"
                )
            }

            composable(route = MigawkaScreen.Settings.name) {
                SettingsScreen(modifier = Modifier.padding(innerPadding))
            }

            composable(
                route = "${MigawkaScreen.SingleMediaView.name}/{$initialImageIdArg}",
                arguments = listOf(navArgument(initialImageIdArg) { type = NavType.StringType })
            ) { backStackEntry ->
                val initialImageId = backStackEntry.arguments
                    ?.getString(initialImageIdArg)
                if (initialImageId != null) {
                    SingleMediaViewScreen(
                        viewModel = imageGalleryViewModel,
                        initialImageId = Sha256.fromHex(initialImageId)
                    )
                } else {
                    Log.e("SingleMediaViewScreen", "initialImageId is null")
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Text("Error: initialImageId is null. The image could not be displayed.")
                    }
                }
            }
        }
    }
}