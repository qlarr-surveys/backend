package com.frankie.backend.services

import com.frankie.backend.api.event.EventDTO
import com.frankie.backend.common.UserUtils
import com.frankie.backend.exceptions.AuthorizationException
import com.frankie.backend.exceptions.UserNotFoundException
import com.frankie.backend.mappers.EventMapper
import com.frankie.backend.persistence.repositories.EventRepository
import com.frankie.backend.persistence.repositories.UserRepository
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
