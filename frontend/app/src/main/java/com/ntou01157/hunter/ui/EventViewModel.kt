// frontend/app/src/main/java/com/ntou01157/hunter/ui/EventViewModel.kt

package com.ntou01157.hunter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.data.EventRepository
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.utils.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventViewModel(private val eventRepository: EventRepository) : ViewModel() {

    private val _dailyEvents = MutableStateFlow<NetworkResult<List<EventModel>>>(NetworkResult.Loading())
    val dailyEvents: StateFlow<NetworkResult<List<EventModel>>> = _dailyEvents

    private val _permanentEvents = MutableStateFlow<NetworkResult<List<EventModel>>>(NetworkResult.Loading())
    val permanentEvents: StateFlow<NetworkResult<List<EventModel>>> = _permanentEvents

    private val _currentEventResult = MutableStateFlow<NetworkResult<Any>>(NetworkResult.Loading())
    val currentEventResult: StateFlow<NetworkResult<Any>> = _currentEventResult

    init {
        fetchDailyEvents()
        fetchPermanentEvents()
    }

    fun fetchDailyEvents() {
        viewModelScope.launch {
            _dailyEvents.value = NetworkResult.Loading()
            _dailyEvents.value = eventRepository.getDailyEvents()
        }
    }

    fun fetchPermanentEvents() {
        viewModelScope.launch {
            _permanentEvents.value = NetworkResult.Loading()
            _permanentEvents.value = eventRepository.getPermanentEvents()
        }
    }

    fun handleMerchantExchange(option: String) {
        viewModelScope.launch {
            _currentEventResult.value = NetworkResult.Loading()
            val result = eventRepository.exchangeItems(option)
            _currentEventResult.value = result
        }
    }

    // 這裡可以新增更多處理不同事件的方法
    // 例如：handleSlimeAttack(), openTreasureBox(), ...
}