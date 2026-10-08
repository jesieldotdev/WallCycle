@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.wallcycle.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.wallcycle.app.core.WallpaperChanger
import com.wallcycle.app.data.CollectionType
import com.wallcycle.app.data.Repository
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.NameDialog
import com.wallcycle.app.ui.glass
import kotlinx.coroutines.launch

@Composable
fun CollectionDetailScreen(collectionId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val collections by Repository.collections.collectAsState()
    val settings by Repository.settings.collectAsState()
    val collection = collections.find { it.id == collectionId }

    BackHandler(onBack = onBack)
    if (collection == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    var refreshKey by remember { mutableIntStateOf(0) }
    val images by produceState<List<Uri>?>(null, collection, refreshKey, settings.includeSubfolders) {
        value = Repository.images(collection, refresh = refreshKey > 0)
    }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selection by remember(collectionId) { mutableStateOf(setOf<Uri>()) }
    var confirmRemove by remember { mutableStateOf(false) }
    val selecting = selection.isNotEmpty()

    // Com itens selecionados, "voltar" só limpa a seleção.
    BackHandler(enabled = selecting) { selection = emptySet() }

    fun removeNow(uris: Set<Uri>) {
        Repository.removeImages(collection.id, uris)
        selection = emptySet()
        Toast.makeText(
            context,
            if (uris.size == 1) "Wallpaper removido" else "${uris.size} wallpapers removidos",
            Toast.LENGTH_SHORT,
        ).show()
    }

    val isActive = collection.id == (settings.activeCollectionId ?: collections.firstOrNull()?.id)

    val addPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        uris.forEach {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        if (uris.isNotEmpty()) Repository.addImages(collection.id, uris)
    }

    fun runChange(block: suspend () -> Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            val ok = block()
            busy = false
            Toast.makeText(
                context,
                if (ok) "Papel de parede alterado" else "Não foi possível alterar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            title = {
                Text(if (selecting) "${selection.size} selecionada(s)" else collection.name, maxLines = 1)
            },
            navigationIcon = {
                if (selecting) {
                    IconButton(onClick = { selection = emptySet() }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancelar seleção")
                    }
                } else {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Voltar")
                    }
                }
            },
            actions = {
                if (selecting) {
                    IconButton(onClick = { images?.let { selection = it.toSet() } }) {
                        Icon(Icons.Rounded.SelectAll, contentDescription = "Selecionar tudo")
                    }
                    IconButton(onClick = { confirmRemove = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Remover selecionadas")
                    }
                } else {
                if (collection.type == CollectionType.FOLDER) {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Atualizar pasta")
                    }
                } else {
                    IconButton(onClick = { addPicker.launch(arrayOf("image/*")) }) {
                        Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = "Adicionar imagens")
                    }
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Mais")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (collection.type == CollectionType.FOLDER && collection.excluded.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Restaurar removidas (${collection.excluded.size})") },
                                onClick = { menuOpen = false; Repository.restoreExcluded(collection.id) },
                            )
                        }
                        DropdownMenuItem(text = { Text("Renomear") }, onClick = {
                            menuOpen = false; renaming = true
                        })
                        DropdownMenuItem(text = { Text("Excluir coleção") }, onClick = {
                            menuOpen = false; confirmDelete = true
                        })
                    }
                }
                }
            },
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            if (isActive) {
                FilledTonalButton(onClick = {}, enabled = false, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Coleção ativa")
                }
            } else {
                Button(onClick = { Repository.setActive(collection.id) }, modifier = Modifier.weight(1f)) {
                    Text("Usar esta coleção")
                }
            }
            FilledTonalButton(
                onClick = {
                    runChange {
                        Repository.setActive(collection.id)
                        WallpaperChanger.next(context)
                    }
                },
                modifier = Modifier.weight(1f),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Rounded.SkipNext, null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Mudar agora")
            }
        }

        val list = images
        when {
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Hint("Nenhuma imagem nesta coleção")
            }
            else -> {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    Hint("${list.size} imagens · toque para aplicar, segure para selecionar e remover")
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(list, key = { it.toString() }) { uri ->
                        val isSel = uri in selection
                        Box(
                            Modifier
                                .aspectRatio(9f / 16f)
                                .glass(RoundedCornerShape(18.dp))
                                .combinedClickable(
                                    onClick = {
                                        if (selecting) selection = if (isSel) selection - uri else selection + uri
                                        else selected = uri
                                    },
                                    onLongClick = {
                                        selection = if (isSel) selection - uri else selection + uri
                                    },
                                ),
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (isSel) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp)),
                                )
                                Icon(
                                    Icons.Rounded.CheckCircle,
                                    contentDescription = "Selecionada",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(26.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { uri ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Definir papel de parede?") },
            text = {
                AsyncImage(
                    model = uri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(20.dp)),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    selected = null
                    runChange { WallpaperChanger.apply(context, uri) }
                }) { Text("Aplicar") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        selected = null
                        removeNow(setOf(uri))
                    }) { Text("Remover") }
                    TextButton(onClick = { selected = null }) { Text("Cancelar") }
                }
            },
        )
    }

    if (confirmRemove) {
        val n = selection.size
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(if (n == 1) "Remover 1 wallpaper?" else "Remover $n wallpapers?") },
            text = {
                Text(
                    if (collection.type == CollectionType.FOLDER)
                        "Eles deixam de aparecer nesta coleção. Os arquivos continuam na pasta e podem ser restaurados pelo menu ⋮."
                    else
                        "Eles saem desta coleção. Os arquivos continuam no aparelho."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    removeNow(selection)
                }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancelar") } },
        )
    }

    if (renaming) {
        NameDialog(
            title = "Renomear coleção",
            initial = collection.name,
            onDismiss = { renaming = false },
            onConfirm = { Repository.rename(collection.id, it); renaming = false },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir “${collection.name}”?") },
            text = { Text("Os arquivos no aparelho não serão apagados, só a coleção.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    Repository.delete(collection.id)
                    onBack()
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}
