@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.wallcycle.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import coil.compose.AsyncImage
import com.wallcycle.app.core.WallpaperChanger
import com.wallcycle.app.data.CollectionType
import com.wallcycle.app.data.Repository
import com.wallcycle.app.data.WallCollection
import com.wallcycle.app.ui.BottomBarSpace
import com.wallcycle.app.ui.GlassIconButton
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.NameDialog
import com.wallcycle.app.ui.Pill
import com.wallcycle.app.ui.ScreenHeader
import com.wallcycle.app.ui.glass
import kotlinx.coroutines.launch

@Composable
fun CollectionsScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val collections by Repository.collections.collectAsState()
    val settings by Repository.settings.collectAsState()
    val current by Repository.current.collectAsState()
    val activeId = settings.activeCollectionId ?: collections.firstOrNull()?.id

    var showAddChooser by remember { mutableStateOf(false) }
    var pendingFolder by remember { mutableStateOf<Uri?>(null) }
    var pendingImages by remember { mutableStateOf<List<Uri>?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            pendingFolder = uri
        }
    }
    val imagesPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        it, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            }
            pendingImages = uris
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomBarSpace),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ScreenHeader(
                title = "Coleções",
                eyebrow = "WallCycle",
                action = {
                    GlassIconButton(Icons.Rounded.Add, "Nova coleção") { showAddChooser = true }
                },
            )
        }

        if (current != null && collections.isNotEmpty()) {
            item {
                NowShowingCard(
                    current = current,
                    collectionName = collections.find { it.id == activeId }?.name,
                )
            }
        }

        if (collections.isEmpty()) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glass()
                        .clickable { showAddChooser = true }
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Rounded.CreateNewFolder, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Nenhuma coleção ainda", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Hint("Toque aqui para adicionar uma pasta de wallpapers ou escolher imagens.")
                }
            }
        }

        items(collections, key = { it.id }) { c ->
            CollectionCard(
                collection = c,
                active = c.id == activeId,
                onClick = { onOpen(c.id) },
            )
        }
    }

    if (showAddChooser) {
        AlertDialog(
            onDismissRequest = { showAddChooser = false },
            title = { Text("Nova coleção") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Pasta") },
                        supportingContent = { Text("Usa todas as imagens de uma pasta, inclusive as novas") },
                        leadingContent = { Icon(Icons.Rounded.CreateNewFolder, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddChooser = false
                                folderPicker.launch(null)
                            },
                    )
                    ListItem(
                        headlineContent = { Text("Imagens") },
                        supportingContent = { Text("Escolha imagens específicas") },
                        leadingContent = { Icon(Icons.Rounded.AddPhotoAlternate, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                showAddChooser = false
                                imagesPicker.launch(arrayOf("image/*"))
                            },
                    )
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAddChooser = false }) { Text("Cancelar") } },
        )
    }

    pendingFolder?.let { uri ->
        val defaultName = remember(uri) {
            DocumentFile.fromTreeUri(context, uri)?.name ?: "Pasta"
        }
        NameDialog(
            title = "Nome da coleção",
            initial = defaultName,
            onDismiss = { pendingFolder = null },
            onConfirm = { name ->
                Repository.addFolderCollection(name, uri)
                pendingFolder = null
            },
        )
    }

    pendingImages?.let { uris ->
        NameDialog(
            title = "Nome da coleção",
            initial = "Minha coleção",
            onDismiss = { pendingImages = null },
            onConfirm = { name ->
                Repository.addImagesCollection(name, uris)
                pendingImages = null
            },
        )
    }
}

/** Destaque com o wallpaper atual e um botão para pular para o próximo. */
@Composable
private fun NowShowingCard(current: Uri?, collectionName: String?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .glass(strong = true)
            .padding(12.dp),
    ) {
        AsyncImage(
            model = current,
            contentDescription = "Wallpaper atual",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(84.dp)
                .aspectRatio(9f / 16f)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "EM USO AGORA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                collectionName ?: "—",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = {
                if (busy) return@FilledTonalButton
                busy = true
                scope.launch {
                    val ok = WallpaperChanger.next(context)
                    busy = false
                    if (!ok) Toast.makeText(context, "Não foi possível alterar", Toast.LENGTH_SHORT).show()
                }
            }) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.SkipNext, null)
                Spacer(Modifier.width(8.dp))
                Text("Próximo")
            }
        }
    }
}

/** Cartão de vidro com mosaico: uma imagem grande e duas pequenas empilhadas. */
@Composable
private fun CollectionCard(collection: WallCollection, active: Boolean, onClick: () -> Unit) {
    val settings by Repository.settings.collectAsState()
    val images by produceState<List<Uri>?>(null, collection, settings.includeSubfolders) {
        value = Repository.images(collection)
    }
    val thumbShape = RoundedCornerShape(20.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(30.dp), strong = active)
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) {
            val list = images
            when {
                list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                list.isEmpty() -> Box(
                    Modifier
                        .fillMaxSize()
                        .clip(thumbShape)
                        .background(Color.White.copy(alpha = 0.06f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Hint("Nenhuma imagem encontrada")
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Thumb(list[0], thumbShape, Modifier.weight(2f).fillMaxHeight())
                        if (list.size > 1) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            ) {
                                Thumb(list[1], thumbShape, Modifier.weight(1f).fillMaxWidth())
                                if (list.size > 2) {
                                    Thumb(list[2], thumbShape, Modifier.weight(1f).fillMaxWidth())
                                }
                            }
                        }
                    }
                    if (list.size > 3) {
                        Pill(
                            "+${list.size - 3}",
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp),
                        )
                    }
                }
            }
            if (active) {
                Pill(
                    "Ativa",
                    accent = true,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 12.dp, bottom = 6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    collection.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val count = images?.size
                Hint(
                    buildString {
                        append(if (collection.type == CollectionType.FOLDER) "Pasta" else "Seleção")
                        if (count != null) append(" · $count imagens")
                    }
                )
            }
        }
    }
}

@Composable
private fun Thumb(uri: Uri, shape: RoundedCornerShape, modifier: Modifier) {
    Box(
        modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Image, null, tint = Color.White.copy(alpha = 0.25f))
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
