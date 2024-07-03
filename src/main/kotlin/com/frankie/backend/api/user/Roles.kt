package com.frankie.backend.api.user

import com.fasterxml.jackson.annotation.JsonProperty


enum class Roles {
    @JsonProperty("super_admin")
    SUPER_ADMIN,

    @JsonProperty("survey_admin")
    SURVEY_ADMIN,

    @JsonProperty("surveyor")
    SURVEYOR,

    @JsonProperty("analyst")
    ANALYST
}
