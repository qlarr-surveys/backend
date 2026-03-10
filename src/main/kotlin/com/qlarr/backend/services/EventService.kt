package com.qlarr.backend.services

import com.qlarr.backend.api.event.EventDTO
import com.qlarr.backend.common.UserUtils
import com.qlarr.backend.exceptions.AuthorizationException
import com.qlarr.backend.exceptions.UserNotFoundException
import com.qlarr.backend.mappers.EventMapper
import com.qlarr.backend.persistence.repositories.EventRepository
import com.qlarr.backend.persistence.repositories.UserRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class EventService(
    private val eventRepository: EventRepository,
    private val userRepository: UserRepository,
    private val eventMapper: EventMapper,
    private val userUtils: UserUtils
) {

    fun getAllEventsForUser(userId: UUID, from: LocalDateTime?, to: LocalDateTime?): List<EventDTO>? {
        userRepository.findByIdAndDeletedIsFalse(userId) ?: throw UserNotFoundException()
        return eventRepository.findAllByUserId(userId, from, to).map { eventMapper.mapToDto(it) }
    }

    fun createNewUserEvent(userId: UUID, eventDTO: EventDTO) {
        if (userUtils.currentUserId() != userId) {
            throw AuthorizationException()
        }
        userRepository.findByIdAndDeletedIsFalse(userId) ?: throw UserNotFoundException()
        eventRepository.save(eventMapper.mapToEntity(eventDTO, userId))
    }
}
