package com.wallcycle.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/** Espaço reservado no fim das telas para a barra de navegação flutuante. */
val BottomBarSpace = 112.dp

/** Superfície de vidro: preenchimento translúcido + borda clara em degradê. */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(28.dp),
    strong: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = if (strong) 0.22f else 0.12f),
                Color.White.copy(alpha = if (strong) 0.12f else 0.05f),
            )
        )
    )
    .border(
        1.dp,
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.06f))
        ),
        shape,
    )

@Composable
fun ScreenHeader(
    title: String,
    eyebrow: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    eyebrow.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(title, style = MaterialTheme.typography.headlineLarge)
        }
        action?.invoke()
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String?,
    size: Int = 48,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(size.dp)
            .glass(CircleShape, strong = true)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = Color.White)
    }
}

@Composable
fun Pill(text: String, accent: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (accent) MaterialTheme.colorScheme.onPrimary else Color.White,
        modifier = modifier
            .clip(CircleShape)
            .background(
                if (accent) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f)
            )
            .padding(horizontal = 12.dp, vertical = 5.dp),
    )
}

@Composable
fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glass()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
        content()
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = enabled) { onChange(!checked) }
            .padding(vertical = 4.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
fun NameDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Nome") },
            )
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim()) }) { Text("Salvar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
