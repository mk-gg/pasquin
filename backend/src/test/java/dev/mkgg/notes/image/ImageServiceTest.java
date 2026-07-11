package dev.mkgg.notes.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.mkgg.notes.auth.User;
import dev.mkgg.notes.auth.storage.InMemoryUserRepository;
import dev.mkgg.notes.config.NotesProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ImageServiceTest {

  private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2};
  private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 9};
  private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 1};
  private static final byte[] NOT_AN_IMAGE = {'M', 'Z', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  private InMemoryUserRepository users;
  private ImageService service;
  private String storedKey;
  private String storedContentType;

  private static NotesProperties props(long maxImage, long maxTotal) {
    return new NotesProperties(
        new NotesProperties.Cors(List.of()),
        new NotesProperties.Aws("r", "t", "b", "s", "u"),
        new NotesProperties.RateLimit(1, 1, 1, 1, 1),
        new NotesProperties.Auth("client-id", "secret-that-is-at-least-32-bytes-long!!"),
        new NotesProperties.Limits(5_242_880),
        new NotesProperties.Mail(false, "noreply@example.com", "owner@example.com"),
        new NotesProperties.Polar(false, "https://sandbox-api.polar.sh", "", "", "", ""),
        new NotesProperties.Images(maxImage, maxTotal, "https://cdn.example"));
  }

  @BeforeEach
  void setUp() {
    users = new InMemoryUserRepository();
    users.save(new User("prem", "p@example.com", "Prem", true, 0, List.of()));
    users.save(new User("free", "f@example.com", "Free", false, 0, List.of()));
    ImageStore store =
        (key, bytes, contentType) -> {
          storedKey = key;
          storedContentType = contentType;
          return "https://cdn.example/" + key;
        };
    service = new ImageService(store, users, props(1024, 2048));
  }

  @Test
  void premiumUserCanUploadPng() {
    String url = service.upload("prem", PNG);

    assertThat(url).startsWith("https://cdn.example/images/prem/").endsWith(".png");
    assertThat(storedKey).startsWith("images/prem/");
    assertThat(storedContentType).isEqualTo("image/png");
    assertThat(users.findById("prem").orElseThrow().imageBytes()).isEqualTo(PNG.length);
  }

  @Test
  void detectsJpegAndWebp() {
    service.upload("prem", JPEG);
    assertThat(storedContentType).isEqualTo("image/jpeg");

    service.upload("prem", WEBP);
    assertThat(storedContentType).isEqualTo("image/webp");
  }

  @Test
  void freeUserIsRejected() {
    assertThatThrownBy(() -> service.upload("free", PNG))
        .isInstanceOf(PremiumRequiredException.class);
  }

  @Test
  void nonImageBytesAreRejectedRegardlessOfClaims() {
    assertThatThrownBy(() -> service.upload("prem", NOT_AN_IMAGE))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported image format");
  }

  @Test
  void oversizedImageIsRejected() {
    byte[] big = new byte[2048];
    System.arraycopy(PNG, 0, big, 0, PNG.length);

    assertThatThrownBy(() -> service.upload("prem", big))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maximum size");
  }

  @Test
  void quotaIsEnforcedAcrossUploads() {
    byte[] halfQuota = new byte[1024];
    System.arraycopy(PNG, 0, halfQuota, 0, PNG.length);

    service.upload("prem", halfQuota);
    service.upload("prem", halfQuota);

    assertThatThrownBy(() -> service.upload("prem", PNG))
        .isInstanceOf(QuotaExceededException.class);
    assertThat(users.findById("prem").orElseThrow().imageBytes()).isEqualTo(2048);
  }
}
