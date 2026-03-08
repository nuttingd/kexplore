package dev.nutting.kexplore.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ScaleDialogPreview() {
    MaterialTheme {
        var text by remember { mutableStateOf("3") }
        val parsed = text.toIntOrNull()
        val isValid = parsed != null && parsed in 0..100

        AlertDialog(
            onDismissRequest = {},
            title = { Text("Scale Replicas") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Replicas") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !isValid,
                    supportingText = if (text.isNotEmpty() && !isValid) {
                        { Text("Enter a number between 0 and 100") }
                    } else null,
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { parsed?.let {} },
                    enabled = isValid,
                ) {
                    Text("Scale")
                }
            },
            dismissButton = {
                TextButton(onClick = {}) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
fun ScaleDialog(
    currentReplicas: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var text by remember { mutableStateOf(currentReplicas.toString()) }
    val parsed = text.toIntOrNull()
    val isValid = parsed != null && parsed in 0..100

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scale Replicas") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Replicas") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = text.isNotEmpty() && !isValid,
                supportingText = if (text.isNotEmpty() && !isValid) {
                    { Text("Enter a number between 0 and 100") }
                } else null,
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { parsed?.let { onConfirm(it) } },
                enabled = isValid,
            ) {
                Text("Scale")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
