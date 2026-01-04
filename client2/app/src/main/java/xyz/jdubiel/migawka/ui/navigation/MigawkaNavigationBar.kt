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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import xyz.jdubiel.migawka.R
import kotlin.reflect.KClass

data class NavBarEntry(
    val destination: MigawkaScreen,
    val icon: @Composable () -> Unit,
    val label: @Composable () -> Unit,
    val onClick: (NavHostController) -> Unit
)

val navigationBarEntries = listOf(
    NavBarEntry(
        destination = MigawkaScreen.Gallery,
        icon = { Icon(Icons.Filled.Home, stringResource(R.string.gallery)) },
        label = { Text(stringResource(R.string.gallery)) },
        onClick = { navController ->
            saveStateAndNavigate(navController, MigawkaScreen.Gallery) }
    ),
    NavBarEntry(
        destination = MigawkaScreen.FolderView(path = "/"),
        icon = { Icon(Icons.Filled.Folder, stringResource(R.string.folders)) },
        label = { Text(stringResource(R.string.folders)) },
        onClick = { navController ->
            saveStateAndNavigate(navController, MigawkaScreen.FolderView(path = "/"))
        }
    ),
    NavBarEntry(
        destination = MigawkaScreen.Menu,
        icon = { Icon(Icons.Filled.Menu, stringResource(R.string.menu)) },
        label = { Text(stringResource(R.string.menu)) },
        onClick = { navController ->
            saveStateAndNavigate(navController, MigawkaScreen.Menu)
        }
    )
)

fun shouldShowBottomNavBar(destination: NavDestination): Boolean {
    val destinationsWithNavBar: List<KClass<*>> = navigationBarEntries.map {
        it.destination::class
    }

    for (dest in destinationsWithNavBar) {
        if (destination.hasRoute(dest)) {
            return true
        }
    }
    return false
}

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

    NavigationBar {
        navigationBarEntries.map {
            NavigationBarItem(
                selected = currentDestination?.hasRoute(it.destination::class) ?: false,
                icon = it.icon,
                label = it.label,
                onClick = { it.onClick(navController) }
            )
        }
    }
}