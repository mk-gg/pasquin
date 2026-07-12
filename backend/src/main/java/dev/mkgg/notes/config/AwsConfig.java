package dev.mkgg.notes.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sesv2.SesV2Client;

/**
 * AWS SDK clients, active only under the {@code aws} profile. Credentials are resolved through the
 * default provider chain (environment variables, shared config, or the instance role).
 */
@Configuration
@Profile("aws")
public class AwsConfig {

  @Bean
  public DynamoDbClient dynamoDbClient(NotesProperties properties) {
    return DynamoDbClient.builder().region(Region.of(properties.aws().region())).build();
  }

  @Bean
  public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
    return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
  }

  @Bean
  public S3Client s3Client(NotesProperties properties) {
    return S3Client.builder().region(Region.of(properties.aws().region())).build();
  }

  @Bean
  public SesV2Client sesV2Client(NotesProperties properties) {
    return SesV2Client.builder().region(Region.of(properties.aws().region())).build();
  }

  @Bean
  public CloudFrontClient cloudFrontClient() {
    // CloudFront is a global service; its control-plane endpoint lives in AWS_GLOBAL.
    return CloudFrontClient.builder().region(Region.AWS_GLOBAL).build();
  }
}
