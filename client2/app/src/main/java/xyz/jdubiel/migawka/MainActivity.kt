package xyz.jdubiel.migawka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

const val TAG = "MyDebug"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MigawkaTheme {
                MigawkaApp()

            }
        }
    }
}
