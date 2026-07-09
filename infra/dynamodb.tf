# Note metadata. On-demand billing: effectively free at hobby traffic.
# TTL on expiresAt implements the auto-expire feature server-side.
resource "aws_dynamodb_table" "notes" {
  name         = var.notes_table_name
  billing_mode = "PAY_PER_REQUEST"
  hash_key     = "slug"

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
