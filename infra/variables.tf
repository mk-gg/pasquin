variable "aws_region" {
  description = "Primary AWS region for the backend resources"
  type        = string
  default     = "us-east-1"
}

variable "project" {
  description = "Name prefix for all resources"
  type        = string
  default     = "pasquin"
}

variable "notes_table_name" {
  description = "DynamoDB table holding note metadata"
  type        = string
  default     = "notes"
}

variable "github_repo" {
  description = "GitHub repo (owner/name) allowed to assume the deploy role via OIDC"
  type        = string
  default     = "mk-gg/pasquin"
}

variable "domain_name" {
  description = "Custom domain for the frontend (CloudFront alias)"
  type        = string
  default     = "pasquin.mkgg.dev"
}

variable "mail_domain" {
  description = "Domain verified in SES for sending submission notifications"
  type        = string
  default     = "mkgg.dev"
}

variable "mail_from" {
  description = "Sender address for submission notifications (any address at mail_domain)"
  type        = string
  default     = "noreply@mkgg.dev"
}

variable "mail_to" {
  description = "Recipient of submission notifications (forwarded by Porkbun to the owner's inbox)"
  type        = string
  default     = "hello@mkgg.dev"
}

variable "google_client_id" {
  description = "Google OAuth client id (public; the audience for ID tokens)"
  type        = string
  default     = "371854982599-q0uu06bo4mvcq0cba2r5b42eso80jg9j.apps.googleusercontent.com"
}
