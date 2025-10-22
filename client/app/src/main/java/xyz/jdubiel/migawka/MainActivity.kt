package xyz.jdubiel.migawka

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.util.Log
import android.view.MenuItem
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.preference.PreferenceManager
import xyz.jdubiel.migawka.databinding.ActivityMainBinding

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

//        TODO: recreate channel when the IP changes (in the settings)
//        TODO: add validation to the IP address when it is set in the settings
//        val serverAddress = PreferenceManager.getDefaultSharedPreferences(this)
//            .getString("server_address", "192.168.5.158")
        val serverAddress = "192.168.5.158"
        Log.d("serverAddress", serverAddress)
        val channel = ManagedChannelBuilder.forAddress(serverAddress, 50051)
            .usePlaintext()
            .build()

        val stub = GreeterGrpcKt.GreeterCoroutineStub(channel)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Sending gRPC message", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()

            lifecycleScope.launch {
                try {
                    val request = HelloRequest.newBuilder()
                        .setName("Android User")
                        .build()

                    val response = stub.sayHello(request)

                    // Update the UI with the response on the main thread
                    Log.i("gRPC", "Response: ${response.message}")

                } catch (e: Exception) {
                    Log.e("gRPC", "Error: ${e.message}", e)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    fun openSettings(): Boolean {
        Snackbar.make(binding.root, "Settings clicked", Snackbar.LENGTH_LONG)
            .setAnchorView(R.id.fab).show()
        navController.navigate(R.id.settingsFragment)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> openSettings()
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}