package dev.mkgg.notes.billing;

/** Checkout could not be created (billing disabled or the Polar API failed). */
public class BillingUnavailableException extends RuntimeException {

  public BillingUnavailableException(String message) {
    super(message);
  }

  public BillingUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
