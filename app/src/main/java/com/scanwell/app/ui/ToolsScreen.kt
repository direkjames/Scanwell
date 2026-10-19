@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanwell.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scanwell.app.MainViewModel
import com.scanwell.app.PickedPdf
import com.scanwell.app.R
import com.scanwell.app.ToolMode
import com.scanwell.app.ToolResult
import com.scanwell.app.ToolUiState
import com.scanwell.app.pdf.CompressionLevel
import com.scanwell.app.pdf.Protection
import com.scanwell.app.ui.theme.ScanwellTheme
import com.scanwell.app.util.PasswordRules
import com.scanwell.app.util.Sizes

/** Compress, protect or unlock a PDF the person picks from their phone. All work happens on the phone. */
@Composable
fun ToolsScreen(
    viewModel: MainViewModel,
    snackbar: SnackbarHostState,
    onPickPdf: () -> Unit,
    onSaveResult: (String) -> Unit,
) {
    val state by viewModel.tool.collectAsState()
    val mode = state.mode

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(mode.title) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.back() }) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val picked = state.picked
            val result = state.result
            AnimatedContent(
                targetState = Step.of(state),
                transitionSpec = { (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut() },
                label = "tool-step",
            ) { step ->
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    when (step) {
                        Step.CHOOSE -> ChooseStep(state, onPickPdf)
                        Step.PASSWORD -> if (picked != null) OpenPasswordStep(viewModel, state, picked)
                        Step.OPTIONS -> if (picked != null) OptionsStep(viewModel, state, picked)
                        Step.DONE -> if (result != null && picked != null) {
                            ResultStep(viewModel, result, picked, mode, onSaveResult)
                        }
                    }
                }
            }
        }
    }
}

private enum class Step {
    CHOOSE, PASSWORD, OPTIONS, DONE;

    companion object {
        fun of(state: ToolUiState): Step = when {
            state.result != null -> DONE
            state.picked == null -> CHOOSE
            state.picked.locked -> PASSWORD
            else -> OPTIONS
        }
    }
}

// ---------- Step 1: choose a file ----------

@Composable
private fun ChooseStep(state: ToolUiState, onPickPdf: () -> Unit) {
    val mode = state.mode
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = mode.tagline,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val shape = MaterialTheme.shapes.extraLarge
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), shape)
                .clickable(enabled = !state.working, onClick = onPickPdf)
                .padding(vertical = 40.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.working) {
                CircularProgressIndicator(Modifier.size(48.dp))
                Spacer(Modifier.height(16.dp))
                Text("Opening your PDF...", style = MaterialTheme.typography.titleMedium)
            } else {
                IconBadge(
                    icon = when (mode) {
                        ToolMode.COMPRESS -> R.drawable.ic_compress
                        ToolMode.PROTECT -> R.drawable.ic_lock
                        ToolMode.UNLOCK -> R.drawable.ic_unlock
                    },
                    size = 72.dp,
                    shape = CircleShape,
                )
                Spacer(Modifier.height(18.dp))
                Text("Choose a PDF", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick a file from your phone. The original is never changed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        state.error?.let { ErrorBanner(it) }
        if (!state.working) PrimaryButton("Choose PDF", onPickPdf, icon = R.drawable.ic_pdf_import)
        PrivacyNote("Works fully offline. Your file never leaves this phone.")
    }
}

// ---------- Step 2: the file needs a password to open ----------

@Composable
private fun OpenPasswordStep(viewModel: MainViewModel, state: ToolUiState, picked: PickedPdf) {
    val form = viewModel.toolForm
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FileCard(picked, locked = true)
        Text(
            "This PDF is protected. Enter its password to continue.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        PasswordField(
            value = form.openPassword,
            onValueChange = { form.openPassword = it },
            label = "Password",
            error = state.error,
        )
        if (state.working) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            PrimaryButton(
                text = "Open PDF",
                onClick = { viewModel.enterOpenPassword(form.openPassword) },
                icon = R.drawable.ic_unlock,
                enabled = form.openPassword.isNotEmpty(),
            )
        }
        TextButton(onClick = { viewModel.toolPickAnother() }, modifier = Modifier.fillMaxWidth()) {
            Text("Choose a different file")
        }
    }
}

// ---------- Step 3: options for the chosen tool ----------

