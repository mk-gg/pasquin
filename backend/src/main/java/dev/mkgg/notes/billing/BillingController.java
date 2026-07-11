package dev.mkgg.notes.billing;

import dev.mkgg.notes.auth.JwtService;
import dev.mkgg.notes.common.InvalidTokenException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Premium checkout for signed-in users. */
@RestController
@RequestMapping("/api/billing")
public class BillingController {

  private static final String BEARER = "Bearer ";

  private final BillingService billingService;
  private final JwtService jwtService;

  public BillingController(BillingService billingService, JwtService jwtService) {
    this.billingService = billingService;
    this.jwtService = jwtService;
  }

  /** Creates a Polar checkout session; the frontend redirects the user to the returned URL. */
  @PostMapping("/checkout")
  public CheckoutResponse checkout(
      @RequestHeader(value = "Authorization", required = false) String auth) {
    return new CheckoutResponse(billingService.createCheckout(userId(auth)));
  }

  /**
   * The hosted checkout URL to redirect the customer to.
   *
   * @param url Polar's checkout session URL
   */
  public record CheckoutResponse(String url) {}

  private String userId(String authHeader) {
    if (authHeader == null || !authHeader.startsWith(BEARER)) {
      throw new InvalidTokenException();
    }
    return jwtService.verify(authHeader.substring(BEARER.length()));
  }
}
