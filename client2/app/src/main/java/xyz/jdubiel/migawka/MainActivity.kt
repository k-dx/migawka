package xyz.jdubiel.migawka

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            MigawkaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Migawka(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Migawka(name: String, modifier: Modifier = Modifier) {
    var counter by remember { mutableIntStateOf(0) }
    Column {
        Text(
            text = "Hello $name! x $counter",
            modifier = modifier
        )
        Button(onClick = { counter++ }) {
            Text(
                text = stringResource(R.string.settings)
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun MigawkaPreview() {
    MigawkaTheme {
        Migawka("Android")
    }
}