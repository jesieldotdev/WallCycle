package com.wallcycle.app.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Fonte única de dados do app. Guarda coleções e configurações em SharedPreferences
 * e expõe tudo como StateFlow para a UI, o Worker e o papel de parede animado.
 */
object Repository {

    const val KEY_CURRENT = "current_uri"
    private const val PREFS = "wallcycle"
    private const val KEY_COLLECTIONS = "collections"
    private const val KEY_SEQ_INDEX = "seq_index"
    private const val KEY_HISTORY = "history"

    /** Chaves cuja alteração exige redesenhar o papel de parede animado. */
    val EFFECT_KEYS = setOf("blur", "dim", "grayscale")

    private lateinit var appContext: Context
    lateinit var prefs: SharedPreferences
        private set

    private val _collections = MutableStateFlow<List<WallCollection>>(emptyList())
    val collections: StateFlow<List<WallCollection>> = _collections.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _current = MutableStateFlow<Uri?>(null)
    val current: StateFlow<Uri?> = _current.asStateFlow()

    private val imageCache = ConcurrentHashMap<String, List<Uri>>()

    fun init(context: Context) {
        if (::prefs.isInitialized) return
        appContext = context.applicationContext
        prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _collections.value = loadCollections()
        _settings.value = loadSettings()
        _current.value = prefs.getString(KEY_CURRENT, null)?.let(Uri::parse)
    }

    // ---------------------------------------------------------------- Coleções

    fun activeCollection(): WallCollection? {
        val list = _collections.value
        return list.find { it.id == _settings.value.activeCollectionId } ?: list.firstOrNull()
    }

    fun addFolderCollection(name: String, treeUri: Uri): WallCollection {
        val c = WallCollection(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Pasta" },
            type = CollectionType.FOLDER,
            folderUri = treeUri.toString(),
        )
        saveCollections(_collections.value + c)
        if (_settings.value.activeCollectionId == null) setActive(c.id)
        return c
    }

    fun addImagesCollection(name: String, uris: List<Uri>): WallCollection {
        val c = WallCollection(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Coleção" },
            type = CollectionType.IMAGES,
            imageUris = uris.map { it.toString() }.distinct(),
        )
        saveCollections(_collections.value + c)
        if (_settings.value.activeCollectionId == null) setActive(c.id)
        return c
    }

    fun addImages(collectionId: String, uris: List<Uri>) = editCollection(collectionId) {
        it.copy(imageUris = (it.imageUris + uris.map(Uri::toString)).distinct())
    }

    fun removeImage(collectionId: String, uri: Uri) = editCollection(collectionId) {
        it.copy(imageUris = it.imageUris - uri.toString())
    }

    fun rename(collectionId: String, name: String) = editCollection(collectionId) {
        it.copy(name = name.ifBlank { it.name })
    }

