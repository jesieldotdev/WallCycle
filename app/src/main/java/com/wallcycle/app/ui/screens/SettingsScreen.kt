@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.wallcycle.app.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wallcycle.app.data.Repository
import com.wallcycle.app.data.WallTarget
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.ScreenHeader
import com.wallcycle.app.ui.SectionCard
import com.wallcycle.app.ui.SwitchRow

@Composable
fun SettingsScreen() {
    val settings by Repository.settings.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Configurações")

        SectionCard("Aplicar em", Icons.Rounded.Smartphone) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val options = listOf(
                    WallTarget.HOME to "Início",
                    WallTarget.LOCK to "Bloqueio",
                    WallTarget.BOTH to "Ambas",
                )
                options.forEachIndexed { i, (target, label) ->
                    SegmentedButton(
                        selected = settings.target == target,
                        onClick = { Repository.update { it.copy(target = target) } },
                        shape = SegmentedButtonDefaults.itemShape(i, options.size),
                    ) { Text(label) }
                }
            }
            Hint("Vale para o modo normal. No modo com gestos, o Android usa o mesmo papel de parede nas duas telas.")
        }

        SectionCard("Pastas", Icons.Rounded.Folder) {
            SwitchRow(
                title = "Incluir subpastas",
                subtitle = "Busca imagens também dentro das subpastas",
                checked = settings.includeSubfolders,
            ) { v ->
                Repository.clearCache()
                Repository.update { it.copy(includeSubfolders = v) }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SectionCard("Aparência", Icons.Rounded.Palette) {
                SwitchRow(
                    title = "Cores dinâmicas",
                    subtitle = "Usa as cores do seu papel de parede (Material You)",
                    checked = settings.dynamicColor,
                ) { v -> Repository.update { it.copy(dynamicColor = v) } }
            }
        }

        SectionCard("Sobre", Icons.Rounded.Info) {
            Hint("WallCycle 1.0 · Troca automática de papel de parede a partir das suas pastas.")
        }
    }
}
