package com.qlarr.backend.exceptions

class ResponseNotFoundException : Throwable()
class WrongColumnException(val columnName: String) : Throwable("Wrong column name: $columnName")
class WrongValueType(val columnName: String, val expectedClassName: String, val actualClassName: String) :
        Throwable("Wrong value type for $columnName, expected $expectedClassName found $actualClassName")

class ResponseAlreadySyncedException : Throwable()

class UnrecognizedZoneException(val zone:String) : Throwable()
class InvalidQuestionId : Throwable()
