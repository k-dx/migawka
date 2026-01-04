package xyz.jdubiel.migawka.ui.navigation

import android.app.Application
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.TAG
import xyz.jdubiel.migawka.hasher
import xyz.jdubiel.migawka.ui.SecondScreen
import xyz.jdubiel.migawka.ui.folderView.EntriesState
import xyz.jdubiel.migawka.ui.folderView.FolderScreen
import xyz.jdubiel.migawka.ui.folderView.FolderScreenViewModel
import xyz.jdubiel.migawka.ui.folderView.FolderScreenViewModelFactory
import xyz.jdubiel.migawka.ui.imageGallery.Gallery
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModelFactory
import xyz.jdubiel.migawka.ui.menu.MenuScreen
import xyz.jdubiel.migawka.ui.settings.SettingsScreen
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewScreen
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewScreenForTimelineViewModelFactory
import xyz.jdubiel.migawka.ui.singleMedia.SingleMediaViewScreenViewModel

sealed interface MigawkaScreen {
    @Serializable
    data object Second : MigawkaScreen

    @Serializable
    data object Gallery : MigawkaScreen

    @Serializable
    data object Menu : MigawkaScreen

    @Serializable
    data object Settings : MigawkaScreen

    @Serializable
    data class FolderView(val path: String) : MigawkaScreen

    @Serializable
    data class SingleMediaViewForTimeline(val initialMediaId: String) : MigawkaScreen

    @Serializable
    data class SingleMediaViewForFolder(val path: String, val initialMediaId: String) :
        MigawkaScreen
}

@Composable
fun MigawkaNavHost(
    navController: NavHostController,
    imageGalleryViewModel: ImageGalleryViewModel = viewModel(
        factory = ImageGalleryViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    ),
    modifier: Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (currentDestination != null && shouldShowBottomNavBar(currentDestination)) {
                MigawkaNavigationBar(navController)
            }
        }
    ) { innerPadding -> // TODO: this is a bit ugly
        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Gallery,
        ) {
            composable<MigawkaScreen.Gallery> {
                Gallery(
                    viewModel = imageGalleryViewModel,
                    onImageClick = { imageId: String ->
                        Log.d(TAG, "onImageClick, imageId = $imageId")
                        navController.navigate(MigawkaScreen.SingleMediaViewForTimeline(imageId))
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            composable<MigawkaScreen.FolderView> { backStackEntry ->
                val folderView: MigawkaScreen.FolderView = backStackEntry.toRoute()
                val path = folderView.path

                val folderScreenViewModel: FolderScreenViewModel = viewModel(
                    viewModelStoreOwner = backStackEntry,
                    factory = FolderScreenViewModelFactory(
                        path,
                        LocalContext.current.applicationContext as Application
                    )
                )

                FolderScreen(
                    path = path,
                    navigateToPath = { path ->
                        val targetRoute = MigawkaScreen.FolderView(path)

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
                        navController.navigate(
                            MigawkaScreen.SingleMediaViewForFolder(
                                path,
                                imageId
                            )
                        )
                    },
                    viewModel = folderScreenViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }


            composable<MigawkaScreen.Second> {
                SecondScreen(modifier = Modifier.padding(innerPadding))
            }

            composable<MigawkaScreen.Settings> {
                SettingsScreen(modifier = Modifier.padding(innerPadding))
            }

            composable<MigawkaScreen.SingleMediaViewForFolder> { backStackEntry ->
                val args: MigawkaScreen.SingleMediaViewForFolder = backStackEntry.toRoute()
                val path = args.path
                val initialMediaId = args.initialMediaId

                // traverse nav stack and find the topmost one with FolderScreenViewModel
                val previousEntry = navController.previousBackStackEntry

                if (previousEntry == null || !previousEntry.destination.hasRoute(MigawkaScreen.FolderView::class)) {
                    Log.e(
                        MigawkaScreen.SingleMediaViewForFolder::class.simpleName,
                        "previousEntry is null"
                    )
                } else {
                    Log.d(
                        MigawkaScreen.SingleMediaViewForFolder::class.simpleName,
                        "path = $path, initialMediaId = $initialMediaId"
                    )

                    // Access the existing FolderScreenViewModel
                    val topFolderScreenViewModel: FolderScreenViewModel =
                        viewModel(
                            viewModelStoreOwner = previousEntry,
                            factory = FolderScreenViewModelFactory(
                                path,
                                LocalContext.current.applicationContext as Application
                            )
                        )

                    val entriesState by topFolderScreenViewModel.mediaEntries.collectAsState()

                    when (val state = entriesState) {
                        is EntriesState.Success -> {
                            val vm: SingleMediaViewScreenViewModel = viewModel(
                                factory = SingleMediaViewScreenForTimelineViewModelFactory(
                                    LocalContext.current.applicationContext as Application,
                                    state.data,
                                    initialImageId = hasher.fromString(initialMediaId)
                                )
                            )

                            SingleMediaViewScreen(viewModel = vm)
                        }

                        else -> {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(stringResource(R.string.the_list_of_files_could_not_be_loaded))
                            }
                        }
                    }


                }
            }

            composable<MigawkaScreen.SingleMediaViewForTimeline> { backStackEntry ->
                val initialMediaId: String = backStackEntry
                    .toRoute<MigawkaScreen.SingleMediaViewForTimeline>()
                    .initialMediaId

                Log.d(
                    MigawkaScreen.SingleMediaViewForTimeline::class.simpleName,
                    "initialMediaId = $initialMediaId"
                )

                val vm: SingleMediaViewScreenViewModel = viewModel(
                    factory = SingleMediaViewScreenForTimelineViewModelFactory(
                        LocalContext.current.applicationContext as Application,
                        imageGalleryViewModel.entries.collectAsState().value,
                        initialImageId = hasher.fromString(initialMediaId)
                    )
                )

                SingleMediaViewScreen(viewModel = vm)
            }

            composable<MigawkaScreen.Menu> {
                MenuScreen(
                    modifier = Modifier.padding(innerPadding),
                    onSecondScreenButtonClick = { navController.navigate(MigawkaScreen.Second) },
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Settings) }
                )
            }
        }
    }
}