# Least-privilege policy for the backend runtime: CRUD on the notes table
# and object access under notes/ in the content bucket.
data "aws_iam_policy_document" "backend" {
  statement {
    sid = "NotesTable"
    actions = [
      "dynamodb:GetItem",
      "dynamodb:PutItem",
      "dynamodb:DeleteItem",
    ]
    resources = [aws_dynamodb_table.notes.arn]
  }

  statement {
    sid       = "SubmissionsTable"
    actions   = ["dynamodb:PutItem"]
    resources = [aws_dynamodb_table.submissions.arn]
  }

  statement {
    sid = "NotesContent"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:DeleteObject",
    ]
    resources = ["${aws_s3_bucket.content.arn}/notes/*"]
  }
}

resource "aws_iam_policy" "backend" {
  name   = "${var.project}-backend"
  policy = data.aws_iam_policy_document.backend.json
}

# Attach this policy to whatever runs Spring Boot:
#  - EC2/Lightsail: an instance role (preferred, no long-lived keys)
#  - App Runner/ECS: the task role
# For a quick start you can create an IAM user, attach the policy, and issue
# an access key manually in the console (keys kept out of Terraform state).
