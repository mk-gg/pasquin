# Note metadata. On-demand billing: effectively free at hobby traffic.
# TTL on expiresAt implements the auto-expire feature server-side.
resource "aws_dynamodb_table" "notes" {
  name         = var.notes_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "slug"

  # Feeds the cleanup Lambda: every removed item (TTL expiry or delete)
  # triggers an S3 sweep of the note body and its images. The Lambda only
  # needs the slug, which is the key.
  stream_enabled   = true
  stream_view_type = "KEYS_ONLY"

  attribute {
    name = "slug"
    type = "S"
  }

  ttl {
    attribute_name = "expiresAt"
    enabled        = true
  }

  point_in_time_recovery {
    enabled = false # enable if the data ever matters enough to pay for it
  }

  tags = {
    Project = var.project
  }
}

# Contact messages and abuse reports. Write-heavy, read rarely (moderation);
# on-demand billing keeps it near-free.
resource "aws_dynamodb_table" "submissions" {
  name         = "${var.notes_table_name}-submissions"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = {
    Project = var.project
  }
}

# User accounts (Google sign-in) with their synced note lists.
resource "aws_dynamodb_table" "users" {
  name         = "${var.notes_table_name}-users"
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "id"

  attribute {
    name = "id"
    type = "S"
  }

  tags = {
    Project = var.project
  }
}
