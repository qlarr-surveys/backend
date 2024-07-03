package com.frankie.backend.controllers

import com.frankie.backend.services.UserService
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.context.ContextConfiguration

@ContextConfiguration(classes = [AccessController::class])
class AccessControllerIntegrationTest(
) : IntegrationTestBase() {

    @MockkBean
    private lateinit var userService: UserService
}
