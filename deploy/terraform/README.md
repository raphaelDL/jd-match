# Deploying jd-match to AWS App Runner

Provisions ECR, RDS Postgres (with pgvector), a VPC connector, and an App Runner
service. App Runner serves the container publicly over HTTPS and reaches RDS through
the VPC connector; `ANTHROPIC_API_KEY` and the DB password live in Secrets Manager and
are injected as runtime env.

> Targets the default VPC for simplicity. Production would use a dedicated VPC with
> private subnets for RDS.

## Prerequisites

- Terraform >= 1.5, AWS credentials configured (`aws configure` / env / SSO).
- Docker with `buildx` (the image must be **linux/amd64** for App Runner).

## Steps

```bash
cd deploy/terraform
cp terraform.tfvars.example terraform.tfvars   # fill in db_password + anthropic_api_key

# 1. Create the registry (and the rest of the infra).
terraform init
terraform apply

# 2. Build and push the linux/amd64 image to the ECR repo from the output.
ECR=$(terraform output -raw ecr_repository_url)
aws ecr get-login-password | docker login --username AWS --password-stdin "${ECR%/*}"
docker buildx build --platform linux/amd64 -t "$ECR:latest" --push ../..

# 3. App Runner picks up :latest. If it was already created, redeploy:
aws apprunner start-deployment --service-arn "$(aws apprunner list-services \
  --query "ServiceSummaryList[?ServiceName=='jd-match'].ServiceArn" --output text)"

terraform output service_url
```

Health check: App Runner polls `/actuator/health/readiness`, which only reports UP once
the embedding model is loaded and the database is reachable.

## Notes

- The image bundles the PyTorch native libs and the embedding model (`ai.djl.offline=true`),
  so instances start without downloading anything — important for App Runner's scale-to-zero.
  It's a large image (~1.5 GB); `apprunner_memory` defaults to 4 GB for headroom.
- `terraform destroy` tears everything down (`force_delete`/`skip_final_snapshot` are set,
  so this is for a throwaway/demo environment).
