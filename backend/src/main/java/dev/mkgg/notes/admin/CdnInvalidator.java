package dev.mkgg.notes.admin;

import java.util.List;

/** Port for evicting taken-down objects from the CDN edge caches. */
public interface CdnInvalidator {

  /**
   * Invalidates the given URL paths (each starting with {@code /}) so cached copies stop being
   * served.
   */
  void invalidate(List<String> paths);
}
