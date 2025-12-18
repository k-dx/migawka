package xyz.jdubiel.migawka

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import xyz.jdubiel.migawka.ui.navigation.MigawkaNavHost

@Composable
fun MigawkaApp(navController: NavHostController = rememberNavController()) {
    MigawkaNavHost(
        navController = navController,
        modifier = Modifier
    )
}