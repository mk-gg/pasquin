# Private bucket for note content (JSON documents). Only the backend
# talks to it; SSE keeps the "stored with encryption" promise honest.
resource "aws_s3_bucket" "content" {
  bucket = "${var.project}-content"

  tags = {
    Project = var.project
  }
}

resource "aws_s3_bucket_public_access_block" "content" {
  bucket = aws_s3_bucket.content.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "content" {
  bucket = aws_s3_bucket.content.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

# Note: when DynamoDB TTL deletes expired metadata, the matching S3 body is
# orphaned (harmless, pennies). A DynamoDB Streams -> Lambda cleanup can be
# added later; a blanket lifecycle expiration would delete permanent notes.
