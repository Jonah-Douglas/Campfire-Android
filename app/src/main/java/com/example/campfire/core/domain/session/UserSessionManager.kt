package com.example.campfire.core.domain.session

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton


sealed class SessionEvent {
    object SessionInvalidated : SessionEvent()
}

@Singleton
class UserSessionManager @Inject constructor() {
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(replay = 0)
    val sessionEvents = _sessionEvents.asSharedFlow()
    
    suspend fun notifySessionInvalidated() {
        _sessionEvents.emit(SessionEvent.SessionInvalidated)
    }
}