package dev.mkgg.notes.common;

/** Thrown when the password supplied for a protected note does not match. */
public class InvalidPasswordException extends RuntimeException {

  public InvalidPasswordException() {
    super("Invalid password");
  }
}
