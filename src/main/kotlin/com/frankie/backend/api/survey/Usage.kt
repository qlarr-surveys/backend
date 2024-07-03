package com.frankie.backend.api.survey

import com.fasterxml.jackson.annotation.JsonProperty

enum class Usage {

    @JsonProperty("web")
    WEB,

    @JsonProperty("offline")
    OFFLINE,

    @JsonProperty("mixed")
    MIXED,
}
