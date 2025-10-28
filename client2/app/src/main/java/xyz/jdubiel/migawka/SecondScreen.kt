package xyz.jdubiel.migawka

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun SecondScreen(content: String) {
    Text(content)
}

@Preview(showBackground = true)
@Composable
fun SecondScreenPreview() {
    SecondScreen("Second screen!")
}