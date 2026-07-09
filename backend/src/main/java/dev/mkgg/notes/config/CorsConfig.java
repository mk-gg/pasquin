package dev.mkgg.notes.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Allows the Astro frontend origins to call the API during development and in production. */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  private final NotesProperties properties;

  public CorsConfig(NotesProperties properties) {
    this.properties = properties;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/api/**")
        .allowedOrigins(properties.cors().allowedOrigins().toArray(String[]::new))
        .allowedMethods("GET", "POST", "PUT", "DELETE")
        .maxAge(3600);
  }
}
