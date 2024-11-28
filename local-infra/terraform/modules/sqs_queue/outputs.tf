output "name" {
  value = aws_sqs_queue.sqs_queue.name
}

output "arn" {
  value = aws_sqs_queue.sqs_queue.arn
}

output "sqs_url" {
  value = aws_sqs_queue.sqs_queue.id
}
