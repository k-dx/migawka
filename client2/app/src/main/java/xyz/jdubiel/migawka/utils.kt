package xyz.jdubiel.migawka

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.provider.Settings
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.grpc.ManagedChannelBuilder
import xyz.jdubiel.migawka.data.Hash

class Utils {
    companion object {
        suspend fun fetchImageBytesGrpc(id: Hash): MediaItem {
            val serverAddress = "192.168.5.158"
            Log.d("serverAddress", serverAddress)
            val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
                    .usePlaintext()
                    .build()

            try {
                val stub = MigawkaGrpcKt.MigawkaCoroutineStub(channel)
                val request = GetMediaItemRequest.newBuilder()
                        .setId(id.toHex())
                        .build()

                val response = stub.getOptimizedMediaItem(request)

                // Update the UI with the response on the main thread
                Log.i(
                        "gRPC__",
                        "Response for full image: ${response.status}"
                )

                return response.mediaItem

            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch(e: Exception) {
                Log.e("gRPC__", "Error: ${e.message}", e)
                throw e
            } finally {
                try {
                    channel.shutdown()
                } catch (e: InterruptedException) {
                    Log.e("gRPC__", "Error shutting down channel: ${e.message}")
                    channel.shutdownNow()
                    Thread.currentThread().interrupt()
                }
            }
        }


        fun isAutoRotateEnabled(context: Context): Boolean {
            return try {
                Settings.System.getInt(
                    context.contentResolver, Settings.System.ACCELEROMETER_ROTATION
                ) == 1
            } catch (e: Settings.SettingNotFoundException) {
                false
            }
        }

        @Composable
        fun ToggleSystemBars(visible: Boolean) {
            // Get the view from the current composition context
            val view = LocalView.current

            // Safely find the window and insets controller, and remember them
            // This prevents recalculating these on every recomposition
            val windowInsetsController = remember(view) {
                // Safely get the activity from the view's context
                val window = (view.context as? Activity)?.window
                window?.let { WindowCompat.getInsetsController(it, view) }
            }

            // Use SideEffect to perform the action after composition
            SideEffect {
                windowInsetsController?.let { controller ->
                    if (visible) {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    } else {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        // Set the behavior for when the bars are hidden
                        controller.systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }

        fun toggleDeviceOrientation(activity: Activity) {
            // Toggle between portrait and landscape
            if (activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }
}

// https://stackoverflow.com/questions/64675386/how-to-get-activity-in-compose
fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) {
            return context
        }
        context = context.baseContext
    }
    // The context is not an Activity, but it might be the base context of an Activity.
    // This is less common but can happen.
    return context as? Activity
}