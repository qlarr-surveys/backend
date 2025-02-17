package com.qlarr.backend

import com.qlarr.backend.services.UserService
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class DataInitializer(private val userService: UserService) {

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationEvent() {
        userService.createFirstUser()
    }
}
