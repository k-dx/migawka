package xyz.jdubiel.migawka.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

@Composable
fun SliderWithLabels(value: Float, onValueChange: (Float) -> Unit, options: List<UInt>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        options.forEach { option ->
            Text(
                text = option.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Slider(
        value = value,
        onValueChange = { onValueChange(it) },
        valueRange = 0f..(options.size - 1).toFloat(),
        steps = options.size - 2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp),
    )
}

@Preview(showBackground = true)
@Composable
fun SliderPreview() {
    MigawkaTheme {
        SliderWithLabels(
            value = 1f,
            onValueChange = {},
            options = listOf(6u, 5u, 4u, 3u, 2u)
        )
    }
}