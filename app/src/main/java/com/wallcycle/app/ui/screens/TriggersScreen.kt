@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.wallcycle.app.ui.screens

import android.app.StatusBarManager
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon as AndroidIcon
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.wallcycle.app.R
import com.wallcycle.app.core.WallpaperChanger
import com.wallcycle.app.data.ChangeOrder
import com.wallcycle.app.data.Repository
import com.wallcycle.app.service.GestureWallpaperService
import com.wallcycle.app.service.NextWallpaperTileService
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.ScreenHeader
import com.wallcycle.app.ui.SectionCard
import com.wallcycle.app.ui.SwitchRow
import com.wallcycle.app.work.Scheduler
import kotlinx.coroutines.launch

private val INTERVALS = listOf(
    0 to "Desligado", 15 to "15 min", 30 to "30 min", 60 to "1 h",
    180 to "3 h", 360 to "6 h", 720 to "12 h", 1440 to "24 h",
)

@Composable
fun TriggersScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Repository.settings.collectAsState()
    var liveActive by remember { mutableStateOf(WallpaperChanger.isLiveActive(context)) }

    // Reverifica ao voltar do seletor de papel de parede.
    LifecycleResumeEffect(Unit) {
        liveActive = WallpaperChanger.isLiveActive(context)
        onPauseOrDispose { }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Gatilhos")

        SectionCard("Mudança automática", Icons.Rounded.Timer) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                INTERVALS.forEach { (minutes, label) ->
                    FilterChip(
                        selected = settings.intervalMinutes == minutes,
                        onClick = {
                            Repository.update { it.copy(intervalMinutes = minutes) }
                            Scheduler.schedule(context, minutes)
                        },
                        label = { Text(label) },
                    )
                }
            }
            Hint("O Android permite no mínimo 15 minutos, e o horário exato pode variar um pouco para economizar bateria.")
        }

        SectionCard("Ordem", Icons.Rounded.Shuffle) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(ChangeOrder.RANDOM to "Aleatória", ChangeOrder.SEQUENTIAL to "Sequencial")
                options.forEachIndexed { i, (order, label) ->
                    SegmentedButton(
                        selected = settings.order == order,
                        onClick = { Repository.update { it.copy(order = order) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            Hint("No modo aleatório, as imagens mostradas recentemente são evitadas.")
        }

        SectionCard("Gestos", Icons.Rounded.TouchApp) {
            Text(
                if (liveActive) "✓ Papel de parede com gestos ativo"
                else "Para usar gestos, ative o papel de parede animado do WallCycle (ele mostra as imagens da sua coleção).",
                style = MaterialTheme.typography.bodyMedium,
                color = if (liveActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!liveActive) {
                Button(
                    onClick = {
                        val component = ComponentName(context, GestureWallpaperService::class.java)
                        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component)
                        runCatching { context.startActivity(intent) }.onFailure {
                            runCatching {
                                context.startActivity(Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Ativar papel de parede com gestos") }
            }
            SwitchRow(
                title = "Toque duplo na tela inicial",
                subtitle = "Toque duas vezes num espaço vazio para trocar",
                checked = settings.doubleTap,
                enabled = liveActive,
            ) { v -> Repository.update { it.copy(doubleTap = v) } }
            SwitchRow(
                title = "Trocar ao desligar a tela",
                subtitle = "Um wallpaper novo a cada desbloqueio",
                checked = settings.changeOnScreenOff,
                enabled = liveActive,
            ) { v -> Repository.update { it.copy(changeOnScreenOff = v) } }
        }

        SectionCard("Atalho rápido", Icons.Rounded.Bolt) {
            Hint("Adicione o bloco “Próximo wallpaper” nas Configurações rápidas (barra de notificações) para trocar com um toque, de qualquer lugar.")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                FilledTonalButton(
                    onClick = {
                        val sbm = context.getSystemService(StatusBarManager::class.java)
                        sbm.requestAddTileService(
                            ComponentName(context, NextWallpaperTileService::class.java),
                            context.getString(R.string.tile_label),
                            AndroidIcon.createWithResource(context, R.drawable.ic_tile),
                            context.mainExecutor,
                        ) { }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Adicionar bloco") }
            }
        }

        Spacer(Modifier.height(4.dp))
        Button(
            onClick = {
                scope.launch {
                    val ok = WallpaperChanger.next(context)
                    Toast.makeText(
                        context,
                        if (ok) "Papel de parede alterado" else "Crie ou escolha uma coleção primeiro",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Icon(Icons.Rounded.SkipNext, null)
            Text("  Mudar agora")
        }
    }
}
