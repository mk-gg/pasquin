package dev.mkgg.notes.common;

/** Thrown when a mutation is attempted with a missing or incorrect edit key. */
public class InvalidEditKeyException extends RuntimeException {

  public InvalidEditKeyException() {
    super("Invalid edit key");
  }
}
