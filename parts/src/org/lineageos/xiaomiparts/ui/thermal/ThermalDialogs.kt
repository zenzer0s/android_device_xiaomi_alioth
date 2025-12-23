package org.lineageos.xiaomiparts.ui.thermal

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.lineageos.xiaomiparts.R

@Composable
fun ResetConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.thermal_reset))
        },
        text = {
            Text(text = stringResource(R.string.thermal_reset_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(android.R.string.yes))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.no))
            }
        }
    )
}
