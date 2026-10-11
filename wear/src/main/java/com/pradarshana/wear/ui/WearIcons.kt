package com.pradarshana.wear.ui

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/** Icons that are only available in the large extended icon set. */
object WearIcons {
    val Pause: ImageVector by lazy {
        materialIcon(name = "Filled.Pause") {
            materialPath {
                moveTo(6f, 19f); horizontalLineToRelative(4f); verticalLineTo(5f); horizontalLineTo(6f); close()
                moveTo(14f, 5f); verticalLineToRelative(14f); horizontalLineToRelative(4f); verticalLineTo(5f); close()
            }
        }
    }

    val Remove: ImageVector by lazy {
        materialIcon(name = "Filled.Remove") {
            materialPath {
                moveTo(19f, 13f); horizontalLineTo(5f); verticalLineToRelative(-2f); horizontalLineToRelative(14f); verticalLineToRelative(2f); close()
            }
        }
    }
}
