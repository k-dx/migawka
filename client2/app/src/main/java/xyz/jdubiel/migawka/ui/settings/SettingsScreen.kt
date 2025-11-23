package xyz.jdubiel.migawka.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.regex.Pattern

// TODO: move validation to UserSettingsRepository
fun isIpv4OrIpv4Port(input: String): Boolean {
    // IPv4 octet 0-255
    val octet = "(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)"
    // full IPv4 address
    val ipv4 = "$octet\\.$octet\\.$octet\\.$octet"
    // port 0-65535
    val port = "(?:6553[0-5]|655[0-2]\\d|65[0-4]\\d{2}|6[0-4]\\d{3}|[1-5]\\d{4}|[1-9]\\d{0,3}|0)"
    // either just IPv4 or IPv4:PORT
    val pattern = Pattern.compile("^($ipv4)(?::($port))?$")
    return pattern.matcher(input).matches()
}

// Validation: non-empty and basic URI/host validation
fun isValidServerAddress(input: String): Boolean {
    return isIpv4OrIpv4Port(input)
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = viewModel(factory = SettingsScreenViewModel.Factory)
) {
    val savedAddress by viewModel.serverAddress.collectAsState()

    var text by remember { mutableStateOf(savedAddress) }
    // keep text synced when savedAddress changes externally
    LaunchedEffect(savedAddress) {
        if (savedAddress != text) text = savedAddress
    }

    val isValid = remember(text) { isValidServerAddress(text) }
    val isChanged = text != savedAddress

    Card(
        modifier = modifier.padding(16.dp),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(text = "Settings", style = MaterialTheme.typography.titleSmall)

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Server address") },
                placeholder = { Text("192.168.1.42 or 192.168.1.42:1234") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (text.isBlank()) {
                Text(
                    text = "Server address cannot be empty.",
                    color = MaterialTheme.colorScheme.error
                )
            } else if (!isValid) {
                Text(
                    text = "Enter a valid host (optionally with port), e.g. 192.168.1.42 or 192.168.1.42:8080",
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "Saved address: ${savedAddress.ifBlank { "(none)" }}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { text = savedAddress }) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        viewModel.setServerAddress(text.trim())
                    },
                    enabled = isValid && isChanged
                ) {
                    Text("Save")
                }
            }
        }
    }
}