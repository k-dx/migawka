package xyz.jdubiel.migawka.ui.navigation

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import xyz.jdubiel.migawka.Migawka
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.hasher
import xyz.jdubiel.migawka.ui.SecondScreen
import xyz.jdubiel.migawka.ui.folderView.FolderScreen
import xyz.jdubiel.migawka.ui.folderView.FolderScreenViewModel
import xyz.jdubiel.migawka.ui.folderView.FolderScreenViewModelFactory
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import xyz.jdubiel.migawka.ui.settings.SettingsScreen
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewScreen

enum class MigawkaScreen {
    Second,
    Gallery,
    FolderView,
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
    val initialFolderPath = "/"

    // This is a way of passing the FolderScreenViewModel that is on the top of the navigation
    // stack to the SingleMediaViewScreen. Might not be the best solution, but it works.
    var topFolderScreenViewModel: FolderScreenViewModel? = null

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding -> // TODO: this is a bit ugly
        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Gallery.name,
        ) {
            composable(route = MigawkaScreen.Gallery.name) {
                Migawka(
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Settings.name) },
                    onSecondScreenButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                    onFolderViewButtonClick = {
                        val rawPath = "/"
                        val encoded = Uri.encode(rawPath)
                        navController.navigate("${MigawkaScreen.FolderView.name}/$encoded") },
                    viewModel = imageGalleryViewModel,
                    onImageClick = { imageId: String ->
                        Log.d(TAG, "onImageClick, imageId = $imageId")
                        navController.navigate("${MigawkaScreen.SingleMediaView.name}/$imageId")
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable(
                route = "${MigawkaScreen.FolderView.name}/{$initialFolderPath}",
                arguments = listOf(navArgument(initialFolderPath) { type = NavType.StringType })
            ) { backStackEntry ->
                val path = backStackEntry.arguments?.getString(initialFolderPath)
                if (path != null) {
                    val decoded = Uri.decode(path)
                    val folderScreenViewModel: FolderScreenViewModel = viewModel(
                        factory = FolderScreenViewModelFactory(
                            path,
                            30,
                            LocalContext.current.applicationContext as Application
                        )
                    )
                    topFolderScreenViewModel = folderScreenViewModel

                    FolderScreen(
                        path = decoded,
                        navigateToPath = { path ->
                            val encoded = Uri.encode(path)
                            val targetRoute = "${MigawkaScreen.FolderView.name}/$encoded"

                            // Try to pop back to target first
                            val popped = navController.popBackStack(
                                route = targetRoute,
                                inclusive = false
                            )

                            if (!popped) {
                                // Target not in back stack, navigate to it
                                navController.navigate(targetRoute)
                            }
                        },
                        onImageClick = { imageId: String ->
                            Log.d("FolderScreen", "clicked on image with id $imageId")
                            navController.navigate("${MigawkaScreen.SingleMediaView.name}/$imageId")
                        },
                        viewModel = folderScreenViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                } else {
                    Log.e("FolderView", "path is null")
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Text("Error: path is null. The folder could not be displayed.")
                    }
                }
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
                    Log.d("SingleMediaViewScreen", "initialImageId = $initialImageId, using folderScreenViewModel = ${topFolderScreenViewModel != null}")
                    SingleMediaViewScreen(
                        viewModel = if (topFolderScreenViewModel != null)
                            topFolderScreenViewModel!!
                            else imageGalleryViewModel,
                        initialImageId = hasher.fromHex(initialImageId)
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