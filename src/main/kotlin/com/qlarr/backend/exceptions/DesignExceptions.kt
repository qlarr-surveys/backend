package com.qlarr.backend.exceptions

class NoPublishedVersionException : Throwable()
class DesignException : Throwable()
class InvalidDesignException : Throwable()
class DesignOutOfSyncException(val subVersion: Int) : Throwable()


class FromCodeNotAvailableException: Throwable()
class DuplicateToCodeException: Throwable()
class InvalidCodeChangeException: Throwable()
class IdenticalFromToCodesException: Throwable()


class AutoCompleteMalformedInputException: Throwable()