output "sqs_queue_urls" {
  value = {for k, v in module.queues: k => v.sqs_url}
}

output "sns_topic_arn" {
  value = {for k, v in module.sns: k => v.arn}
}

output "table_names" {
    value = {for k, v in module.dynamodb_tables: k => v.name}
}

output "reports_bucket_name" {
  value = module.reports_bucket.name
}
