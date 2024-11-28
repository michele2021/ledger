variable "name" {
  description = "(Required) Unique topic name"
  type        = string

}

output "name" {
  value = aws_sns_topic.sns_topic.name
}

output "arn" {
  value = aws_sns_topic.sns_topic.arn
}

resource "aws_sns_topic" "sns_topic" {
  name = var.name
}
