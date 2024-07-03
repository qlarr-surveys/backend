package com.frankie.backend.api.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ResponseUploadFile(
    val filename: String,
    @JsonProperty("stored_filename")
    val storedFilename: String,
    val size: Long,
    val type: String
)