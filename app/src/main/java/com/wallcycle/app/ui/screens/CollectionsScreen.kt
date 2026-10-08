@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.wallcycle.app.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import com.wallcycle.app.data.CollectionType
import com.wallcycle.app.data.Repository
import com.wallcycle.app.data.WallCollection
import com.wallcycle.app.ui.Hint
import com.wallcycle.app.ui.NameDialog
import com.wallcycle.app.ui.ScreenHeader

@Composable
fun CollectionsScreen(onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val collections by Repository.collections.collectAsState()
    val settings by Repository.settings.collectAsState()

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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { ScreenHeader("Coleções") }
            if (collections.isEmpty()) {
                item {
                    Column(Modifier.padding(12.dp)) {
                        Text("Nenhuma coleção ainda", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Hint("Toque em + para adicionar uma pasta de wallpapers ou escolher imagens.")
                    }
                }
            }
            items(collections, key = { it.id }) { c ->
                CollectionCard(
                    collection = c,
                    active = c.id == (settings.activeCollectionId ?: collections.firstOrNull()?.id),
                    onClick = { onOpen(c.id) },
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddChooser = true },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(72.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Adicionar", modifier = Modifier.size(32.dp))
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

@Composable
private fun CollectionCard(collection: WallCollection, active: Boolean, onClick: () -> Unit) {
    val settings by Repository.settings.collectAsState()
    val images by produceState<List<Uri>?>(null, collection, settings.includeSubfolders) {
        value = Repository.images(collection)
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.surfaceContainerHighest
            else MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                Icon(
                    if (collection.type == CollectionType.FOLDER) Icons.Rounded.Folder
                    else Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(20.dp))
                Text(
                    collection.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (active) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = "Coleção ativa",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            val list = images
            when {
                list == null -> {
                    Spacer(Modifier.height(20.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                list.isNotEmpty() -> {
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        list.take(4).forEachIndexed { i, uri ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center,
                            ) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                if (i == 3 && list.size > 4) {
                                    Box(
                                        Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.55f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            "+${list.size - 4}",
                                            color = Color.White,
                                            style = MaterialTheme.typography.headlineSmall,
                                        )
                                    }
                                }
                            }
                        }
                        repeat(4 - minOf(4, list.size)) { Spacer(Modifier.weight(1f)) }
                    }
                }
                else -> {
                    Spacer(Modifier.height(12.dp))
                    Hint("Nenhuma imagem encontrada")
                }
            }
        }
    }
}
