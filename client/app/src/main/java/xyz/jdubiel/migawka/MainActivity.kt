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
import xyz.jdubiel.migawka.databinding.ActivityMainBinding

import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        val channel = ManagedChannelBuilder.forAddress("192.168.5.158", 50051)
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




//            lifecycleScope.launch {
//                val response = stub.sayHello(HelloRequest.newBuilder().setName("Kuba").build())
//                println(response.message)
//            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}