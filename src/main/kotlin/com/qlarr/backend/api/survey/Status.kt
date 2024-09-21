package com.qlarr.backend.api.survey

import com.fasterxml.jackson.annotation.JsonProperty

enum class Status {

    @JsonProperty("draft")
    DRAFT,

    @JsonProperty("active")
    ACTIVE,

    @JsonProperty("closed")
    CLOSED,
}
