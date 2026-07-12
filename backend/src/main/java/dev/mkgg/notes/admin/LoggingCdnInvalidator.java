package dev.mkgg.notes.admin;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/** Log-only stand-in for local development. */
@Component
@Profile("!aws")
public class LoggingCdnInvalidator implements CdnInvalidator {

  private static final Logger log = LoggerFactory.getLogger(LoggingCdnInvalidator.class);

  @Override
  public void invalidate(List<String> paths) {
    log.info("Would invalidate CDN paths: {}", paths);
  }
}
