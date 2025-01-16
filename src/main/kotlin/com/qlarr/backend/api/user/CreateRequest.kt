package com.qlarr.backend.api.user


data class CreateRequest(
        val firstName: String,
        val lastName: String,
        val email: String,
        val roles: Set<Roles>,
) {
    fun email() = email.trim().lowercase()
}

data class EditUserRequest(
        val firstName: String? = null,
        val lastName: String? = null,
        val roles: Set<Roles>? = null,
)

data class EditProfileRequest(
        val firstName: String? = null,
        val lastName: String? = null,
        val email: String? = null,
        val password: String? = null,
        val newPassword: String? = null,
) {
    fun email() = email?.trim()?.lowercase()
}