package digital.tonima.mydiary.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import digital.tonima.mydiary.R
import digital.tonima.mydiary.R.string.app_name

@Composable
fun DrawerContent(
    isProUser: Boolean,
    onDeleteAll: () -> Unit,
    onResetApp: () -> Unit,
    onUpgradeToPro: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "N/A"
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            "N/A"
        }
    }
    ModalDrawerSheet {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column {
                Column(modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)) {
                    Text(stringResource(app_name), modifier = Modifier.padding(16.dp))
                }
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                if (!isProUser) {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text(stringResource(R.string.remove_ads)) },
                        selected = false,
                        onClick = {
                            onUpgradeToPro()
                            onCloseDrawer()
                        },
                    )
                }

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    label = { Text(stringResource(R.string.delete_all_notes)) },
                    selected = false,
                    onClick = {
                        onDeleteAll()
                        onCloseDrawer()
                    },
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Lock, contentDescription = null) },
                    label = { Text(stringResource(R.string.reset_master_password)) },
                    selected = false,
                    onClick = {
                        onResetApp()
                        onCloseDrawer()
                    },
                )
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    label = { Text(stringResource(R.string.our_other_apps)) },
                    selected = false,
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/dev?id=6594602823307179845".toUri(),
                        )
                        context.startActivity(browserIntent)
                        onCloseDrawer()
                    },
                )
            }
            Text(
                text = stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}
