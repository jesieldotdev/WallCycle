package com.wallcycle.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.wallcycle.app.data.Repository
import com.wallcycle.app.ui.screens.CollectionDetailScreen
import com.wallcycle.app.ui.screens.CollectionsScreen
import com.wallcycle.app.ui.screens.EffectsScreen
import com.wallcycle.app.ui.screens.SettingsScreen
import com.wallcycle.app.ui.screens.TriggersScreen
import com.wallcycle.app.ui.theme.WallCycleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by Repository.settings.collectAsState()
            WallCycleTheme(dynamicColor = settings.dynamicColor) {
                AppRoot()
            }
        }
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    COLLECTIONS("Coleções", Icons.Rounded.PhotoLibrary),
    TRIGGERS("Gatilhos", Icons.Rounded.Autorenew),
    EFFECTS("Efeitos", Icons.Rounded.AutoAwesome),
    SETTINGS("Configurações", Icons.Rounded.Settings),
}

@Composable
fun AppRoot() {
    var tab by rememberSaveable { mutableStateOf(Tab.COLLECTIONS) }
    var openCollection by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (openCollection == null) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val id = openCollection
            when {
                id != null -> CollectionDetailScreen(id, onBack = { openCollection = null })
                tab == Tab.COLLECTIONS -> CollectionsScreen(onOpen = { openCollection = it })
                tab == Tab.TRIGGERS -> TriggersScreen()
                tab == Tab.EFFECTS -> EffectsScreen()
                tab == Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}
