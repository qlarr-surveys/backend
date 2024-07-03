package com.frankie.backend.persistence.repositories

import com.frankie.backend.persistence.entities.EventEntity
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.ListCrudRepository
import java.time.LocalDateTime
import java.util.*

interface EventRepository : ListCrudRepository<EventEntity, UUID> {

    @Query(
        "SELECT e FROM EventEntity e " +
                "WHERE e.userId = :userId " +
                "AND (cast(:from as date) is null or e.time > :from) " +
                "AND (cast(:to as date) is null or e.time < :to) "
    )
    fun findAllByUserId(userId: UUID, from: LocalDateTime?, to: LocalDateTime?): List<EventEntity>
}
