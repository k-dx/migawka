package xyz.jdubiel.migawka.ui.settings

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.map
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.ui.singleMedia.locale
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle


fun isValidServerPort(input: String): Boolean {
    val port = input.toIntOrNull(10) ?: return false
    return port in 0..65535
}

// Validation: only check if non-empty, since this could be any hostname or IP address
fun isValidServerAddress(input: String): Boolean {
    return !input.trim().isEmpty()
}

@Composable
fun RestartOnBack(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = enabled) {
        // custom back action: show confirmation dialog
        showDialog = true
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(stringResource(R.string.apply_changes)) },
            text = { Text(stringResource(R.string.the_app_will_be_restarted_to_apply_the_changes)) },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    onBack()
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
fun Section(
    text: String,
    setText: (String) -> Unit,
    isTextValid: Boolean,
    label: String,
    placeholder: String?,
    hintIfBlank: String,
    hintIfInvalid: String,
    hintIfOk: String
) {
    OutlinedTextField(
        value = text,
        onValueChange = { setText(it) },
        label = { Text(label) },
        placeholder = if (placeholder != null) { -> Text(placeholder) } else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(8.dp))

    if (text.isBlank()) {
        Text(
            text = hintIfBlank,
            color = MaterialTheme.colorScheme.error
        )
    } else if (!isTextValid) {
        Text(
            text = hintIfInvalid,
            color = MaterialTheme.colorScheme.error
        )
    } else {
        Text(
            text = hintIfOk,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel
) {
    val savedAddress by viewModel.serverAddress.collectAsState()
    val savedPort by viewModel.serverPort.map { it.toString(10) }.collectAsState("")
    val savedAuthToken by viewModel.authToken.collectAsState()

    var textAddress by remember { mutableStateOf(savedAddress) }
    var textPort by remember { mutableStateOf(savedPort) }
    var textAuthToken by remember { mutableStateOf(savedAuthToken) }
    // keep text synced when savedAddress changes externally
    LaunchedEffect(savedAddress) {
        if (savedAddress != textAddress) textAddress = savedAddress
    }
    LaunchedEffect(savedPort) {
        if (savedPort != textPort)  textPort = savedPort
    }
    LaunchedEffect(savedAuthToken) {
        if (savedAuthToken != textAuthToken) textAuthToken = savedAuthToken
    }

    val isAddressValid = remember(textAddress) { isValidServerAddress(textAddress) }
    val isPortValid = remember(textPort) { isValidServerPort(textPort) }

    val isChanged =
        (textAddress != savedAddress) || (textPort != savedPort) || (textAuthToken != savedAuthToken)

    val databaseCleared by viewModel.databaseCleared.collectAsState()
    val scrollState = rememberScrollState()

    Column(modifier = modifier
        .padding(16.dp)
        .verticalScroll(scrollState)) {
        Text(
            text = stringResource(R.string.settings),
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        Card(

            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.server_settings),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Section(
                    text = textAddress,
                    setText = { textAddress = it },
                    isTextValid = isAddressValid,
                    label = stringResource(R.string.settings_server_address_label),
                    placeholder = stringResource(R.string.settings_example_server_ip),
                    hintIfBlank = stringResource(R.string.settings_server_address_cannot_be_empty),
                    hintIfInvalid = stringResource(
                        R.string.settings_enter_a_valid_host,
                        stringResource(R.string.settings_example_server_ip)
                    ),
                    hintIfOk = stringResource(R.string.settings_saved_address, savedAddress)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Section(
                    text = textPort,
                    setText = { textPort = it },
                    isTextValid = isPortValid,
                    label = stringResource(R.string.settings_server_port),
                    placeholder = stringResource(R.string.settings_example_port),
                    hintIfBlank = stringResource(R.string.settings_server_port_cannot_be_empty),
                    hintIfInvalid = stringResource(
                        R.string.settings_enter_a_valid_port,
                        stringResource(R.string.settings_example_port)
                    ),
                    hintIfOk = stringResource(R.string.settings_saved_port, savedPort)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Section(
                    text = textAuthToken,
                    setText = { textAuthToken = it },
                    isTextValid = true,
                    label = stringResource(R.string.settings_auth_token),
                    placeholder = null,
                    hintIfBlank = stringResource(R.string.settings_auth_token_cannot_be_empty),
                    hintIfInvalid = stringResource(
                        R.string.settings_enter_a_valid_port,
                        stringResource(R.string.settings_example_port)
                    ),
                    hintIfOk = stringResource(
                        R.string.settings_saved_auth_token,
                        savedAuthToken
                    ),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            textAddress = savedAddress
                            textPort = savedPort
                        },
                        enabled = isChanged
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            viewModel.setServerAddress(textAddress.trim())
                            viewModel.setServerPort(textPort.trim().toInt())
                            viewModel.setAuthToken(textAuthToken.trim())
                        },
                        enabled = isAddressValid && isPortValid && isChanged
                    ) {
                        Text(stringResource(R.string.save))
                    }

                    Spacer(modifier = Modifier.height(16.dp))


                }


            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.synchronization),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                var showTimePicker by remember { mutableStateOf(false) }
                // TODO: use dataStore to store this
                var isSyncEnabled by remember { mutableStateOf(false) }
                var onlyUnmeteredConnections by remember { mutableStateOf(true) }
                var onlyWhenCharging by remember { mutableStateOf(false) }
                var savedTime by remember { mutableStateOf(LocalTime.of(2, 0)) }

                val timePickerState = rememberTimePickerState(
                    initialHour = 2,
                    initialMinute = 0
                )

                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    // If user denies permission, turn the switch back off
                    if (!isGranted) {
                        isSyncEnabled = false
                        Toast.makeText(
                            context,
                            "Permission denied. Sync not enabled.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        isSyncEnabled = true
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sync photos to the server")

                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { checked ->
                            if (checked) {
                                // Pre-Android 13, permission is granted at install time
                                // Check if we need to ask for permission (Android 13+)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    val status = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )

                                    if (status == PackageManager.PERMISSION_GRANTED) {
                                        isSyncEnabled = true
                                    } else {
                                        // Trigger the system dialog
                                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        // Note: We don't set isSyncEnabled = true yet.
                                        // We wait for the launcher result above.
                                    }
                                }
                            } else {
                                isSyncEnabled = false
                            }
                            
                            if (isSyncEnabled) {
                                viewModel.scheduleSync(savedTime, onlyUnmeteredConnections, onlyWhenCharging)
                            } else {
                                viewModel.cancelSync()
                            }
                        }
                    )
                }

                if (isSyncEnabled) {
                    if (showTimePicker) {
                        val onDismiss = { showTimePicker = false }
                        val onConfirm = {
                            savedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                            viewModel.scheduleSync(savedTime, onlyUnmeteredConnections, onlyWhenCharging)
                            showTimePicker = false
                        }
                        AlertDialog(
                            onDismissRequest = onDismiss,
                            dismissButton = {
                                TextButton(onClick = { onDismiss() }) {
                                    Text("Dismiss")
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { onConfirm() }) {
                                    Text("OK")
                                }
                            },
                            text = { TimePicker(state = timePickerState) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val formatter: DateTimeFormatter = DateTimeFormatter
                            .ofLocalizedTime(FormatStyle.SHORT)
                            .withLocale(locale)
                        Text("Everyday at ${savedTime.format(formatter)}")

                        Button(onClick = { showTimePicker = true }) {
                            Text("Set time")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Only unmetered connections")

                        Checkbox(
                            checked = onlyUnmeteredConnections,
                            onCheckedChange = {
                                onlyUnmeteredConnections = it
                                viewModel.scheduleSync(savedTime, onlyUnmeteredConnections, onlyWhenCharging)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Only when charging")

                        Checkbox(
                            checked = onlyWhenCharging,
                            onCheckedChange = {
                                onlyWhenCharging = it
                                viewModel.scheduleSync(savedTime, onlyUnmeteredConnections, onlyWhenCharging)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            elevation = CardDefaults.elevatedCardElevation()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.local_settings),
                    style = MaterialTheme.typography.titleSmall
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        enabled = !databaseCleared,
                        onClick = {
                            viewModel.clearLocalMediaDatabase()
                        }
                    ) {
                        Text(stringResource(R.string.clear_internal_media_database))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel = viewModel(factory = SettingsScreenViewModel.Factory)
) {
    val context = LocalContext.current
    val restartOnBack = viewModel.settingsModified.collectAsStateWithLifecycle()

    Column(modifier = modifier) {
        RestartOnBack(enabled = restartOnBack.value, onBack = {
            // restart the app
            // https://stackoverflow.com/questions/72932093/jetpack-compose-is-there-a-way-to-restart-whole-app-programmatically
            val packageManager: PackageManager = context.packageManager
            val intent: Intent = packageManager.getLaunchIntentForPackage(context.packageName)!!
            val componentName: ComponentName = intent.component!!
            val restartIntent: Intent = Intent.makeRestartActivityTask(componentName)
            context.startActivity(restartIntent)
            Runtime.getRuntime().exit(0)

        })
        SettingsContent(viewModel = viewModel)
    }
}