# Infrastructure & Deploy

Terraform for the notes app on AWS:

- **DynamoDB** — note metadata, TTL on `expiresAt` for auto-expire
- **S3** — note content (`pasquin-content`) and the static site (`pasquin-site`)
- **CloudFront** — CDN for the site, with the `/n/{slug}` URL-rewrite function
- **ECR + App Runner** — the Spring Boot backend as a container, HTTPS out of the box
- **IAM** — least-privilege policy the backend assumes for DynamoDB + S3

## Prerequisites

```sh
winget install Hashicorp.Terraform   # not yet installed on this machine
aws configure                         # credentials with admin-ish access
# Docker Desktop running (for building/pushing the backend image)
```

Bucket and ECR names derive from `project` in `variables.tf` (default
`pasquin`). Names are globally unique — change `project` if taken.

## First deploy (ordering matters)

App Runner needs the image to exist before it can start, so create the
registry first, push, then apply the rest.

```sh
cd infra
terraform init

# 1. Create just the ECR repo
terraform apply -target=aws_ecr_repository.backend

# 2. Build and push the backend image to it
REPO=$(terraform output -raw ecr_repository_url)
REGION=$(terraform output -raw aws_region 2>/dev/null || echo ap-southeast-1)
aws ecr get-login-password --region $REGION \
  | docker login --username AWS --password-stdin ${REPO%/*}
docker build -t $REPO:latest ../backend
docker push $REPO:latest

# 3. Create everything else (DynamoDB, S3, CloudFront, App Runner, IAM)
terraform apply
```

Note the outputs: `backend_url`, `cloudfront_domain`, `content_bucket`,
`site_bucket`.

## Deploy the frontend

The static build bakes in the backend URL, so build *after* the backend
exists:

```sh
cd ../frontend
echo "PUBLIC_API_URL=$(cd ../infra && terraform output -raw backend_url)" > .env
npm run build
aws s3 sync dist/ s3://$(cd ../infra && terraform output -raw site_bucket) --delete
aws cloudfront create-invalidation \
  --distribution-id <id> --paths '/*'   # after content changes
```

## Redeploys

- **Backend**: `docker build` + `docker push $REPO:latest`. App Runner has
  `auto_deployments_enabled`, so it rolls out the new image automatically.
- **Frontend**: `npm run build` + `aws s3 sync` + CloudFront invalidation.

## Cost control ($170 budget)

App Runner bills for the provisioned container while the service runs
(~$5–15/mo at 0.25 vCPU / 0.5 GB). **Pause it between demo sessions** to
drop to ~$0:

```sh
aws apprunner pause-service  --service-arn <arn>
aws apprunner resume-service --service-arn <arn>
```

DynamoDB (on-demand), S3, and CloudFront are all pennies at hobby traffic.

## Event-driven cleanup

Removing a note's DynamoDB item — whether by TTL expiry, a user delete,
or an admin takedown — emits a stream event that triggers the cleanup
Lambda (`lambda.tf`, code in `lambda/cleanup/`). It deletes the orphaned
S3 content body and any premium images the note embedded, then evicts
those images from CloudFront. Costs effectively nothing: Lambda and
stream reads sit inside the always-free tier.

Everything once on the "not yet automated" list has shipped: custom
domain + ACM (`acm.tf`), CI/CD (GitHub Actions with OIDC), and this
cleanup pipeline.
