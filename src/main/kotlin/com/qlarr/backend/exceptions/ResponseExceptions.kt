package com.qlarr.backend.exceptions

class ResponseNotFoundException : Throwable()
class WrongColumnException(val columnName: String) : Throwable("Wrong column name: $columnName")
class WrongValueType(val columnName: String, val expectedClassName: String, val actualClassName: String) :
    Throwable("Wrong value type for $columnName, expected $expectedClassName found $actualClassName")

class ResponseAlreadySyncedException : Throwable()

class UnrecognizedZoneException(val zone: String) : Throwable()
class InvalidQuestionId : Throwable()
class FileTooBigException(
    actualSize: Long,
    maxSize: Int,
    mimeType: String
) : RuntimeException(
    "File too large: ${actualSize / 1024 / 1024} MB. Maximum allowed for $mimeType is ${maxSize / 1024 / 1024} MB."
)
