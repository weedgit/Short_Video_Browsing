package com.shortvideo.feature.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shortvideo.domain.model.InboxNotification
import com.shortvideo.domain.repository.InboxRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InboxUiState(
    val isLoading: Boolean = true,
    val notifications: List<InboxNotification> = emptyList(),
    val unreadCount: Int = 0,
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val inboxRepository: InboxRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { inboxRepository.getNotifications() }
                .onSuccess { (items, unread) ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            notifications = items,
                            unreadCount = unread,
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isLoading = false) }
                }
        }
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            inboxRepository.markRead(id)
            _uiState.update { state ->
                state.copy(
                    notifications = state.notifications.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    },
                    unreadCount = (state.unreadCount - 1).coerceAtLeast(0),
                )
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            inboxRepository.markAllRead()
            _uiState.update { state ->
                state.copy(
                    notifications = state.notifications.map { it.copy(isRead = true) },
                    unreadCount = 0,
                )
            }
        }
    }
}
