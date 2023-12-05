package uk.gov.justice.digital.hmpps.incidentreporting.config

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.ValidationException
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class ApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: ValidationException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
          errorCode = ErrorCode.ValidationFailure,
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

  @ExceptionHandler(HttpMessageNotReadableException::class)
  fun handleNoBodyValidationException(e: HttpMessageNotReadableException): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: Couldn't read request body: {}", e.message)
    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Validation failure: Couldn't read request body: ${e.message}",
          developerMessage = e.message,
          errorCode = ErrorCode.ValidationFailure,
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
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Validation failure: $message",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> {
    log.debug("Forbidden (403) returned with message {}", e.message)
    return ResponseEntity
      .status(HttpStatus.FORBIDDEN)
      .body(
        ErrorResponse(
          status = HttpStatus.FORBIDDEN,
          userMessage = "Forbidden: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(WebClientResponseException.NotFound::class)
  fun handleSpringNotFound(e: WebClientResponseException.NotFound): ResponseEntity<ErrorResponse?>? {
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
  fun handleNoResourceFoundException(e: NoResourceFoundException): ResponseEntity<ErrorResponse?>? {
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
  fun handleResponseStatusException(e: ResponseStatusException): ResponseEntity<ErrorResponse?>? {
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

  @ExceptionHandler(MethodArgumentNotValidException::class)
  fun handleInvalidMethodArgumentException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse>? {
    log.debug("MethodArgumentNotValidException exception caught: {}", e.message)

    return ResponseEntity
      .status(HttpStatus.BAD_REQUEST)
      .body(
        ErrorResponse(
          status = HttpStatus.BAD_REQUEST,
          userMessage = "Validation Failure: ${e.message}",
          developerMessage = e.message,
          errorCode = ErrorCode.ValidationFailure,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
    log.error("Unexpected exception", e)
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .body(
        ErrorResponse(
          status = HttpStatus.INTERNAL_SERVER_ERROR,
          userMessage = "Unexpected error: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

/**
 * Codes that can be used by api clients to uniquely discriminate between error types,
 * instead of relying on non-constant text descriptions of HTTP status codes.
 *
 * NB: Once defined, the values must not be changed
 */
enum class ErrorCode(val errorCode: Int) {
  ValidationFailure(101),
}

@Schema(description = "Error response")
data class ErrorResponse(
  @Schema(description = "HTTP status code", example = "500", required = true)
  val status: Int,
  @Schema(description = "User message for the error", example = "No non-association found for ID `324234`", required = true)
  val userMessage: String,
  @Schema(description = "More detailed error message", example = "[Details, sometimes a stack trace]", required = true)
  val developerMessage: String,
  @Schema(description = "When present, uniquely identifies the type of error making it easier for clients to discriminate without relying on error description or HTTP status code; see `uk.gov.justice.digital.hmpps.hmppsnonassociationsapi.config.ErrorCode` enumeration in hmpps-non-associations-api", example = "101", required = false)
  val errorCode: Int? = null,
  @Schema(description = "More information about the error", example = "[Rarely used, error-specific]", required = false)
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    userMessage: String,
    developerMessage: String? = null,
    errorCode: ErrorCode? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), userMessage, developerMessage ?: userMessage, errorCode?.errorCode, moreInfo)
}
