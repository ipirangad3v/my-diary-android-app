package digital.tonima.mydiary.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import digital.tonima.mydiary.BuildConfig.ADMOB_BANNER_AD_UNIT_LOCKED_DIARY
import digital.tonima.mydiary.R.string.locked_diary
import digital.tonima.mydiary.R.string.unlock_diary
import digital.tonima.mydiary.ui.components.AdBannerView

@Composable
fun LockedScreen(onUnlockRequest: () -> Unit) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { onUnlockRequest() }) {
                Icon(Icons.Filled.Lock, contentDescription = stringResource(id = unlock_diary))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            AdBannerView(adId = ADMOB_BANNER_AD_UNIT_LOCKED_DIARY)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(id = locked_diary))
            }
        }
    }
}
