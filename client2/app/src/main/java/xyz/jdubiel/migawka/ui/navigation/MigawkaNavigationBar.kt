package xyz.jdubiel.migawka.ui.navigation

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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

fun saveStateAndNavigate(navController: NavHostController, destination: MigawkaScreen) {
    navController.navigate(destination) {
        // Pop up to start destination to avoid large back stack
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        // Prevent duplicate copies of the same destination
        launchSingleTop = true
        // Restore state when returning to previously visited tabs
        restoreState = true
    }
}

@Composable
fun MigawkaNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val gallerySelected = currentDestination?.hasRoute<MigawkaScreen.Gallery>() ?: false
    val folderViewSelected = currentDestination?.hasRoute<MigawkaScreen.FolderView>() ?: false
    val menuSelected = currentDestination?.hasRoute<MigawkaScreen.Menu>() ?: false

    NavigationBar {
        NavigationBarItem(
            selected = gallerySelected,
            icon = { Icon(Icons.Filled.Home, "Gallery") },
            label = { Text("Gallery") },
            onClick = {
                if (!gallerySelected) {
                    saveStateAndNavigate(navController, MigawkaScreen.Gallery)
                }
            }
        )
        NavigationBarItem(
            selected = folderViewSelected,
            icon = { Icon(Icons.Filled.Folder, "Folders") },
            label = { Text("Folders") },
            onClick = {
                if (!folderViewSelected) {
                    saveStateAndNavigate(navController, MigawkaScreen.FolderView(path = "/"))
                }
            }
        )
        NavigationBarItem(
            selected = menuSelected,
            icon = { Icon(Icons.Filled.Menu, "Menu") },
            label = { Text("Menu") },
            onClick = {
                if (!menuSelected) {
                    saveStateAndNavigate(navController, MigawkaScreen.Menu)
                }
            }
        )
    }
}