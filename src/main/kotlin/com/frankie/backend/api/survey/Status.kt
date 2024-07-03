package com.frankie.backend.api.survey

import com.fasterxml.jackson.annotation.JsonProperty

enum class Status {

    @JsonProperty("draft")
    DRAFT,

    @JsonProperty("active")
    ACTIVE,

    @JsonProperty("closed")
    CLOSED,
}
