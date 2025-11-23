package xyz.jdubiel.migawka.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryScreen
import xyz.jdubiel.migawka.ui.imageGallery.ImageGalleryViewModel
import xyz.jdubiel.migawka.PermissionRationaleScreen

@Composable
fun GalleryPermissionWrapper(
    viewModel: ImageGalleryViewModel,
    onImageClick: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Determine the correct permission to request
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    // 2. State to track the permission status
    var hasPermission by remember {
        mutableStateOf(context.hasPermission(permission))
    }
    // State to track if the rationale should be shown
    var showRationale by remember { mutableStateOf(false) }

    // 3. The permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
            if (!isGranted) {
                // If permission is denied, we show our rationale screen
                showRationale = true
            }
        }
    )

    // This effect will observe lifecycle events.
    // It will run the check whenever the app is RESUMED.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // When the app comes back to the foreground, re-check the permission.
                hasPermission = context.hasPermission(permission)
            }
        }

        // Add the observer to the lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // When the effect leaves the Composition, remove the observer
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 4. The main logic to decide what to show
    when {
        // If we have permission, show the main gallery screen
        hasPermission -> {
            ImageGalleryScreen(
                viewModel = viewModel,
                onImageClick = onImageClick
            )
        }
        // If user denied, show the rationale screen
        showRationale -> {
            // We check if the rationale should be shown. If not, it means the user has permanently declined.
            val isPermanentlyDeclined = !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)

            PermissionRationaleScreen(
                isPermanentlyDeclined = isPermanentlyDeclined,
                onPermissionRequest = {
                    // Reset rationale and launch permission request again
                    showRationale = false
                    permissionLauncher.launch(permission)
                }
            )
        }
        // This is the first launch or rationale is not needed yet.
        // We use LaunchedEffect to request permission automatically on first composition.
        else -> {
            LaunchedEffect(Unit) {
                permissionLauncher.launch(permission)
            }
        }
    }
}

// Checks if the permission is granted
private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}