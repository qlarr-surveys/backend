package com.qlarr.backend.api.user


interface CountByRoleResponse {
    val superAdmin: Long
    val surveyAdmin: Long
    val surveyor: Long
}
