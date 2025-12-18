package xyz.jdubiel.migawka

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModelFactory
import xyz.jdubiel.migawka.ui.navigation.MigawkaNavHost


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController(),
    imageGalleryViewModel: ImageGalleryViewModel = viewModel(
        factory = ImageGalleryViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    // TODO: probably MikawkaNavHost should be moved outside Scaffold (?)
//    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        MigawkaNavHost(
            navController = navController,
            imageGalleryViewModel = imageGalleryViewModel,
            modifier = Modifier //.padding(innerPadding)
        )
//    }
}