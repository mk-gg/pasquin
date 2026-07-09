output "aws_region" {
  value = var.aws_region
}

output "notes_table_name" {
  value = aws_dynamodb_table.notes.name
}

output "content_bucket" {
  value = aws_s3_bucket.content.bucket
}

output "site_bucket" {
  value = aws_s3_bucket.site.bucket
}

output "cloudfront_domain" {
  description = "CloudFront default frontend URL"
  value       = "https://${aws_cloudfront_distribution.site.domain_name}"
}

output "site_url" {
  description = "Custom-domain frontend URL"
  value       = "https://${var.domain_name}"
}

# CNAME to add at your DNS host (Porkbun) so ACM can validate the certificate.
output "acm_validation_record" {
  description = "Add this CNAME at Porkbun to validate the TLS certificate"
  value = {
    for dvo in aws_acm_certificate.site.domain_validation_options :
    dvo.domain_name => {
      name  = dvo.resource_record_name
      type  = dvo.resource_record_type
      value = dvo.resource_record_value
    }
  }
}

output "cloudfront_distribution_id" {
  description = "Use for cache invalidations after a frontend deploy"
  value       = aws_cloudfront_distribution.site.id
}

output "apprunner_service_arn" {
  description = "Use to pause/resume the backend to save cost between demos"
  value       = aws_apprunner_service.backend.arn
}

output "github_actions_role_arn" {
  description = "Set as the AWS_DEPLOY_ROLE_ARN GitHub Actions variable"
  value       = aws_iam_role.github_actions.arn
}

output "backend_policy_arn" {
  description = "Attach to the role/user running Spring Boot"
  value       = aws_iam_policy.backend.arn
}

output "ecr_repository_url" {
  description = "Push the backend image here (docker tag/push :latest)"
  value       = aws_ecr_repository.backend.repository_url
}

output "backend_url" {
  description = "Public HTTPS URL of the App Runner backend"
  value       = "https://${aws_apprunner_service.backend.service_url}"
}
