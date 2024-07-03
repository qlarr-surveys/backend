package com.frankie.backend.api.user


data class RefreshRequest(
    val accessToken: String,
    val refreshToken: String,
)