@Composable
private fun OptionsStep(viewModel: MainViewModel, state: ToolUiState, picked: PickedPdf) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FileCard(picked, locked = false, onChange = { viewModel.toolPickAnother() })
        when (state.mode) {
            ToolMode.COMPRESS -> CompressOptions(viewModel, state, picked)
            ToolMode.PROTECT -> ProtectOptions(viewModel, state, picked)
            ToolMode.UNLOCK -> UnlockOptions(viewModel, state, picked)
        }
        state.error?.let { ErrorBanner(it) }
    }
}

@Composable
private fun CompressOptions(viewModel: MainViewModel, state: ToolUiState, picked: PickedPdf) {
    val form = viewModel.toolForm
    SectionTitle("Compression")
    for (level in CompressionLevel.entries) {
        ChoiceCard(
            title = level.label,
            subtitle = level.hint,
            selected = form.level == level,
            onClick = { form.level = level },
        )
    }
    if (picked.wasProtected) {
        PrivacyNote("This file's password is kept on the compressed copy.")
    }
    if (state.working) {
        WorkingCard("Compressing...", state.progress)
    } else {
        PrimaryButton("Compress PDF", { viewModel.runCompress(form.level) }, icon = R.drawable.ic_compress)
    }
    PrivacyNote("Pictures are re-encoded. Text and layout stay sharp. Your original file is untouched.")
}

@Composable
private fun ProtectOptions(viewModel: MainViewModel, state: ToolUiState, picked: PickedPdf) {
    val form = viewModel.toolForm
    val passwordProblem = if (form.password.isEmpty()) null else PasswordRules.problem(form.password)
    val mismatch = form.confirm.isNotEmpty() && form.confirm != form.password
    val ownerProblem = when {
        !form.restrict -> null
        form.ownerPassword.isEmpty() -> null
        PasswordRules.problem(form.ownerPassword) != null -> PasswordRules.problem(form.ownerPassword)
        form.ownerPassword == form.password -> "Use a different password from the one to open"
        else -> null
    }
    val ready = PasswordRules.problem(form.password) == null &&
        form.confirm == form.password &&
        (!form.restrict || (form.ownerPassword.isNotEmpty() && ownerProblem == null))

    SectionTitle("Password")
    PasswordField(
        value = form.password,
        onValueChange = { form.password = it },
        label = "Password to open",
        error = passwordProblem,
    )
    StrengthMeter(form.password)
    PasswordField(
        value = form.confirm,
        onValueChange = { form.confirm = it },
        label = "Confirm password",
        error = if (mismatch) "Passwords do not match" else null,
    )

    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Also lock editing and copying", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Printing stays allowed. Needs a second, owner password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = form.restrict, onCheckedChange = { form.restrict = it })
            }
            if (form.restrict) {
                PasswordField(
                    value = form.ownerPassword,
                    onValueChange = { form.ownerPassword = it },
                    label = "Owner password",
                    error = ownerProblem,
                    supporting = "Keep this one safe. It unlocks editing.",
                )
            }
        }
    }

    if (picked.wasProtected) {
        PrivacyNote("This file already has a password. It will be replaced by the new one.")
    }
    if (state.working) {
        WorkingCard("Encrypting...", null)
    } else {
        PrimaryButton(
            text = "Protect PDF",
            onClick = {
                viewModel.runProtect(
                    Protection(
                        userPassword = form.password,
                        ownerPassword = form.ownerPassword.takeIf { form.restrict },
                        restrict = form.restrict,
                    ),
                )
            },
            icon = R.drawable.ic_lock,
            enabled = ready,
        )
    }
    PrivacyNote("AES-256 encryption. There is no way to recover a forgotten password, so write it down somewhere safe.")
}

@Composable
private fun UnlockOptions(viewModel: MainViewModel, state: ToolUiState, picked: PickedPdf) {
    if (!picked.wasProtected) {
        InfoCard(
            icon = R.drawable.ic_check,
            title = "No password to remove",
            body = "This PDF opens without a password. If you want to lock it, use Protect instead.",
        )
        PrimaryButton("Protect this PDF", { viewModel.openTool(ToolMode.PROTECT) }, icon = R.drawable.ic_lock)
        return
    }
    InfoCard(
        icon = R.drawable.ic_unlock,
        title = "Password accepted",
        body = "Remove the password to get a copy anyone can open. Only do this with files you own or may unlock.",
    )
    if (state.working) {
        WorkingCard("Removing the password...", null)
    } else {
        PrimaryButton("Remove password", { viewModel.runUnlock() }, icon = R.drawable.ic_unlock)
    }
}

