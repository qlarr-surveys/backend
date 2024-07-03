package com.frankie.backend.exceptions

class SurveyIsActiveException : Throwable()

class SurveyIsClosedException : Throwable()

class SurveyIsNotActiveException : Throwable()

class ResumeNotAllowed : Throwable()

class PreviousNotAllowed : Throwable()

class JumpNotAllowed : Throwable()

class SurveyNotFoundException : Throwable()

class DuplicateSurveyException : Throwable()

class InvalidSurveyDates : Throwable()
class IncompleteResponse : Throwable()
class InvalidResponse : Throwable()
class InvalidSurveyName : Throwable()