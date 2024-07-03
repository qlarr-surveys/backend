package com.frankie.backend.error

import com.amazonaws.AmazonServiceException
import com.amazonaws.SdkClientException
import com.frankie.backend.exceptions.*
import com.frankie.expressionmanager.usecase.SurveyDesignWithErrorException
import io.jsonwebtoken.JwtException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ControllerExceptionHandler {

    @ExceptionHandler
    fun handleException(exception: JwtException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(exception.message ?: "", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: SdkClientException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "SDK Client connection error/unable to parse the response from service",
                        exception.javaClass.simpleName
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler
    fun handleException(exception: AmazonServiceException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Error response returned by an Amazon web service, the service was not able to process the request",
                        exception.javaClass.simpleName
                ),
                HttpStatus.INTERNAL_SERVER_ERROR
        )
    }

    @ExceptionHandler
    fun resourceMissMatch(exception: ResourceNotFoundException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(ErrorMessage("File not found", exception.javaClass.simpleName), HttpStatus.NOT_FOUND)
    }


    @ExceptionHandler(UserNotFoundException::class)
    fun handleException(exception: UserNotFoundException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(ErrorMessage("User not found", exception.javaClass.simpleName), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(EmptyRolesException::class)
    fun handleException(exception: EmptyRolesException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(ErrorMessage("Cannot Set a user with empty roles", exception.javaClass.simpleName), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(EditOwnUserException::class)
    fun handleException(exception: EditOwnUserException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(ErrorMessage("Cannot Edit own user from here, do it from profile", exception.javaClass.simpleName), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler
    fun handleException(exception: PermissionNotFoundException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Permission not found", exception.javaClass.simpleName),
                HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler
    fun handleException(exception: PermissionAlreadyExists): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Permission already Exists", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongEmailOrPasswordException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Wrong email or password", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidInputException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid input " + exception.problemDescription,
                        exception.javaClass.simpleName
                ), HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: MethodArgumentTypeMismatchException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid input with field:${exception.name}... ${exception.cause?.message}",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: MissingServletRequestPartException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(exception.message ?: "", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: MissingServletRequestParameterException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(exception.message, exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(exception.message ?: "", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: HttpMessageNotReadableException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(ErrorMessage("Invalid input", exception.javaClass.simpleName), HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler
    fun handleException(exception: DuplicateEmailException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Invalid input, duplicate email", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidFirstName): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Invalid first name", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidEmail): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Invalid Email", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidLastName): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Invalid last name", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun duplicateSurvey(exception: DuplicateSurveyException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Invalid input, duplicate survey", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidSurveyDates): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid input, survey end date must always be before start date",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: IncompleteResponse): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Incomplete Response, Responses must be at End to be synced",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidResponse): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid Response, Responses must be at End to be synced",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidSurveyName): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid Survey Name",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyNotStartedException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "This survey has not started yet, it starts on ${exception.startDate}",
                        exception.javaClass.simpleName
                ),
                HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyExpiredException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid input, survey end date must always be before start date",
                        exception.javaClass.simpleName
                ),
                HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyQuotaExceeded): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Invalid input, survey end date must always be before start date",
                        exception.javaClass.simpleName
                ),
                HttpStatus.NOT_FOUND
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyNotFoundException): ResponseEntity<ErrorMessage> {
        return ResponseEntity(ErrorMessage("Survey not found", exception.javaClass.simpleName), HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler
    fun handleException(exception: SurveyIsActiveException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Survey has active status", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyIsClosedException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Should not modify a closed Survey", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyIsNotActiveException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This survey should be active", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: PreviousNotAllowed): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This Navigation Direction is not allowed", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: JumpNotAllowed): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This survey should be active", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: ResumeNotAllowed): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This survey should be active", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: AuthorizationException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("unauthorized", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongCredentialsException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Wrong email or password", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: SignupNotAllowed): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Wrong email or password", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: GoogleAuthError): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Could not process google credentials", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: SurveyDesignWithErrorException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Survey Design with Error", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: DesignException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Major issue, contact design", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: InvalidDesignException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Major issue, contact design", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: ComponentDeletedException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Componet was deleted: ${exception.deletedCode}", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: DesignOutOfSyncException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "can only update from latest subVersion: ${exception.subVersion}",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongConfirmationTokenException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "This confirmation token is either wrong or has been already used",
                        exception.javaClass.simpleName
                ),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongResetTokenException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "This confirmation token is either wrong or has been already used",
                        exception.javaClass.simpleName
                ),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongEmailChangePinException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "This ",
                        exception.javaClass.simpleName
                ),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: ExpiredConfirmationTokenException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This confirmation token has expired", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: ExpiredResetTokenException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This reset token has expired", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: UsedConfirmationTokenException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("This confirmation token has been already used", exception.javaClass.simpleName),
                HttpStatus.UNAUTHORIZED
        )
    }

    @ExceptionHandler
    fun handleException(exception: ResponseNotFoundException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Response not found", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: ResponseAlreadySyncedException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Response already synced", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }


    @ExceptionHandler
    fun handleException(exception: WrongColumnException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Wrong column name: ${exception.columnName}", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: UnrecognizedZoneException): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Unrecognized zone: ${exception.zone}", exception.javaClass.simpleName),
                HttpStatus.BAD_REQUEST
        )
    }

    @ExceptionHandler
    fun handleException(exception: WrongValueType): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage(
                        "Wrong value type for ${exception.columnName}" +
                                ", expected ${exception.expectedClassName} found ${exception.actualClassName}",
                        exception.javaClass.simpleName
                ),
                HttpStatus.BAD_REQUEST
        )
    }
}

@ControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class DefaultExceptionHandler {
    @ExceptionHandler
    fun handleException(exception: Exception): ResponseEntity<ErrorMessage> {
        exception.printStackTrace()
        return ResponseEntity(
                ErrorMessage("Unexpected error", exception.javaClass.simpleName),
                HttpStatus.INTERNAL_SERVER_ERROR
        )
    }
}
