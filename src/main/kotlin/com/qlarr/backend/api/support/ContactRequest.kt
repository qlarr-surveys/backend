package com.qlarr.backend.api.support

data class ContactRequest(
    val name: String,
    val email: String,
    val message: String
)
