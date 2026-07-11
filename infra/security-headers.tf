# Security headers for the delivered site: HSTS/nosniff/frame-deny/referrer are
# enforced immediately (they cannot break the app), while the Content-Security-
# Policy ships in REPORT-ONLY mode so violations can be observed in the browser
# console against the live site before it is enforced.
#
# To enforce the CSP once the console is clean: rename the custom header below
# from "Content-Security-Policy-Report-Only" to "Content-Security-Policy".

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
    "script-src 'self' 'unsafe-inline' https://accounts.google.com/gsi/client;",
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
  }

  custom_headers_config {
    items {
      header   = "Content-Security-Policy-Report-Only"
      value    = local.csp
      override = true
    }
  }
}
