# jd-match on AWS App Runner + RDS Postgres (pgvector).
#
# For simplicity this targets the account's default VPC; a production setup would
# place RDS in private subnets with its own VPC. App Runner serves the container
# publicly and reaches RDS through a VPC connector.

data "aws_vpc" "default" {
  default = true
}

data "aws_subnets" "default" {
  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.default.id]
  }
}

# --- Image registry --------------------------------------------------------
resource "aws_ecr_repository" "jdmatch" {
  name                 = "jd-match"
  image_tag_mutability = "MUTABLE"
  force_delete         = true
}

# --- Secrets ---------------------------------------------------------------
resource "aws_secretsmanager_secret" "anthropic_api_key" {
  name = "jd-match/anthropic-api-key"
}

resource "aws_secretsmanager_secret_version" "anthropic_api_key" {
  secret_id     = aws_secretsmanager_secret.anthropic_api_key.id
  secret_string = var.anthropic_api_key
}

resource "aws_secretsmanager_secret" "db_password" {
  name = "jd-match/db-password"
}

resource "aws_secretsmanager_secret_version" "db_password" {
  secret_id     = aws_secretsmanager_secret.db_password.id
  secret_string = var.db_password
}

# --- Networking ------------------------------------------------------------
resource "aws_security_group" "apprunner_connector" {
  name_prefix = "jd-match-apprunner-"
  description = "App Runner VPC connector egress"
  vpc_id      = data.aws_vpc.default.id

  egress {
    description = "All egress"
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_security_group" "rds" {
  name_prefix = "jd-match-rds-"
  description = "Postgres access from the App Runner connector only"
  vpc_id      = data.aws_vpc.default.id

  ingress {
    description     = "Postgres from App Runner"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.apprunner_connector.id]
  }
}

# --- Database --------------------------------------------------------------
resource "aws_db_subnet_group" "jdmatch" {
  name       = "jd-match"
  subnet_ids = data.aws_subnets.default.ids
}

resource "aws_db_instance" "jdmatch" {
  identifier             = "jd-match"
  engine                 = "postgres"
  engine_version         = "16.4"
  instance_class         = var.db_instance_class
  allocated_storage      = var.db_allocated_storage
  db_name                = var.db_name
  username               = var.db_username
  password               = var.db_password
  db_subnet_group_name   = aws_db_subnet_group.jdmatch.name
  vpc_security_group_ids = [aws_security_group.rds.id]
  publicly_accessible    = false
  skip_final_snapshot    = true
  apply_immediately      = true
  # pgvector ships with RDS Postgres 16; the app's Flyway migration runs
  # `CREATE EXTENSION vector` as this master user on first start.
}

# --- IAM -------------------------------------------------------------------
# Role App Runner assumes to pull the image from ECR.
data "aws_iam_policy_document" "apprunner_build_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["build.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_ecr_access" {
  name               = "jd-match-apprunner-ecr-access"
  assume_role_policy = data.aws_iam_policy_document.apprunner_build_assume.json
}

resource "aws_iam_role_policy_attachment" "apprunner_ecr_access" {
  role       = aws_iam_role.apprunner_ecr_access.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSAppRunnerServicePolicyForECRAccess"
}

# Instance role the running service uses to read its secrets.
data "aws_iam_policy_document" "apprunner_tasks_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["tasks.apprunner.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "apprunner_instance" {
  name               = "jd-match-apprunner-instance"
  assume_role_policy = data.aws_iam_policy_document.apprunner_tasks_assume.json
}

data "aws_iam_policy_document" "read_secrets" {
  statement {
    actions = ["secretsmanager:GetSecretValue"]
    resources = [
      aws_secretsmanager_secret.anthropic_api_key.arn,
      aws_secretsmanager_secret.db_password.arn,
    ]
  }
}

resource "aws_iam_role_policy" "read_secrets" {
  name   = "read-secrets"
  role   = aws_iam_role.apprunner_instance.id
  policy = data.aws_iam_policy_document.read_secrets.json
}

# --- App Runner ------------------------------------------------------------
resource "aws_apprunner_vpc_connector" "jdmatch" {
  vpc_connector_name = "jd-match"
  subnets            = data.aws_subnets.default.ids
  security_groups    = [aws_security_group.apprunner_connector.id]
}

resource "aws_apprunner_service" "jdmatch" {
  service_name = "jd-match"

  source_configuration {
    auto_deployments_enabled = false

    authentication_configuration {
      access_role_arn = aws_iam_role.apprunner_ecr_access.arn
    }

    image_repository {
      image_identifier      = "${aws_ecr_repository.jdmatch.repository_url}:${var.image_tag}"
      image_repository_type = "ECR"

      image_configuration {
        port = "8080"

        runtime_environment_variables = {
          DB_HOST = aws_db_instance.jdmatch.address
          DB_PORT = "5432"
          DB_NAME = var.db_name
          DB_USER = var.db_username
        }

        runtime_environment_secrets = {
          ANTHROPIC_API_KEY = aws_secretsmanager_secret.anthropic_api_key.arn
          DB_PASSWORD       = aws_secretsmanager_secret.db_password.arn
        }
      }
    }
  }

  instance_configuration {
    cpu               = var.apprunner_cpu
    memory            = var.apprunner_memory
    instance_role_arn = aws_iam_role.apprunner_instance.arn
  }

  health_check_configuration {
    protocol            = "HTTP"
    path                = "/actuator/health/readiness"
    interval            = 10
    timeout             = 5
    healthy_threshold   = 1
    unhealthy_threshold = 5
  }

  network_configuration {
    egress_configuration {
      egress_type       = "VPC"
      vpc_connector_arn = aws_apprunner_vpc_connector.jdmatch.arn
    }
  }

  depends_on = [aws_db_instance.jdmatch]
}
