variable "region" {
  description = "AWS region to deploy into."
  type        = string
  default     = "us-east-1"
}

variable "image_tag" {
  description = "Tag of the jd-match image in ECR to run."
  type        = string
  default     = "latest"
}

variable "db_name" {
  description = "Postgres database name."
  type        = string
  default     = "jdmatch"
}

variable "db_username" {
  description = "Postgres master username (runs Flyway, including CREATE EXTENSION vector)."
  type        = string
  default     = "jdmatch"
}

variable "db_password" {
  description = "Postgres master password."
  type        = string
  sensitive   = true
}

variable "anthropic_api_key" {
  description = "Anthropic API key, stored in Secrets Manager and injected at runtime."
  type        = string
  sensitive   = true
}

variable "db_instance_class" {
  description = "RDS instance class."
  type        = string
  default     = "db.t4g.micro"
}

variable "db_allocated_storage" {
  description = "RDS allocated storage in GB."
  type        = number
  default     = 20
}

variable "apprunner_cpu" {
  description = "App Runner CPU (1024 = 1 vCPU). The embedding model + libtorch need headroom."
  type        = string
  default     = "1024"
}

variable "apprunner_memory" {
  description = "App Runner memory in MB."
  type        = string
  default     = "4096"
}
