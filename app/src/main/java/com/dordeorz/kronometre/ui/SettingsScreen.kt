package com.dordeorz.kronometre.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dordeorz.kronometre.R
import com.dordeorz.kronometre.data.AppSettings
import com.dordeorz.kronometre.data.ThemeMode
import com.dordeorz.kronometre.data.UpdateChecker
import com.dordeorz.kronometre.data.UpdateStatus
import com.dordeorz.kronometre.ui.theme.KronometreTheme

@Composable
fun SettingsScreen(
    settings: AppSettings,
    version: String,
    onThemeModeChange: (ThemeMode) -> Unit,
    onShowCentisChange: (Boolean) -> Unit,
    onDimScreenChange: (Boolean) -> Unit,
    updateStatus: UpdateStatus,
    onCheckUpdates: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onBack: () -> Unit,
) {
    val themeMode = settings.themeMode
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            }
            SectionTitle(stringResource(R.string.settings_theme))
            Column(Modifier.selectableGroup()) {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = mode == themeMode,
                                onClick = { onThemeModeChange(mode) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        RadioButton(selected = mode == themeMode, onClick = null)
                        Text(stringResource(themeLabel(mode)), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_stopwatch))
            SwitchRow(
                title = stringResource(R.string.settings_centis),
                hint = stringResource(R.string.settings_centis_hint),
                checked = settings.showCentis,
                onCheckedChange = onShowCentisChange,
            )
            SwitchRow(
                title = stringResource(R.string.settings_dim),
                hint = stringResource(R.string.settings_dim_hint),
                checked = settings.dimScreen,
                onCheckedChange = onDimScreenChange,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_updates))
            UpdateRow(updateStatus, onCheckUpdates, onOpenUrl)
            ClickRow(
                title = stringResource(R.string.settings_github),
                hint = stringResource(R.string.settings_github_hint),
                onClick = { onOpenUrl(UpdateChecker.REPO_URL) },
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle(stringResource(R.string.settings_about))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.settings_version), style = MaterialTheme.typography.bodyLarge)
                Text(version, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun UpdateRow(status: UpdateStatus, onCheck: () -> Unit, onOpenUrl: (String) -> Unit) {
    val hint = when (status) {
        UpdateStatus.Unknown -> null
        UpdateStatus.Checking -> stringResource(R.string.update_checking)
        UpdateStatus.UpToDate -> stringResource(R.string.update_up_to_date)
        is UpdateStatus.Available -> stringResource(R.string.update_available, status.version)
        UpdateStatus.Failed -> stringResource(R.string.update_failed)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = status != UpdateStatus.Checking, onClick = onCheck)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.settings_check_updates), style = MaterialTheme.typography.bodyLarge)
            if (hint != null) {
                Text(
                    hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (status is UpdateStatus.Available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (status is UpdateStatus.Available) {
            Button(onClick = { onOpenUrl(status.url) }) { Text(stringResource(R.string.update_download)) }
        }
    }
}

@Composable
private fun ClickRow(title: String, hint: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SwitchRow(title: String, hint: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Switch)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

private fun themeLabel(mode: ThemeMode) = when (mode) {
    ThemeMode.System -> R.string.theme_system
    ThemeMode.Light -> R.string.theme_light
    ThemeMode.Dark -> R.string.theme_dark
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    KronometreTheme {
        SettingsScreen(
            settings = AppSettings(),
            version = "1.0.0",
            onThemeModeChange = {},
            onShowCentisChange = {},
            onDimScreenChange = {},
            updateStatus = UpdateStatus.Available("1.0.1", ""),
            onCheckUpdates = {},
            onOpenUrl = {},
            onBack = {},
        )
    }
}
