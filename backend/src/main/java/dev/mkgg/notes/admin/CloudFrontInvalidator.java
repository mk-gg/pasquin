package dev.mkgg.notes.admin;

import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;

/** Evicts objects from the site's CloudFront distribution. */
@Component
@Profile("aws")
public class CloudFrontInvalidator implements CdnInvalidator {

  private static final Logger log = LoggerFactory.getLogger(CloudFrontInvalidator.class);

  private final CloudFrontClient cloudFront;
  private final String distributionId;

  public CloudFrontInvalidator(CloudFrontClient cloudFront, NotesProperties properties) {
    this.cloudFront = cloudFront;
    this.distributionId = properties.images().distributionId();
  }

  @Override
  public void invalidate(List<String> paths) {
    if (distributionId.isBlank() || paths.isEmpty()) {
      return;
    }
    cloudFront.createInvalidation(
        request ->
            request
                .distributionId(distributionId)
                .invalidationBatch(
                    batch ->
                        batch
                            .callerReference("takedown-" + System.currentTimeMillis())
                            .paths(p -> p.items(paths).quantity(paths.size()))));
    log.info("Invalidated {} CDN path(s)", paths.size());
  }
}
