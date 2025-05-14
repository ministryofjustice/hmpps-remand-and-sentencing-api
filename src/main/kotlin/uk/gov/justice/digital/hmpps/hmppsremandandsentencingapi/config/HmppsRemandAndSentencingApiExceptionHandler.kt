package uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.config

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ChargeAlreadySentencedException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtAppearanceException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.ImmutableCourtCaseException
import uk.gov.justice.digital.hmpps.hmppsremandandsentencingapi.error.OrphanedChargeException

@RestControllerAdvice
class HmppsRemandAndSentencingApiExceptionHandler {
  @ExceptionHandler(ValidationException::class)
  fun handleValidationException(e: Exception): ResponseEntity<ErrorResponse> {
    log.info("Validation exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Validation failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(AccessDeniedException::class)
  fun handleAccessDeniedException(e: AccessDeniedException): ResponseEntity<ErrorResponse> = ResponseEntity
    .status(FORBIDDEN)
    .body(
      ErrorResponse(
        status = FORBIDDEN,
        userMessage = "forbidden: ${e.message}",
        developerMessage = e.message,
      ),
    )

  @ExceptionHandler(EntityNotFoundException::class)
  fun handleEntityNotFoundException(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
    log.info("Entity not found exception: ${e.message}", e)
    return ResponseEntity
      .status(NOT_FOUND)
      .body(
        ErrorResponse(
          status = NOT_FOUND,
          userMessage = "not found: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ImmutableCourtAppearanceException::class)
  fun handleImmutableCourtAppearanceException(e: ImmutableCourtAppearanceException): ResponseEntity<ErrorResponse> {
    log.info("Immutable court appearance exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Immutable court appearance failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ImmutableCourtCaseException::class)
  fun handleImmutableCourtCaseException(e: ImmutableCourtCaseException): ResponseEntity<ErrorResponse> {
    log.info("Immutable court case exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Immutable court case failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(MissingServletRequestParameterException::class)
  fun handleMissingServletRequestParameterException(e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
    log.info("Missing servlet request parameter exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Missing servlet request parameter failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(OrphanedChargeException::class)
  fun handleIOrphanedChargeException(e: OrphanedChargeException): ResponseEntity<ErrorResponse> {
    log.info("Orphaned charge exception: {}", e.message)
    return ResponseEntity
      .status(BAD_REQUEST)
      .body(
        ErrorResponse(
          status = BAD_REQUEST,
          userMessage = "Orphaned charge failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(ChargeAlreadySentencedException::class)
  fun handleChargeAlreadySentencedException(e: ChargeAlreadySentencedException): ResponseEntity<ErrorResponse> {
    log.info("Charge already sentenced exception: ${e.message}", e)
    return ResponseEntity
      .status(UNPROCESSABLE_ENTITY)
      .body(
        ErrorResponse(
          status = UNPROCESSABLE_ENTITY,
          userMessage = "charge already sentenced failure: ${e.message}",
          developerMessage = e.message,
        ),
      )
  }

  @ExceptionHandler(java.lang.Exception::class)
  fun handleException(e: java.lang.Exception): ResponseEntity<ErrorResponse?>? {
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

data class ErrorResponse(
  val status: Int,
  val errorCode: Int? = null,
  val userMessage: String? = null,
  val developerMessage: String? = null,
  val moreInfo: String? = null,
) {
  constructor(
    status: HttpStatus,
    errorCode: Int? = null,
    userMessage: String? = null,
    developerMessage: String? = null,
    moreInfo: String? = null,
  ) :
    this(status.value(), errorCode, userMessage, developerMessage, moreInfo)
}
