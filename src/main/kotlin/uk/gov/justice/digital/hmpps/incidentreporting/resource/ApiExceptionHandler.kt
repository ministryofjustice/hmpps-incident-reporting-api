package uk.gov.justice.digital.hmpps.incidentreporting.resource

import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.data.mapping.PropertyReferenceException
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.validation.FieldError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UserAuthorisationException
import uk.gov.justice.digital.hmpps.incidentreporting.service.PrisonersNotFoundException
import java.util.UUID
import kotlin.reflect.KClass

@RestControllerAdvice
class ApiExceptionHandler {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
  fun handleMethodNotAllowedException(e: HttpRequestMethodNotSupportedException): ResponseEntity<ErrorResponse> {
    log.info("Method not allowed: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.METHOD_NOT_ALLOWED)
      .body(
        ErrorResponse(
          status = HttpStatus.METHOD_NOT_ALLOWED,
          userMessage = "Method not allowed: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
  fun handleInvalidRequestFormatException(e: HttpMediaTypeNotSupportedException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Request format not supported: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
      .body(
        ErrorResponse(
          status = HttpStatus.UNSUPPORTED_MEDIA_TYPE,
          userMessage = "Validation failure: Request format not supported: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(UserAuthorisationException::class)
  @ResponseStatus(FORBIDDEN)
  fun handleUserAuthorisationException(e: UserAuthorisationException): ResponseEntity<ErrorResponse> {
    log.error("Access denied exception: {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "User authorisation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleNoBodyValidationException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Couldn't read request body: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation failure: Couldn't read request body: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException::class)
  fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
    val type = e.requiredType
    val message = if (type.isEnum) {
      "Parameter ${e.name} must be one of the following ${StringUtils.join(type.enumConstants, ", ")}"
    } else {
      "Parameter ${e.name} must be of type ${type.typeName}"
    }

    log.info("Validation exception: {}", message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation failure: $message",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(FORBIDDEN)
      .body(
        ErrorResponse(
          status = FORBIDDEN,
          userMessage = "Forbidden: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException.NotFound::class)
  fun handleSpringNotFound(e: WebClientResponseException.NotFound): ResponseEntity<ErrorResponse> {
    log.debug("Not found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "Not Found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(NoResourceFoundException::class)
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse> {
    log.debug("No resource found exception caught: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "No resource found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ResponseStatusException::class)
  fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse> {
    log.debug("Response status exception caught: {}", e.message)
    val reason = e.reason ?: "Unknown error"
    return ResponseEntity
      .status(e.statusCode)
      .body(
        ErrorResponse(
          status = e.statusCode.value(),
          userMessage = reason,
          developerMessage = reason,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse> {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleInvalidMethodArgumentException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
    val message = e.allErrors.joinToString(", ") {
      val field = if (it is FieldError) {
        "${it.objectName}.${it.field}"
      } else {
        it.objectName
      }
      "$field: ${it.defaultMessage}"
    }
    log.debug("MethodArgumentNotValidException caught: {}", message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation Failure: $message",
          developerMessage = message,
        ),
      )
  }

  @ExceptionHandler(PropertyReferenceException::class)
  fun handlePropertyReferenceException(e: PropertyReferenceException): ResponseEntity<ErrorResponse> {
    log.debug("PropertyReferenceException caught: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          errorCode = ErrorCode.ValidationFailure,
          userMessage = "Validation Failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ReportNotFoundException::class)
  fun handleReportNotFound(e: ReportNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          errorCode = ErrorCode.ReportNotFound,
          userMessage = "Report not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ReportAlreadyExistsException::class)
  fun handleReportAlreadyExists(e: ReportAlreadyExistsException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          errorCode = ErrorCode.ReportAlreadyExists,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ReportModifiedInDpsException::class)
  fun handleReportModifedInDps(e: ReportModifiedInDpsException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.CONFLICT)
      .body(
        ErrorResponse(
          status = HttpStatus.CONFLICT,
          errorCode = ErrorCode.ReportModifedInDps,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(QuestionsNotFoundException::class)
  fun handleQuestionsNotFound(e: QuestionsNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          errorCode = ErrorCode.ReportNotFound,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ObjectAtIndexNotFoundException::class)
  fun handleObjectAtIndexNotFound(e: ObjectAtIndexNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(SubjectAccessRequestNoReports::class)
  fun handleSarNoReports(e: SubjectAccessRequestNoReports): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.NO_CONTENT)
      .body(
        ErrorResponse(
          status = HttpStatus.NO_CONTENT,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(PrisonersNotFoundException::class)
  fun handlePrisonerSearchNotFound(e: PrisonersNotFoundException): ResponseEntity<ErrorResponse> {
    log.debug(e.message)
    return ResponseEntity
      .status(HttpStatus.NOT_FOUND)
      .body(
        ErrorResponse(
          status = HttpStatus.NOT_FOUND,
          userMessage = "${e.message}",
          developerMessage = e.message,
        ),
      )
  }
}

class ReportNotFoundException(
  description: String,
) : Exception("There is no report found: $description") {
  constructor(id: UUID) : this(id.toString())
}

class ReportAlreadyExistsException(
  description: String,
) : Exception("Report already exists: $description") {
  constructor(id: UUID) : this(id.toString())
}

class ReportModifiedInDpsException(
  description: String,
) : Exception("Report last modified in DPS: $description") {
  constructor(id: UUID) : this(id.toString())
}

class ObjectAtIndexNotFoundException(
  type: KClass<*>,
  index: Int,
) : Exception("Object ${type.simpleName} at index $index not found")

class QuestionsNotFoundException(
  questionCodes: Set<String>,
) : Exception("Questions codes not found: ${questionCodes.sorted().joinToString()}")

class SubjectAccessRequestNoReports : Exception("No reports found for given SAR filters")
