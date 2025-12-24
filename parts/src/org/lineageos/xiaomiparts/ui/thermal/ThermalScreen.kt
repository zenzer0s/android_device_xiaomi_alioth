package org.lineageos.xiaomiparts.ui.thermal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.lineageos.xiaomiparts.R
import org.lineageos.xiaomiparts.ui.components.BaseSettingsScreen
import org.lineageos.xiaomiparts.theme.CustomColors
import org.lineageos.xiaomiparts.theme.GroupedListDefaults


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThermalScreen(
    viewModel: ThermalViewModel,
    onBackPressed: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    var showBottomSheet by remember { mutableStateOf(false) }
    var appToEdit by remember { mutableStateOf<AppThermalState?>(null) }
    
    BaseSettingsScreen(
        title = stringResource(R.string.thermal_title),
        onBackPressed = onBackPressed,
        actions = {
            IconButton(
                onClick = { showResetDialog = true },
                enabled = uiState.isEnabled && uiState.apps.isNotEmpty()
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.thermal_reset)
                )
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.VerticalSpacing)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(), // Removed padding
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_thermal_settings),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.thermal_enable),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = { viewModel.toggleThermalEnabled(it) },
                            colors = CustomColors.switchColors,
                            thumbContent = {
                                Icon(
                                    imageVector = if (uiState.isEnabled) Icons.Rounded.Check else Icons.Rounded.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        )
                    }
                }
            }
            if (uiState.isEnabled) {
                when {
                    uiState.isLoading -> {
                        item { LoadingState() }
                    }
                    uiState.error != null -> {
                        item { ErrorState(error = uiState.error!!) }
                    }
                    uiState.apps.isEmpty() -> {
                        item { EmptyState() }
                    }
                    else -> {
                        item {
                            Text(
                                text = stringResource(R.string.thermal_apps_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                        itemsIndexed(
                            items = uiState.apps,
                            key = { _, app -> app.packageName }
                        ) { index, app ->
                            val shape = GroupedListDefaults.getShape(index, uiState.apps.size)

                            AppThermalItem(
                                app = app,
                                onClick = {
                                    appToEdit = app
                                    showBottomSheet = true
                                },
                                modifier = Modifier.clip(shape)
                            )
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showResetDialog) {
        ResetConfirmationDialog(
            onConfirm = {
                viewModel.resetProfiles()
                showResetDialog = false
            },
            onDismiss = { showResetDialog = false }
        )
    }

    if (showBottomSheet && appToEdit != null) {
        val appFromUiState = uiState.apps.find { it.packageName == appToEdit!!.packageName }
        val currentActualState = appFromUiState?.currentState ?: appToEdit!!.currentState

        ButtonGroup(
            appLabel = appToEdit!!.label,
            currentState = currentActualState,
            onDismiss = { showBottomSheet = false },
            onStateSelected = { newState ->
                viewModel.updateAppThermalState(appToEdit!!.packageName, newState)
            }
        )
    }
}

@Composable
private fun LazyItemScope.LoadingState() {
    Box(
        modifier = Modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.thermal_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LazyItemScope.ErrorState(error: String) {
    Box(
        modifier = Modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.thermal_error, error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun LazyItemScope.EmptyState() {
    Box(
        modifier = Modifier.fillParentMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = stringResource(R.string.thermal_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.thermal_empty_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}