# TLS certificate for the custom frontend domain. CloudFront only accepts
# certificates from us-east-1, hence the aliased provider.
resource "aws_acm_certificate" "site" {
  provider          = aws.us_east_1
  domain_name       = var.domain_name
  validation_method = "DNS"

  lifecycle {
    create_before_destroy = true
  }

  tags = {
    Project = var.project
  }
}

# Blocks until ACM confirms the DNS validation record (added manually at the
# domain's DNS host) proves ownership. The record values come from
# `terraform output acm_validation_record`.
resource "aws_acm_certificate_validation" "site" {
  provider        = aws.us_east_1
  certificate_arn = aws_acm_certificate.site.arn
}
