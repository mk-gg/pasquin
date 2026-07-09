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