// ---------- Step 4: the result ----------

@Composable
private fun ResultStep(
    viewModel: MainViewModel,
    result: ToolResult,
    picked: PickedPdf,
    mode: ToolMode,
    onSaveResult: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (result) {
            is ToolResult.Compressed -> CompressedSummary(result)
            is ToolResult.AlreadySmall -> {
                InfoCard(
                    icon = R.drawable.ic_check,
                    title = "Already as small as it gets",
                    body = "Compressing would not make this PDF smaller (${Sizes.format(result.sizeBytes)}), so your file was left as it is. Try a higher level.",
                )
            }
            is ToolResult.Finished -> {
                SuccessHero(
                    title = if (mode == ToolMode.PROTECT) "PDF protected" else "Password removed",
                    subtitle = result.note,
                    icon = if (mode == ToolMode.PROTECT) R.drawable.ic_lock else R.drawable.ic_unlock,
                )
                FileLine(result.fileName, Sizes.format(result.sizeBytes))
            }
        }
        if (result !is ToolResult.AlreadySmall) {
            PrimaryButton("Save to phone", { onSaveResult(result.fileName) }, icon = R.drawable.ic_download)
            TonalButton("Share", { viewModel.shareToolResult() }, icon = R.drawable.ic_share)
        } else {
            PrimaryButton("Try again", { viewModel.clearToolResult() })
        }
        TextButton(onClick = { viewModel.toolPickAnother() }, modifier = Modifier.fillMaxWidth()) {
            Text("Work on another PDF")
        }
        PrivacyNote("The original file \"${picked.name}.pdf\" was not changed.")
    }
}

@Composable
private fun CompressedSummary(result: ToolResult.Compressed) {
    val saved = Sizes.percentSaved(result.beforeBytes, result.afterBytes)
    val animated by animateFloatAsState(saved / 100f, label = "saved")
    val extras = ScanwellTheme.extras
    SuccessHero(
        title = "$saved% smaller",
        subtitle = "Saved ${Sizes.format(result.beforeBytes - result.afterBytes)}",
        icon = R.drawable.ic_compress,
    )
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SizeColumn("Before", Sizes.format(result.beforeBytes), MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                SizeColumn("After", Sizes.format(result.afterBytes), extras.success, Alignment.End)
            }
            LinearProgressIndicator(
                progress = { 1f - animated },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape),
                color = extras.success,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                text = if (result.picturesShrunk == 1) "1 picture optimized" else "${result.picturesShrunk} pictures optimized",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SizeColumn(label: String, value: String, color: Color, align: Alignment.Horizontal = Alignment.Start) {
    Column(horizontalAlignment = align) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, color = color)
    }
}

@Composable
private fun SuccessHero(title: String, subtitle: String, icon: Int) {
    val extras = ScanwellTheme.extras
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon, size = 72.dp, shape = CircleShape, tint = extras.success, background = extras.successContainer)
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ---------- Shared pieces ----------

@Composable
private fun FileCard(picked: PickedPdf, locked: Boolean, onChange: (() -> Unit)? = null) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(if (locked) R.drawable.ic_lock else R.drawable.ic_pdf, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    picked.name + ".pdf",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                val pages = when {
                    locked -> "Protected"
                    picked.pageCount == 1 -> "1 page"
                    else -> "${picked.pageCount} pages"
                }
                Text(
                    "$pages · ${Sizes.format(picked.sizeBytes)}" + if (!locked && picked.wasProtected) " · Protected" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onChange != null) {
                TextButton(onClick = onChange) { Text("Change") }
            }
        }
    }
}

@Composable
private fun FileLine(name: String, size: String) {
    SoftCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBadge(R.drawable.ic_pdf, size = 44.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(size, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WorkingCard(text: String, progress: Float?) {
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text, style = MaterialTheme.typography.titleSmall)
            if (progress == null) {
                LinearProgressIndicator(Modifier.fillMaxWidth().clip(CircleShape))
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                )
            }
            Text(
                "Keep Scanwell open until this finishes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(icon: Int, title: String, body: String) {
    SoftCard(Modifier.fillMaxWidth().animateContentSize()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            IconBadge(icon, size = 40.dp, shape = CircleShape)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(2.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ErrorBanner(message: String) {
    val shape = MaterialTheme.shapes.medium
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(R.drawable.ic_warning),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun PrivacyNote(text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            painterResource(R.drawable.ic_shield),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
