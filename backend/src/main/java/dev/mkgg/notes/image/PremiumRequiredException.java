package dev.mkgg.notes.image;

/** The caller is signed in but has not purchased premium. */
public class PremiumRequiredException extends RuntimeException {

  public PremiumRequiredException() {
    super("Image uploads require premium");
  }
}
