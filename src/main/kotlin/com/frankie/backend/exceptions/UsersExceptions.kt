package com.frankie.backend.exceptions

class WrongConfirmationTokenException : Throwable()

class ExpiredConfirmationTokenException : Throwable()

class UsedConfirmationTokenException : Throwable()

class WrongResetTokenException : Throwable()
class WrongEmailChangePinException : Throwable()

class ExpiredResetTokenException : Throwable()

class WrongEmailOrPasswordException : Throwable()

class DeleteOwnUserException : Throwable()
class UserNotFoundException : Throwable()
class EmptyRolesException : Throwable()
class EditOwnUserException : Throwable()

class WrongCredentialsException : Throwable()

class SignupNotAllowed : Throwable()

class AuthorizationException : Throwable()

class DuplicateEmailException : Throwable()

class InvalidFirstName : Throwable()
class InvalidLastName : Throwable()
class InvalidEmail : Throwable()