    fun delete(collectionId: String) {
        val removed = _collections.value.find { it.id == collectionId } ?: return
        saveCollections(_collections.value.filterNot { it.id == collectionId })
        imageCache.keys.removeAll { it.startsWith(collectionId) }
        // Libera a permissão persistente da pasta.
        removed.folderUri?.let {
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(
                    Uri.parse(it), Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        if (_settings.value.activeCollectionId == collectionId) {
            update { s -> s.copy(activeCollectionId = _collections.value.firstOrNull()?.id) }
        }
    }

    fun setActive(collectionId: String) {
        if (_settings.value.activeCollectionId != collectionId) {
            prefs.edit().putInt(KEY_SEQ_INDEX, -1).remove(KEY_HISTORY).apply()
        }
        update { it.copy(activeCollectionId = collectionId) }
    }

    private fun editCollection(id: String, transform: (WallCollection) -> WallCollection) {
        saveCollections(_collections.value.map { if (it.id == id) transform(it) else it })
        imageCache.keys.removeAll { it.startsWith(id) }
    }

    /** Lista as imagens da coleção (com cache em memória). */
    suspend fun images(c: WallCollection, refresh: Boolean = false): List<Uri> =
        withContext(Dispatchers.IO) {
            val recursive = _settings.value.includeSubfolders
            val key = "${c.id}|$recursive|${c.imageUris.size}"
            if (!refresh) imageCache[key]?.let { return@withContext it }
            val list = when (c.type) {
                CollectionType.IMAGES -> c.imageUris.map(Uri::parse)
                CollectionType.FOLDER -> c.folderUri?.let {
                    runCatching { listImagesInTree(Uri.parse(it), recursive) }.getOrDefault(emptyList())
                } ?: emptyList()
            }
            imageCache.keys.removeAll { it.startsWith(c.id) }
            imageCache[key] = list
            list
        }

    fun clearCache() = imageCache.clear()

    /** Percorre a pasta escolhida via SAF usando consultas diretas (bem mais rápido que DocumentFile). */
    private fun listImagesInTree(treeUri: Uri, recursive: Boolean): List<Uri> {
        val resolver = appContext.contentResolver
        val found = mutableListOf<Pair<String, Uri>>()
        val stack = ArrayDeque<String>()
        stack.add(DocumentsContract.getTreeDocumentId(treeUri))
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        while (stack.isNotEmpty()) {
            val parentId = stack.removeLast()
            val children = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
            resolver.query(children, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getString(0) ?: continue
                    val mime = cursor.getString(1) ?: ""
                    val name = cursor.getString(2) ?: id
                    when {
                        mime == DocumentsContract.Document.MIME_TYPE_DIR -> if (recursive) stack.add(id)
                        mime.startsWith("image/") ->
                            found += name.lowercase() to DocumentsContract.buildDocumentUriUsingTree(treeUri, id)
                    }
                }
            }
        }
        return found.sortedBy { it.first }.map { it.second }
    }

    // ------------------------------------------------------------ Configurações

    fun update(transform: (AppSettings) -> AppSettings) {
        val s = transform(_settings.value)
        _settings.value = s
        prefs.edit()
            .putString("active", s.activeCollectionId)
            .putString("order", s.order.name)
            .putString("target", s.target.name)
            .putInt("interval", s.intervalMinutes)
            .putBoolean("double_tap", s.doubleTap)
            .putBoolean("screen_off", s.changeOnScreenOff)
            .putBoolean("subfolders", s.includeSubfolders)
            .putInt("blur", s.blur)
            .putInt("dim", s.dim)
            .putBoolean("grayscale", s.grayscale)
            .putBoolean("dynamic_color", s.dynamicColor)
            .apply()
    }

    private fun loadSettings() = AppSettings(
        activeCollectionId = prefs.getString("active", null),
        order = enumOr(prefs.getString("order", null), ChangeOrder.RANDOM),
        target = enumOr(prefs.getString("target", null), WallTarget.BOTH),
        intervalMinutes = prefs.getInt("interval", 0),
        doubleTap = prefs.getBoolean("double_tap", true),
        changeOnScreenOff = prefs.getBoolean("screen_off", false),
        includeSubfolders = prefs.getBoolean("subfolders", false),
        blur = prefs.getInt("blur", 0),
        dim = prefs.getInt("dim", 0),
        grayscale = prefs.getBoolean("grayscale", false),
        dynamicColor = prefs.getBoolean("dynamic_color", false),
    )

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        name?.let { n -> enumValues<T>().find { it.name == n } } ?: default

    // ------------------------------------------------------ Estado da rotação

    fun setCurrent(uri: Uri) {
        _current.value = uri
        val history = history().toMutableList().apply {
            remove(uri.toString()); add(uri.toString())
            while (size > 50) removeAt(0)
        }
        prefs.edit()
            .putString(KEY_CURRENT, uri.toString())
            .putString(KEY_HISTORY, JSONArray(history).toString())
            .apply()
    }

    fun history(): List<String> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_HISTORY, "[]"))
        List(arr.length()) { arr.getString(it) }
    }.getOrDefault(emptyList())

    var sequenceIndex: Int
        get() = prefs.getInt(KEY_SEQ_INDEX, -1)
        set(value) = prefs.edit().putInt(KEY_SEQ_INDEX, value).apply()

    // -------------------------------------------------------- Persistência

    private fun saveCollections(list: List<WallCollection>) {
        _collections.value = list
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("type", c.type.name)
                put("folder", c.folderUri ?: "")
                put("images", JSONArray(c.imageUris))
            })
        }
        prefs.edit().putString(KEY_COLLECTIONS, arr.toString()).apply()
    }

    private fun loadCollections(): List<WallCollection> = runCatching {
        val arr = JSONArray(prefs.getString(KEY_COLLECTIONS, "[]"))
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            val imgs = o.optJSONArray("images") ?: JSONArray()
            WallCollection(
                id = o.getString("id"),
                name = o.getString("name"),
                type = enumOr(o.optString("type"), CollectionType.FOLDER),
                folderUri = o.optString("folder").ifBlank { null },
                imageUris = List(imgs.length()) { imgs.getString(it) },
            )
        }
    }.getOrDefault(emptyList())
}
