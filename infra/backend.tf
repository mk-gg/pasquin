# Backend runtime: App Runner pulls the container image from ECR. The image
# must be pushed before `terraform apply` creates the service (see README).

# Secret for signing user session tokens. Generated once and kept in state
# (local, gitignored) — never appears in committed files.
resource "random_password" "jwt_secret" {
  length  = 48
  special = false
}

resource "aws_ecr_repository" "backend" {
  name                 = "${var.project}-backend"
  image_tag_mutability = "MUTABLE"
  force_delete         = true

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = {
    Project = var.project
  }
}

# Role App Runner assumes to pull from the private ECR repo.
data "aws_iam_policy_document" "apprunner_ecr_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["build.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_ecr_access" {
  name               = "${var.project}-apprunner-ecr"
  assume_role_policy = data.aws_iam_policy_document.apprunner_ecr_assume.json
}

resource "aws_iam_role_policy_attachment" "apprunner_ecr_access" {
  role       = aws_iam_role.apprunner_ecr_access.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess"
}

# Role the running container assumes: gives the app its DynamoDB + S3 access.
data "aws_iam_policy_document" "apprunner_instance_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_instance" {
  name               = "${var.project}-apprunner-instance"
  assume_role_policy = data.aws_iam_policy_document.apprunner_instance_assume.json
}

resource "aws_iam_role_policy_attachment" "apprunner_instance" {
  role       = aws_iam_role.apprunner_instance.name
  policy_arn = aws_iam_policy.backend.arn
}

resource "aws_apprunner_service" "backend" {
  service_name = "${var.project}-backend"

  source_configuration {
    authentication_configuration {
      access_role_arn = aws_iam_role.apprunner_ecr_access.arn
    }
    image_repository {
      image_identifier      = "${aws_ecr_repository.backend.repository_url}:latest"
      image_repository_type = "ECR"
      image_configuration {
        port = "8080"
        runtime_environment_variables = {
          SPRING_PROFILES_ACTIVE     = "aws"
          NOTES_AWS_REGION           = var.aws_region
          NOTES_AWS_TABLENAME        = aws_dynamodb_table.notes.name
          NOTES_AWS_BUCKET           = aws_s3_bucket.content.bucket
          NOTES_AWS_SUBMISSIONSTABLE = aws_dynamodb_table.submissions.name
          NOTES_AWS_USERSTABLE       = aws_dynamodb_table.users.name
          GOOGLE_CLIENT_ID           = var.google_client_id
          # Submission notifications; sends fail (and are logged, not surfaced)
          # until the SES DKIM records are live at Porkbun.
          NOTES_MAIL_ENABLED = "true"
          NOTES_MAIL_FROM    = var.mail_from
          NOTES_MAIL_TO      = var.mail_to
          JWT_SECRET                 = random_password.jwt_secret.result
          # Cap heap so the JVM leaves room for AWS SDK metaspace/threads
          # inside the container's memory limit.
          JAVA_OPTS = "-XX:MaxRAMPercentage=70.0"
          # Allow both the CloudFront default domain and the custom domain.
          NOTES_CORS_ALLOWEDORIGINS = "https://${aws_cloudfront_distribution.site.domain_name},https://${var.domain_name}"
        }
      }
    }
    # Redeploy automatically when a new :latest image is pushed.
    auto_deployments_enabled = true
  }

  instance_configuration {
    cpu               = "512"  # 0.5 vCPU
    memory            = "1024" # 1 GB (Spring Boot + AWS SDK OOMs at 512 MB)
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  # Widen the grace period so a cold JVM start isn't marked unhealthy early.
  health_check_configuration {
    protocol            = "TCP"
    interval            = 10
    timeout             = 5
    healthy_threshold   = 1
    unhealthy_threshold = 5
  }

  tags = {
    Project = var.project
  }
}
