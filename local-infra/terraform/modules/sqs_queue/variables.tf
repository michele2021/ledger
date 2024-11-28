variable "name" {
  description = "Unique queue name."
  type        = string
}
variable "visibility_timeout_seconds" {
  type        = number
  description = "The visibility timeout for the queue. An integer from 0 to 43200 (12 hours)."
  default     = 30
}

variable "content_based_deduplication" {
  type        = bool
  description = "Enables content-based deduplication for FIFO queues"
  default     = false
}

variable "delay_seconds" {
  description = <<EOF
  (Optional) The time in seconds that the delivery of all messages in the queue will be delayed.
  An integer from 0 to 900 (15 minutes)
  EOF
  type        = number
  default     = 0
}

variable "max_message_size" {
  description = <<EOF
  (Optional) The limit of how many bytes a message can contain before Amazon SQS rejects it.
  An integer from 1024 bytes (1 KiB) up to 262144 bytes (256 KiB)
  EOF
  type        = number
  default     = 262144
}

variable "message_retention_seconds" {
  description = <<EOF
  (Optional) The number of seconds Amazon SQS retains a message.
  Integer representing seconds, from 60 (1 minute) to 1209600 (14 days)
  EOF
  type        = number
  default     = 600
}

variable "receive_wait_time_seconds" {
  description = <<EOF
  (Optional) The time for which a ReceiveMessage call will wait for a message to arrive (long polling) before returning.
  An integer from 0 to 20 (seconds).
  EOF
  type        = number
  default     = 20
}

variable "tags" {
  description = ""
  type        = map(string)
  default     = {}
}

variable "dead_letter_queue" {
  description = "Dead letter queue arn"
  type        = string
  default     = null
}

variable "max_receive_count" {
  description = "Max receive count"
  type        = number
  default     = 5
}

variable "fifo_queue" {
  description = "Optional value to disable/enable fifo mode."
  type        = bool
  default     = false
}

locals {
  redrive_policy = jsonencode({
    deadLetterTargetArn = var.dead_letter_queue
    maxReceiveCount     = var.max_receive_count
  })
}
