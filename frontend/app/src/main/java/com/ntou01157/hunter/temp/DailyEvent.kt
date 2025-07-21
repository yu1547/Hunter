package com.ntou01157.hunter

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp

data class DailyEvent(
    val title: String,
    val description: String,
    val options: List<String>
)

//事件選擇視窗
@Composable
fun DailyEventDialog(
    event: DailyEvent,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("關閉")
            }
        },
        title = { Text(event.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(event.description)
                event.options.forEach { option ->
                    Button(
                        onClick = {
                            onOptionSelected(option)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option)
                    }
                }
            }
        }
    )
}
