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
