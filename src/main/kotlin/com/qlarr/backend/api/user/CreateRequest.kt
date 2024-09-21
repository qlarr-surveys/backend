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

data class SignupRequest(
        val firstName: String,
        val lastName: String,
        val email: String,
        val password: String
) {
    fun email() = email.trim().lowercase()
    fun trim() = copy(
            firstName = firstName.trim(),
            lastName = lastName.trim(),
            email = email()
    )
}