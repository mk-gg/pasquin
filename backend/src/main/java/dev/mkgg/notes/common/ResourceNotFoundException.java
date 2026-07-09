package dev.mkgg.notes.common;

/** Thrown when a requested resource does not exist or has expired. */
public class ResourceNotFoundException extends RuntimeException {

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
