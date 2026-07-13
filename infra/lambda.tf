# Event-driven S3 cleanup: when a note's DynamoDB item is removed (TTL
# expiry or delete), this Lambda sweeps the orphaned content body and any
# embedded premium images, then evicts the images from CloudFront.

data "archive_file" "cleanup_lambda" {
  type        = "zip"
  source_dir  = "${path.module}/lambda/cleanup"
  output_path = "${path.module}/build/cleanup.zip"
}

resource "aws_cloudwatch_log_group" "cleanup_lambda" {
  name              = "/aws/lambda/${var.project}-note-cleanup"
  retention_in_days = 14

  tags = {
    Project = var.project
  }
}

data "aws_iam_policy_document" "cleanup_lambda_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "cleanup_lambda" {
  name               = "${var.project}-note-cleanup"
  assume_role_policy = data.aws_iam_policy_document.cleanup_lambda_assume.json
}

data "aws_iam_policy_document" "cleanup_lambda" {
  statement {
    sid       = "Logs"
    actions   = ["logs:CreateLogStream", "logs:PutLogEvents"]
    resources = ["${aws_cloudwatch_log_group.cleanup_lambda.arn}:*"]
  }

  statement {
    sid = "ReadStream"
    actions = [
      "dynamodb:GetRecords",
      "dynamodb:GetShardIterator",
      "dynamodb:DescribeStream",
      "dynamodb:ListStreams",
    ]
    resources = ["${aws_dynamodb_table.notes.arn}/stream/*"]
  }

  statement {
    sid     = "SweepObjects"
    actions = ["s3:GetObject", "s3:DeleteObject"]
    resources = [
      "${aws_s3_bucket.content.arn}/notes/*",
      "${aws_s3_bucket.content.arn}/images/*",
    ]
  }

  statement {
    sid       = "EvictCdn"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = [aws_cloudfront_distribution.site.arn]
  }
}

resource "aws_iam_role_policy" "cleanup_lambda" {
  name   = "${var.project}-note-cleanup"
  role   = aws_iam_role.cleanup_lambda.id
  policy = data.aws_iam_policy_document.cleanup_lambda.json
}

resource "aws_lambda_function" "cleanup" {
  function_name    = "${var.project}-note-cleanup"
  role             = aws_iam_role.cleanup_lambda.arn
  runtime          = "nodejs20.x"
  handler          = "index.handler"
  filename         = data.archive_file.cleanup_lambda.output_path
  source_code_hash = data.archive_file.cleanup_lambda.output_base64sha256
  timeout          = 30
  memory_size      = 128

  environment {
    variables = {
      BUCKET          = aws_s3_bucket.content.bucket
      DISTRIBUTION_ID = aws_cloudfront_distribution.site.id
    }
  }

  depends_on = [aws_cloudwatch_log_group.cleanup_lambda]

  tags = {
    Project = var.project
  }
}

resource "aws_lambda_event_source_mapping" "cleanup" {
  event_source_arn  = aws_dynamodb_table.notes.stream_arn
  function_name     = aws_lambda_function.cleanup.arn
  starting_position = "LATEST"
  batch_size        = 10

  # A poisoned record splits and retries a few times, then is dropped
  # (orphans are harmless; the next takedown or manual sweep can catch up).
  maximum_retry_attempts         = 4
  bisect_batch_on_function_error = true

  filter_criteria {
    filter {
      pattern = jsonencode({ eventName = ["REMOVE"] })
    }
  }
}
