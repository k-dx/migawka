package xyz.jdubiel.migawka.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch

class SecondScreenViewModel : ViewModel() {
    private val job = SupervisorJob()
    private val dispatcher = Dispatchers.Default
    private val customScope = CoroutineScope(job + dispatcher)
    private val randomFlowCold: Flow<Int> = flow {
        var n = 0;
        while (n < 10) {
            val rnds = (0..100).random()
            emit(rnds)
            delay(1000L)
            n++;
        }
    }
    private val shared = randomFlowCold.shareIn(viewModelScope, started = SharingStarted.Eagerly, replay = 0)

    init {

        collectFlow(viewModelScope, "A")
        collectFlow(customScope, "B")
    }

    private fun collectFlow(scope: CoroutineScope, tag: String) {
        scope.launch {
            shared.collect {
                Log.d("FLOW", "$tag: received $it")
            }
        }
    }
}