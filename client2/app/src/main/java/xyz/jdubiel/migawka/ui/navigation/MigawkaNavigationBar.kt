package xyz.jdubiel.migawka.ui.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState


fun getRouteNameForBottomBar(route: String?): String? = route?.substringBefore('/')

fun saveStateAndNavigate(navController: NavHostController, destination: String) {
    navController.navigate(destination) {
        // Pop up to start destination to avoid large back stack
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true  // Save state of popped destinations
        }
        // Prevent duplicate copies of the same destination
        launchSingleTop = true
        // Restore state when returning to previously visited tabs
        restoreState = true
    }
}

// TODO: it would be better to have one source of truth for bottomBarRoutes
// and content of MigawkaNavigationBar
val bottomBarRoutes = setOf(
    MigawkaScreen.Gallery.name,
    MigawkaScreen.FolderView.name,
    MigawkaScreen.Menu.name
)

@Composable
fun MigawkaNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentRouteWithoutArgs = getRouteNameForBottomBar(currentRoute)

    val gallerySelected = currentRouteWithoutArgs == MigawkaScreen.Gallery.name
    val folderViewSelected = currentRouteWithoutArgs == MigawkaScreen.FolderView.name
    val menuSelected = currentRouteWithoutArgs == MigawkaScreen.Menu.name

    NavigationBar() {
        NavigationBarItem(
            selected = gallerySelected,
            icon = { Icon(Icons.Filled.Home, contentDescription = "Gallery") },
            label = { Text("Gallery") },
            onClick = {
                if (!gallerySelected) {
                    saveStateAndNavigate(navController,MigawkaScreen.Gallery.name)
                }
            }
        )
        NavigationBarItem(
            selected = folderViewSelected,
            icon = { Icon(Icons.Filled.Folder, contentDescription = "Folders") },
            label = { Text("Folders") },
            onClick = {
                if (!folderViewSelected) {
                    val rawPath = "/"
                    val encoded = Uri.encode(rawPath)

                    saveStateAndNavigate(navController, "${MigawkaScreen.FolderView.name}/$encoded")
                }
            }
        )
        NavigationBarItem(
            selected = menuSelected,
            icon = { Icon(Icons.Filled.Menu, contentDescription = "Menu") },
            label = { Text("Menu") },
            onClick = {
                if (!menuSelected) {
                    saveStateAndNavigate(navController, MigawkaScreen.Menu.name)
                }
            }
        )
    }
}


