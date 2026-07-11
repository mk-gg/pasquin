package dev.mkgg.notes.billing;

/** Port for the Polar API; only checkout-session creation is needed. */
public interface PolarClient {

  /**
   * Creates a checkout session for the premium product and returns the hosted checkout URL the
   * customer should be redirected to.
   *
   * @param userId our user id, passed as Polar's {@code external_customer_id} so the webhook can
   *     map the order back to the account
   * @param email prefills the checkout's email field
   */
  String createCheckoutUrl(String userId, String email);
}
