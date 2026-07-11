package dev.mkgg.notes.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain exceptions to RFC 9457 problem-detail responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleNotFound(ResourceNotFoundException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(InvalidPasswordException.class)
  public ProblemDetail handleInvalidPassword(InvalidPasswordException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(InvalidEditKeyException.class)
  public ProblemDetail handleInvalidEditKey(InvalidEditKeyException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(InvalidTokenException.class)
  public ProblemDetail handleInvalidToken(InvalidTokenException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(dev.mkgg.notes.image.PremiumRequiredException.class)
  public ProblemDetail handlePremiumRequired(dev.mkgg.notes.image.PremiumRequiredException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler(dev.mkgg.notes.image.QuotaExceededException.class)
  public ProblemDetail handleQuotaExceeded(dev.mkgg.notes.image.QuotaExceededException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
  }

  @ExceptionHandler(dev.mkgg.notes.billing.BillingUnavailableException.class)
  public ProblemDetail handleBillingUnavailable(
      dev.mkgg.notes.billing.BillingUnavailableException e) {
    return ProblemDetail.forStatusAndDetail(
        HttpStatus.SERVICE_UNAVAILABLE, "Checkout is unavailable right now. Please try again.");
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
    return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
  }
}
