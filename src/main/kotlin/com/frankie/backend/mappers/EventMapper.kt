package com.frankie.backend.mappers

import com.frankie.backend.api.event.EventDTO
import com.frankie.backend.persistence.entities.EventEntity
import org.springframework.stereotype.Component
import java.util.*

@Component
class EventMapper {

    fun mapToDto(eventEntity: EventEntity) = EventDTO(eventEntity.time, eventEntity.name, eventEntity.details)
    fun mapToEntity(eventDTO: EventDTO, userId: UUID) = EventEntity(
        time = eventDTO.time, name = eventDTO.name, details = eventDTO.details, userId = userId
    )
}