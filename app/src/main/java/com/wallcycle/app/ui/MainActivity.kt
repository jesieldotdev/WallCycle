package com.wallcycle.app.ui

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
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
        // Ícones claros nas barras do sistema, já que o fundo é sempre o wallpaper escurecido.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
        )
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

    // Véu escuro sobre o papel de parede para manter o texto legível.
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Black.copy(alpha = 0.30f),
                        Color.Black.copy(alpha = 0.50f),
                        Color.Black.copy(alpha = 0.72f),
                    )
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
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

        if (openCollection == null) {
            GlassNavBar(
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            )
        }
    }
}

/** Barra flutuante em formato de pílula; a aba ativa se expande mostrando o nome. */
@Composable
private fun GlassNavBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .glass(CircleShape, strong = true)
            .background(Color.Black.copy(alpha = 0.25f))
            .padding(6.dp),
    ) {
        Tab.entries.forEach { t ->
            val isSelected = t == selected
            val bg by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "tabBg",
            )
            val fg = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.85f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(bg)
                    .clickable { onSelect(t) }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .animateContentSize(),
            ) {
                Icon(t.icon, contentDescription = t.label, tint = fg)
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                ) {
                    Row {
                        Spacer(Modifier.width(8.dp))
                        Text(t.label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
    }
}
