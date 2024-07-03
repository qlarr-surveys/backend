package com.frankie.backend.services

import com.frankie.backend.api.event.EventDTO
import com.frankie.backend.common.UserUtils
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.AuthorizationException
import com.frankie.backend.exceptions.UserNotFoundException
import com.frankie.backend.mappers.EventMapper
import com.frankie.backend.persistence.entities.EventEntity
import com.frankie.backend.persistence.repositories.EventRepository
import com.frankie.backend.persistence.repositories.UserRepository
import com.frankie.backend.services.UserServiceTest.Companion.generateSurveyor
import io.mockk.*
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.persistence.*
import org.junit.Assert.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockKExtension::class)
class EventServiceTest {

    private val eventMapper = EventMapper()

    @MockK
    private lateinit var eventRepository: EventRepository

    @MockK
    private lateinit var userRepository: UserRepository

    @MockK
    private lateinit var userUtils: UserUtils

    @InjectMockKs
    private lateinit var eventService: EventService


    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }


    @Test
    fun `cannot add an event to a non-existent user`() {
        val userId = UUID.randomUUID()
        every { userUtils.currentUserId() } returns userId
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns null
        assertThrows(UserNotFoundException::class.java) {
            eventService.createNewUserEvent(userId, buildEventDto())
        }
    }

    @Test
    fun `cannot add an event to a anyone but oneself user`() {
        val userId = UUID.randomUUID()
        every { userUtils.currentUserId() } returns UUID.randomUUID()
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns generateSurveyor(userId)
        assertThrows(AuthorizationException::class.java) {
            eventService.createNewUserEvent(userId, buildEventDto())
        }
    }

    @Test
    fun `can add event to oneself`() {
        val userId = UUID.randomUUID()
        val event = slot<EventEntity>()
        every { userUtils.currentUserId() } returns userId
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns generateSurveyor(userId)
        every { eventRepository.save(capture(event)) } returns buildEventEntity(userId)
        eventService.createNewUserEvent(userId, buildEventDto())
        Assertions.assertEquals(event.captured.userId, userId)
    }

    @Test
    fun `fetching an event for non-existent user throws user not found`() {
        val userId = UUID.randomUUID()
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns null
        assertThrows(UserNotFoundException::class.java) {
            eventService.getAllEventsForUser(userId, null, null)
        }
    }

    @Test
    fun `fetching otherwise works`() {
        val userId = UUID.randomUUID()
        val event = buildEventEntity(userId)
        every { userRepository.findByIdAndDeletedIsFalse(userId) } returns generateSurveyor(userId)
        every { eventRepository.findAllByUserId(userId, any(), any()) } returns listOf(event)
        val returnedEvents = eventService.getAllEventsForUser(userId, null, null)
        Assertions.assertEquals(event.time, returnedEvents!![0].time)
        Assertions.assertEquals(event.details, returnedEvents[0].details)
    }

    companion object {
        private fun buildEventDto() = EventDTO(
            time = nowUtc().truncatedTo(ChronoUnit.SECONDS),
            name = "name",
            details = "details"
        )

        private fun buildEventEntity(userID: UUID) = EventEntity(
            id = UUID.randomUUID(),
            userId = userID,
            time = nowUtc().truncatedTo(ChronoUnit.SECONDS),
            name = "name",
            details = "details"
        )
    }


}
