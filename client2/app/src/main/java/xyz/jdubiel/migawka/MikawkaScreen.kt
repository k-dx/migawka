package xyz.jdubiel.migawka

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

enum class MigawkaScreen {
    Second,
    Gallery,
    SingleMediaView
}


@Composable
fun MigawkaApp(
    navController: NavHostController = rememberNavController(),
    imageListViewModel: ImageListViewModel = ImageListViewModel()
) {
    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        val initialIndexArg = "initialIndex"

        NavHost(
            navController = navController,
            startDestination = MigawkaScreen.Gallery.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(route = MigawkaScreen.Gallery.name) {
                Migawka(
                    onSettingsButtonClick = { navController.navigate(MigawkaScreen.Second.name) },
                    viewModel = imageListViewModel,
                    onImageClick = { index ->
                        Log.d(TAG, "onImageClick, index = $index")
                        navController.navigate("${MigawkaScreen.SingleMediaView.name}/$index")
                    }
                )
            }

            composable(route = MigawkaScreen.Second.name) {
                SecondScreen(content = "Second screen! Yay!")
            }

            composable(
                route = "${MigawkaScreen.SingleMediaView.name}/{$initialIndexArg}",
                arguments = listOf(navArgument(initialIndexArg) { type = NavType.IntType })
            ) { backStackEntry ->
                // Extract the index argument
                val initialIndex = backStackEntry.arguments?.getInt(initialIndexArg) ?: 0
                SingleMediaViewScreen(
                    viewModel = imageListViewModel,
                    initialIndex = initialIndex
                )
            }
        }
    }
}

@Composable
fun Migawka(
    viewModel: ImageListViewModel,
    onImageClick: (Int) -> Unit,
    onSettingsButtonClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Button(onClick = { onSettingsButtonClick() }) {
            Text(text = stringResource(R.string.settings))
        }
        GalleryPermissionWrapper(
            viewModel = viewModel,
            onImageClick = onImageClick
        )
    }

}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Migawka(
            viewModel = viewModel<ImageListViewModel>(),
            onSettingsButtonClick = {},
            onImageClick = {}
        )
    }
}