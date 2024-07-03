package com.frankie.backend.api.user

import java.util.*


data class UserDTO(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val isConfirmed: Boolean,
    val roles: Set<Roles>,
)
