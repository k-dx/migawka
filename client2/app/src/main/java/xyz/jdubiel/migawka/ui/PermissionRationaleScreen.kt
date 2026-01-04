package xyz.jdubiel.migawka.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.jdubiel.migawka.R
import xyz.jdubiel.migawka.ui.theme.MigawkaTheme

@Composable
fun PermissionRationaleScreen(
    isPermanentlyDeclined: Boolean,
    onPermissionRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.permission_required),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.media_permission_explanation),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        val buttonText =
            if (isPermanentlyDeclined) stringResource(R.string.open_settings)
            else stringResource(R.string.grant_permission)
        val onClickAction = if (isPermanentlyDeclined) {
            { context.openAppSettings() }
        } else {
            onPermissionRequest
        }
        Button(onClick = onClickAction) {
            Text(buttonText)
        }
    }
}

// Helper function to open app settings
private fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    )
    startActivity(intent)
}

@Preview(showBackground = true)
@Composable
fun PermissionRationaleScreenPreview() {
    MigawkaTheme {
        PermissionRationaleScreen(
            isPermanentlyDeclined = false,
            onPermissionRequest = {}
        )
    }
}