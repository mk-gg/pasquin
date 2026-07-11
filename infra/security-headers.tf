# Security headers for the delivered site: HSTS, nosniff, frame-deny, a strict
# referrer policy, and an enforced Content-Security-Policy.
#
# The CSP was validated in report-only mode against the live site (clean
# console after fixing the Astro view-transition data: script) before being
# enforced here. The enforced policy must live in security_headers_config;
# CloudFront rejects "Content-Security-Policy" as a custom header. To debug a
# future violation without breaking users, move the policy back to a
# custom_headers_config item named "Content-Security-Policy-Report-Only"
# (report-only has no slot in security_headers_config).

locals {
  # connect-src must list every origin the browser makes fetch/XHR/WebSocket
  # calls to: the app itself, the App Runner API, and Google sign-in. This is
  # the directive that stops an injected script from exfiltrating the session
  # token to an attacker host. The API is matched by the App Runner wildcard
  # rather than the exact service URL, because referencing the service here
  # would create a dependency cycle (App Runner already depends on this
  # distribution for its CORS origins).
  #
  # script-src keeps 'unsafe-inline': Astro emits inline scripts (view
  # transitions, theme init) on every page, and hashing them is brittle across
  # builds on a static CDN. connect-src + frame-ancestors remain the meaningful
  # protections; revisit if Astro's built-in CSP hashing is adopted later.
  csp = join(" ", [
    "default-src 'self';",
    "base-uri 'self';",
    "object-src 'none';",
    "frame-ancestors 'none';",
    "form-action 'self';",
    # 'unsafe-inline': Astro emits inline scripts on every page. data:: Astro's
    # <ClientRouter /> re-executes scripts as data: URIs during view transitions.
    # Both are needed for the app to run; because 'unsafe-inline' already permits
    # inline script, adding data: does not widen the attack surface further --
    # connect-src and frame-ancestors remain the meaningful protections.
    "script-src 'self' 'unsafe-inline' data: https://accounts.google.com/gsi/client;",
    "style-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/style https://cdn.jsdelivr.net;",
    "img-src 'self' data: https:;",
    "font-src 'self' https://cdn.jsdelivr.net;",
    "frame-src https://accounts.google.com;",
    "connect-src 'self' https://accounts.google.com https://*.awsapprunner.com;",
    "upgrade-insecure-requests",
  ])
}

resource "aws_cloudfront_response_headers_policy" "site" {
  name = "${var.project}-security-headers"

  security_headers_config {
    strict_transport_security {
      access_control_max_age_sec = 31536000 # 1 year
      include_subdomains         = false    # leaf host; no subdomains to cover
      preload                    = false    # avoid the hard-to-reverse preload commitment
      override                   = true
    }
    content_type_options {
      override = true # X-Content-Type-Options: nosniff
    }
    frame_options {
      frame_option = "DENY"
      override     = true
    }
    referrer_policy {
      referrer_policy = "strict-origin-when-cross-origin"
      override        = true
    }
    # Content-Security-Policy is a recognized security header and must live here
    # rather than in custom_headers_config, which CloudFront rejects for it.
    content_security_policy {
      content_security_policy = local.csp
      override                = true
    }
  }
}
