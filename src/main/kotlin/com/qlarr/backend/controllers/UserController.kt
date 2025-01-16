package com.qlarr.backend.controllers

import com.qlarr.backend.api.user.*
import com.qlarr.backend.services.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
class UserController(
    private val userService: UserService,
) {

    @GetMapping("/user/all")
    @PreAuthorize("hasAnyAuthority({'super_admin', 'survey_admin'})")
    fun getAll(): ResponseEntity<List<UserDTO>> {
        val allUsers = userService.getAllUsers()
        return ResponseEntity(allUsers, HttpStatus.OK)
    }

    @GetMapping("/user/{userId}")
    fun getById(@PathVariable userId: UUID): ResponseEntity<UserDTO> {
        val userDTO = userService.getUserById(userId)
        return ResponseEntity(userDTO, HttpStatus.OK)
    }

    @PostMapping("/user/create")
    @PreAuthorize("hasAuthority('super_admin')")
    fun create(@RequestBody createRequest: CreateRequest): ResponseEntity<UserDTO> {
        val userDTO = userService.create(createRequest)
        return ResponseEntity(userDTO, HttpStatus.OK)
    }

    @PutMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('super_admin')")
    fun edit(
        @PathVariable userId: UUID,
        @RequestBody editUserRequest: EditUserRequest,
    ): ResponseEntity<UserDTO> {
        val userDTO = userService.update(userId, editUserRequest)
        return ResponseEntity(userDTO, HttpStatus.OK)
    }

    @PutMapping("/user/profile")
    fun editProfile(
        @RequestBody editProfileRequest: EditProfileRequest,
    ): ResponseEntity<UserDTO> {
        val userDTO = userService.editProfile(editProfileRequest)
        return ResponseEntity(userDTO, HttpStatus.OK)
    }

    @DeleteMapping("/user/{userId}")
    @PreAuthorize("hasAuthority('super_admin')")
    fun delete(@PathVariable userId: UUID): ResponseEntity<Any> {
        userService.delete(userId)
        return ResponseEntity(HttpStatus.NO_CONTENT)
    }

    @GetMapping("/user/count_by_role")
    @PreAuthorize("hasAnyAuthority({'super_admin'})")
    fun getUserCountByRole(): ResponseEntity<CountByRoleResponse> {
        val countByRoleResponse = userService.countUserRoles()
        return ResponseEntity(countByRoleResponse, HttpStatus.OK)
    }

    @PostMapping("/user/confirm_new_email")
    fun confirmNewEmail(@RequestBody confirmEmailRequest: ConfirmEmailRequest): ResponseEntity<LoggedInUserResponse> {
        val loggedInUserResponse = userService.confirmNewEmail(confirmEmailRequest)
        return ResponseEntity(loggedInUserResponse, HttpStatus.OK)
    }
}
