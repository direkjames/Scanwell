package com.scanwell.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.remember
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.scanwell.app.R
import com.scanwell.app.pdf.ExportQuality
import com.scanwell.app.pdf.Protection
import com.scanwell.app.util.PasswordRules

/** Asks for a new name. */
@Composable
fun RenameDialog(currentTitle: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(currentTitle) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()) }, enabled = text.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Lets the user pick an existing folder, type a new one, or choose "No folder". */
@Composable
fun FolderDialog(
    folders: List<String>,
    currentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var selected by rememberSaveable { mutableStateOf(currentFolder) }
    var newFolder by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Move to folder") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ChoiceRow(
                    label = "No folder",
                    selected = newFolder.isBlank() && selected.isEmpty(),
                    onClick = {
                        selected = ""
                        newFolder = ""
                    },
                )
                for (folder in folders) {
                    ChoiceRow(
                        label = folder,
                        selected = newFolder.isBlank() && selected == folder,
                        onClick = {
                            selected = folder
                            newFolder = ""
                        },
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = newFolder,
                    onValueChange = { newFolder = it },
                    label = { Text("Or type a new folder") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(if (newFolder.isBlank()) selected else newFolder.trim()) }) {
                Text("Move")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** A yes/no question before something is deleted. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/**
 * A sheet that slides up from the bottom: choose a file size, optionally lock the PDF with a
 * password, then share it, save it, or share the pages as pictures.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    onDismiss: () -> Unit,
    onSharePdf: (ExportQuality, Protection?) -> Unit,
    onSavePdf: (ExportQuality, Protection?) -> Unit,
    onShareImages: () -> Unit,
    startLocked: Boolean = false,
) {
    var quality by rememberSaveable { mutableStateOf(ExportQuality.ORIGINAL) }
    // Passwords are deliberately not saved with rememberSaveable: they must never be written to disk.
    var lock by remember { mutableStateOf(startLocked) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    val problem = if (password.isEmpty()) null else PasswordRules.problem(password)
    val mismatch = confirm.isNotEmpty() && confirm != password
    val ready = !lock || (PasswordRules.problem(password) == null && confirm == password)
    fun protection(): Protection? = if (lock) Protection(password, null, false) else null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Share or save", style = MaterialTheme.typography.titleLarge)

            Text("PDF size", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            for (option in ExportQuality.entries) {
                ChoiceCard(
                    title = option.label,
                    subtitle = option.hint,
                    selected = quality == option,
                    onClick = { quality = option },
                )
            }

            SoftCard(Modifier.fillMaxWidth(), container = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(R.drawable.ic_lock, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Password protect", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "AES-256 lock on the PDF",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = lock, onCheckedChange = { lock = it })
                    }
                    if (lock) {
                        PasswordField(password, { password = it }, "Password to open", error = problem)
                        StrengthMeter(password)
                        PasswordField(
                            confirm,
                            { confirm = it },
                            "Confirm password",
                            error = if (mismatch) "Passwords do not match" else null,
                        )
                        Text(
                            "A forgotten password cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            PrimaryButton("Share as PDF", { onSharePdf(quality, protection()) }, icon = R.drawable.ic_share, enabled = ready)
            TonalButton("Save PDF to this phone", { onSavePdf(quality, protection()) }, icon = R.drawable.ic_download, enabled = ready)
            ActionRow(R.drawable.ic_image, if (lock) "Share as pictures (not password protected)" else "Share as pictures (JPEG)") {
                onShareImages()
            }
        }
    }
}

/**
 * Shows the text that was read from a scan.
 * [text] is null while the text is not available yet; [reading] says whether work is in progress.
 */
@Composable
fun TextDialog(
    title: String,
    text: String?,
    reading: Boolean,
    onRetry: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (text == null && reading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Reading the text...")
                    }
                } else if (text == null) {
                    Text(
                        "The text could not be read yet. The text reader downloads itself the " +
                            "first time, so connect to the internet and try again.",
                    )
                } else if (text.isBlank()) {
                    Text("No text was found.")
                } else {
                    // SelectionContainer lets the user long-press to select part of the text.
                    SelectionContainer { Text(text) }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.End) {
                if (text == null && !reading) {
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
                if (!text.isNullOrBlank()) {
                    TextButton(onClick = { onShare(text) }) { Text("Share") }
                    TextButton(onClick = { onCopy(text) }) { Text("Copy") }
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // onClick = null: the whole row handles the tap, not just the small circle.
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}

@Composable
private fun ActionRow(iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label)
    }
}
