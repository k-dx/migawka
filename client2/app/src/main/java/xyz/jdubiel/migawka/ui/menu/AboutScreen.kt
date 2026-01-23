package xyz.jdubiel.migawka.ui.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import xyz.jdubiel.migawka.BuildConfig
import xyz.jdubiel.migawka.R

@Composable
fun AboutScreen(modifier: Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            stringResource(R.string.about),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(stringResource(R.string.about_content))
        Text(stringResource(R.string.version, BuildConfig.VERSION_NAME))

        HorizontalDivider(modifier = Modifier.padding(16.dp))

        Text(
            stringResource(R.string.libraries_used),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val libraries by produceLibraries(R.raw.aboutlibraries)
        LibrariesContainer(libraries, Modifier.fillMaxSize())
    }
}