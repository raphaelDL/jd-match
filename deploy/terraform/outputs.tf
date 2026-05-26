output "ecr_repository_url" {
  description = "Push the image here, then set image_tag and apply again."
  value       = aws_ecr_repository.jdmatch.repository_url
}

output "service_url" {
  description = "Public HTTPS URL of the App Runner service."
  value       = "https://${aws_apprunner_service.jdmatch.service_url}"
}

output "rds_endpoint" {
  description = "RDS Postgres endpoint (private)."
  value       = aws_db_instance.jdmatch.address
}
