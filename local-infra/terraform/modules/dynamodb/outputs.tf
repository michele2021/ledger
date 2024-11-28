output "name" {
  value = aws_dynamodb_table.dynamodb_table.name
}

output "arn" {
  value = aws_dynamodb_table.dynamodb_table.arn
}

output "table_id" {
  value = aws_dynamodb_table.dynamodb_table.id
}

output "hash_key" {
  value = aws_dynamodb_table.dynamodb_table.hash_key
}
