package com.ntou01157.hunter.ui

// frontend/app/src/main/java/com/ntou01157/hunter/ui/Event_UI.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.utils.NetworkResult

@Composable
fun EventScreen(eventViewModel: EventViewModel = viewModel()) {
    val dailyEventsResult by eventViewModel.dailyEvents.collectAsState()
    val permanentEventsResult by eventViewModel.permanentEvents.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "每日事件", style = MaterialTheme.typography.headlineMedium)
        EventList(eventsResult = dailyEventsResult, onEventClick = { event ->
            // 處理每日事件點擊，導航到對應的 UI
            // 例如：if (event.name == "神秘商人的試煉") NavController.navigate("merchant/${event.id}")
        })

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "任務板事件", style = MaterialTheme.typography.headlineMedium)
        EventList(eventsResult = permanentEventsResult, onEventClick = { event ->
            // 處理任務板事件點擊，導航到對應的 UI
        })
    }
}

@Composable
fun EventList(eventsResult: NetworkResult<List<EventModel>>, onEventClick: (EventModel) -> Unit) {
    when (eventsResult) {
        is NetworkResult.Loading -> {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
        is NetworkResult.Success -> {
            LazyColumn {
                eventsResult.data?.let { events ->
                    items(events) { event ->
                        EventCard(event = event, onClick = { onEventClick(event) })
                    }
                }
            }
        }
        is NetworkResult.Error -> {
            Text(text = eventsResult.message ?: "載入事件失敗", color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCard(event: EventModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = event.name, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = event.description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}