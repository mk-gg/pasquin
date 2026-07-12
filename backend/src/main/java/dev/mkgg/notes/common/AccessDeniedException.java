package dev.mkgg.notes.common;

/** The caller is authenticated but not allowed to perform this action. */
public class AccessDeniedException extends RuntimeException {

  public AccessDeniedException() {
    super("Access denied");
  }
}
