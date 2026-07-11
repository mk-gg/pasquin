package dev.mkgg.notes.image;

import dev.mkgg.notes.auth.JwtService;
import dev.mkgg.notes.common.InvalidTokenException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Premium image uploads; the raw image is the request body. */
@RestController
@RequestMapping("/api/images")
public class ImageController {

  private static final String BEARER = "Bearer ";

  private final ImageService imageService;
  private final JwtService jwtService;

  public ImageController(ImageService imageService, JwtService jwtService) {
    this.imageService = imageService;
    this.jwtService = jwtService;
  }

  @PostMapping(consumes = "image/*")
  public UploadResponse upload(
      @RequestHeader(value = "Authorization", required = false) String auth,
      @RequestBody byte[] body) {
    return new UploadResponse(imageService.upload(userId(auth), body));
  }

  /**
   * Where the uploaded image is served from.
   *
   * @param url public URL to embed in the note document
   */
  public record UploadResponse(String url) {}

  private String userId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith(BEARER)) {
      throw new InvalidTokenException();
    }
    return jwtService.verify(authHeader.substring(BEARER.length()));
  }
}
