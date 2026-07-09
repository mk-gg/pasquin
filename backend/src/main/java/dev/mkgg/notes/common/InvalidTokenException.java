package dev.mkgg.notes.common;

/** Thrown when a Google ID token or our own session token is missing, invalid, or expired. */
public class InvalidTokenException extends RuntimeException {

  public InvalidTokenException() {
    super("Invalid or expired token");
  }
}
