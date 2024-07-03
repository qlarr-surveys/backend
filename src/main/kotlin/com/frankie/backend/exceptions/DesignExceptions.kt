package com.frankie.backend.exceptions

class NoPublishedVersionException : Throwable()
class DesignException : Throwable()
class InvalidDesignException : Throwable()
class ComponentDeletedException(val deletedCode: List<String>) : Throwable()
class DesignOutOfSyncException(val subVersion: Int) : Throwable()