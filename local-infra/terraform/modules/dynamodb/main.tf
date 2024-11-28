resource "aws_dynamodb_table" "dynamodb_table" {
  name           = var.name
  billing_mode   = var.billing_mode
  read_capacity  = var.read_capacity
  write_capacity = var.write_capacity
  hash_key       = var.hash_key
  range_key      = var.range_key


  dynamic "attribute" {
    for_each = var.attributes
    content {
      name = attribute.value.name
      type = attribute.value.type
    }
  }

  ttl {
    attribute_name = var.ttl.attribute_name
    enabled        = var.ttl.enabled
  }

  global_secondary_index {
    name               = var.global_secondary_index.name
    hash_key           = var.global_secondary_index.hash_key
    range_key          = var.global_secondary_index_range_key
    projection_type    = var.global_secondary_index.projection_type
    write_capacity     = var.global_secondary_index_write_capacity
    read_capacity      = var.global_secondary_index_read_capacity
    non_key_attributes = var.global_secondary_index_non_key_attributes
  }

}
