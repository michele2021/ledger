resource "aws_sqs_queue" "sqs_queue" {
  name                        = "${var.name}${var.fifo_queue ? ".fifo" : ""}"
  fifo_queue                  = var.fifo_queue
  delay_seconds               = var.delay_seconds
  max_message_size            = var.max_message_size
  message_retention_seconds   = var.message_retention_seconds
  receive_wait_time_seconds   = var.receive_wait_time_seconds
  redrive_policy              = var.dead_letter_queue != null ? local.redrive_policy : null
  content_based_deduplication = var.content_based_deduplication
  visibility_timeout_seconds  = var.visibility_timeout_seconds
  tags                        = var.tags
}

