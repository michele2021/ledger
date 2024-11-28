variable "sqs_queues" {
  description = "List of sqs queue names"
  type        = set(string)
  default     = []
}

variable "sns_topics" {
  description = "List of sns topic names"
  type        = set(string)
  default     = []
}

variable "dynamodb_tables" {
  description = "List of dynamodb table names"
  type        = set(string)
  default     = []
}
