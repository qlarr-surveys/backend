package com.frankie.backend.controllers

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.frankie.backend.api.event.EventDTO
import com.frankie.backend.api.user.Roles
import com.frankie.backend.common.DATE_TIME_UTC_FORMAT
import com.frankie.backend.common.nowUtc
import com.frankie.backend.exceptions.UserNotFoundException
import com.frankie.backend.services.EventService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.justRun
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers.print
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

@ContextConfiguration(classes = [EventController::class])
class EventControllerIntegrationTest : IntegrationTestBase() {


    @MockkBean
    private lateinit var eventService: EventService
    private val objectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())


    @Test
    fun test_getAll_unfilteredEmpty_expect_emptyResult() {
        val userId = UUID.randomUUID()
        every { eventService.getAllEventsForUser(userId, null, null) } returns emptyList()

        val result = mockMvc.perform(
            get("/user/{userId}/event", userId)
                .withRole(Roles.SUPER_ADMIN)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val eventResultList = mapEventsJSONToEventDtoList(result)
        assertThat(eventResultList).isEmpty()

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, null, null) }
    }

    @Test
    fun test_getAll_unfilteredTwo_expect_twoElements() {
        val userId = UUID.randomUUID()
        val event1: EventDTO = createRandomEventDTO()
        val event2: EventDTO = createRandomEventDTO()
        val eventList = listOf(event1, event2)
        every { eventService.getAllEventsForUser(userId, null, null) } returns eventList

        val result = mockMvc.perform(
            get("/user/{userId}/event", userId)
                .withRole(Roles.SUPER_ADMIN)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val eventResultList = mapEventsJSONToEventDtoList(result)
        assertThat(eventResultList).isNotEmpty
        assertThat(eventResultList.containsAll(eventList))

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, null, null) }
    }

    @Test
    fun test_getAll_fromFilteredTwo_expect_oneElement() {
        val userId = UUID.randomUUID()
        val from = nowUtc().truncatedTo(ChronoUnit.SECONDS)
        val event: EventDTO = createRandomEventDTO()
        val eventList = listOf(event)
        every { eventService.getAllEventsForUser(userId, from, null) } returns eventList

        val result = mockMvc.perform(
            get("/user/{userId}/event?from=${from.format()}", userId)
                .withRole(Roles.SUPER_ADMIN)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val eventResultList = mapEventsJSONToEventDtoList(result)
        assertThat(eventResultList).isNotEmpty
        assertThat(eventResultList.containsAll(eventList))

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, from, null) }
    }

    @Test
    fun test_getAll_fromToFilteredTwo_expect_oneElement() {
        val userId = UUID.randomUUID()
        val from = nowUtc()
        val to = nowUtc()
        val event: EventDTO = createRandomEventDTO()
        val eventList = listOf(event)
        every { eventService.getAllEventsForUser(userId, from, to) } returns eventList

        val result = mockMvc.perform(get("/user/{userId}/event?from=$from&to=$to", userId).withRole(Roles.SUPER_ADMIN))
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val eventResultList = mapEventsJSONToEventDtoList(result)
        assertThat(eventResultList).isNotEmpty
        assertThat(eventResultList.containsAll(eventList))

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, from, to) }
    }

    @Test
    fun test_getAll_toFilteredTwo_expect_oneElement() {
        val userId = UUID.randomUUID()
        val to = nowUtc().truncatedTo(ChronoUnit.SECONDS)
        val event: EventDTO = createRandomEventDTO()
        val eventList = listOf(event)
        every { eventService.getAllEventsForUser(userId, null, to) } returns eventList

        val result = mockMvc.perform(
            get("/user/{userId}/event?to=${to.format()}", userId)
                .withRole(Roles.SUPER_ADMIN)
        )
            .andDo(print())
            .andExpect(status().isOk)
            .andReturn()

        val eventResultList = mapEventsJSONToEventDtoList(result)
        assertThat(eventResultList).isNotEmpty
        assertThat(eventResultList.containsAll(eventList))

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, null, to) }
    }

    @Test
    fun test_getAll_invalidUserId_expect_invalidInput() {
        mockMvc.perform(get("/user/XXXXXX/event").withRole(Roles.SUPER_ADMIN))
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid input")))

        verify(exactly = 0) { eventService.getAllEventsForUser(any(), any(), any()) }
    }

    @Test
    fun test_getAll_unknownUser_expect_userNotFound() {
        val userId = UUID.randomUUID()
        every {
            eventService.getAllEventsForUser(
                userId,
                null,
                null
            )
        } throws UserNotFoundException()

        mockMvc.perform(
            get("/user/{userId}/event", userId)
                .withRole(Roles.SUPER_ADMIN)
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("User not found"))

        verify(exactly = 1) { eventService.getAllEventsForUser(userId, null, null) }
    }

    @Test
    fun `default exceptions are handled`() {
        val userId = UUID.randomUUID()
        every {
            eventService.getAllEventsForUser(
                userId,
                null,
                null
            )
        } throws NullPointerException()

        mockMvc.perform(
            get("/user/{userId}/event", userId)
                .withRole(Roles.SURVEYOR)
        )
            .andDo(print())
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.message").value("Unexpected error"))

    }


    @Test
    fun test_create_validInput_expect_created() {
        val userId = UUID.randomUUID()
        val eventDTO: EventDTO = createRandomEventDTO()
        justRun { eventService.createNewUserEvent(userId, eventDTO) }

        mockMvc.perform(
            post("/user/{userId}/event", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapEventToPayload(eventDTO))
                .withRole(Roles.SUPER_ADMIN)
                .with(csrf())
        )
            .andDo(print())
            .andExpect(status().isCreated)

        verify(exactly = 1) { eventService.createNewUserEvent(userId, eventDTO) }
    }

    @Test
    fun test_create_invalidUserId_expect_invalidInput() {
        val eventDTO: EventDTO = createRandomEventDTO()

        mockMvc.perform(
            post("/user/XXXXXX/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapEventToPayload(eventDTO))
                .with(csrf())
                .withRoles()
        )
            .andDo(print())
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("Invalid input")))

        verify(exactly = 0) { eventService.createNewUserEvent(any(), eventDTO) }
    }

    @Test
    fun test_create_unknownUser_expect_userNotFound() {
        val userId = UUID.randomUUID()
        val eventDTO: EventDTO = createRandomEventDTO()
        every {
            eventService.createNewUserEvent(
                userId,
                eventDTO
            )
        } throws UserNotFoundException()

        mockMvc.perform(
            post("/user/{userId}/event", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapEventToPayload(eventDTO))
                .with(csrf())
                .withRoles()
        )
            .andDo(print())
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("User not found"))

        verify(exactly = 1) { eventService.createNewUserEvent(userId, eventDTO) }
    }


    private fun mapEventsJSONToEventDtoList(result: MvcResult): List<EventDTO> =
        objectMapper.readValue(result.response.contentAsString)

    private fun mapEventToPayload(eventDTO: EventDTO): String = objectMapper.writeValueAsString(eventDTO)

    private fun createRandomEventDTO(): EventDTO = EventDTO(
        nowUtc().truncatedTo(ChronoUnit.SECONDS),
        "name-" + UUID.randomUUID(),
        "details-" + UUID.randomUUID(),
    )

    private fun LocalDateTime.format() = format(DateTimeFormatter.ofPattern(DATE_TIME_UTC_FORMAT))

}
