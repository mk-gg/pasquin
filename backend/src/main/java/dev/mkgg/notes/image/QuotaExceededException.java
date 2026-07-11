package dev.mkgg.notes.image;

/** The upload would push the user past their lifetime image-storage quota. */
public class QuotaExceededException extends RuntimeException {

  public QuotaExceededException() {
    super("Image storage quota exceeded");
  }
}
