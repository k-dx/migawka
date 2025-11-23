package xyz.jdubiel.migawka.ui.navigation

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.jdubiel.migawka.ImageGalleryViewModel
import xyz.jdubiel.migawka.Migawka
import xyz.jdubiel.migawka.SecondScreen
import xyz.jdubiel.migawka.SettingsScreen
import xyz.jdubiel.migawka.Sha256
import xyz.jdubiel.migawka.SingleMediaViewScreen
import xyz.jdubiel.migawka.TAG

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

    NavHost(
        navController = navController,
        startDestination = MigawkaScreen.Gallery.name,
        modifier = modifier
    ) {
        composable(route = MigawkaScreen.Gallery.name) {
            Migawka(
                onSettingsButtonClick = { navController.navigate(MigawkaScreen.Settings.name) },
                onSecondScreenButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                viewModel = imageGalleryViewModel,
                onImageClick = { imageId: String ->
                    Log.d(TAG, "onImageClick, imageId = $imageId")
                    navController.navigate("${MigawkaScreen.SingleMediaView.name}/$imageId")
                }
            )
        }

        composable(route = MigawkaScreen.Second.name) {
            SecondScreen(content = "Second screen! Yay!")
        }

        composable(route = MigawkaScreen.Settings.name) {
            SettingsScreen()
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
                Text("Error: initialImageId is null. The image could not be displayed.")
            }
        }
    }
}