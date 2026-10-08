package com.wallcycle.app.data

enum class CollectionType { FOLDER, IMAGES }

enum class ChangeOrder { RANDOM, SEQUENTIAL }

enum class WallTarget { HOME, LOCK, BOTH }

data class WallCollection(
    val id: String,
    val name: String,
    val type: CollectionType,
    /** URI da pasta (SAF) quando type == FOLDER. */
    val folderUri: String? = null,
    /** Imagens escolhidas manualmente quando type == IMAGES. */
    val imageUris: List<String> = emptyList(),
)

data class AppSettings(
    val activeCollectionId: String? = null,
    val order: ChangeOrder = ChangeOrder.RANDOM,
    val target: WallTarget = WallTarget.BOTH,
    /** 0 = desligado. */
    val intervalMinutes: Int = 0,
    val doubleTap: Boolean = true,
    val changeOnScreenOff: Boolean = false,
    val includeSubfolders: Boolean = false,
    /** 0..25 */
    val blur: Int = 0,
    /** 0..80 (%) */
    val dim: Int = 0,
    val grayscale: Boolean = false,
    val dynamicColor: Boolean = false,
)
