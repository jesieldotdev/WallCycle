package com.wallcycle.app.ui.screens

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wallcycle.app.core.WallpaperChanger
import com.wallcycle.app.data.Repository
import com.wallcycle.app.ui.BottomBarSpace
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.ScreenHeader
import com.wallcycle.app.ui.SectionCard
import com.wallcycle.app.ui.SwitchRow
import com.wallcycle.app.ui.glass
import kotlinx.coroutines.launch

@Composable
fun EffectsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by Repository.settings.collectAsState()
    val current by Repository.current.collectAsState()

    // Valores locais enquanto arrasta; salva só ao soltar o slider.
    var blur by remember { mutableFloatStateOf(settings.blur.toFloat()) }
    var dim by remember { mutableFloatStateOf(settings.dim.toFloat()) }
    LaunchedEffect(settings.blur, settings.dim) {
        blur = settings.blur.toFloat(); dim = settings.dim.toFloat()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenHeader("Efeitos", eyebrow = "Visual do wallpaper")

        // Pré-visualização do wallpaper atual com os efeitos.
        Box(
            Modifier
                .fillMaxWidth(0.55f)
                .aspectRatio(9f / 19.5f)
                .align(Alignment.CenterHorizontally)
                .glass(RoundedCornerShape(28.dp), strong = true),
            contentAlignment = Alignment.Center,
        ) {
            if (current != null) {
                AsyncImage(
                    model = current,
                    contentDescription = "Pré-visualização",
                    contentScale = ContentScale.Crop,
                    colorFilter = if (settings.grayscale)
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blur > 0)
                                Modifier.blur((blur * 0.6f).dp) else Modifier
                        ),
                )
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim / 100f)))
            } else {
                Hint("Nenhum wallpaper aplicado ainda")
            }
        }

        SectionCard("Ajustes", Icons.Rounded.AutoAwesome) {
            Text("Desfoque: ${blur.toInt()}", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = blur,
                onValueChange = { blur = it },
                valueRange = 0f..25f,
                onValueChangeFinished = { Repository.update { it.copy(blur = blur.toInt()) } },
            )
            Text("Escurecer: ${dim.toInt()}%", style = MaterialTheme.typography.bodyLarge)
            Slider(
                value = dim,
                onValueChange = { dim = it },
                valueRange = 0f..80f,
                onValueChangeFinished = { Repository.update { it.copy(dim = dim.toInt()) } },
            )
            SwitchRow(
                title = "Preto e branco",
                subtitle = null,
                checked = settings.grayscale,
            ) { v -> Repository.update { it.copy(grayscale = v) } }
            Hint("Os efeitos valem para as próximas trocas. No modo com gestos, são aplicados na hora.")
        }

        Button(
            onClick = {
                scope.launch {
                    val ok = WallpaperChanger.reapply(context)
                    Toast.makeText(
                        context,
                        if (ok) "Efeitos aplicados" else "Não foi possível aplicar",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Aplicar no wallpaper atual") }
    }
}
