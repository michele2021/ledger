variable "name" {
  description = "(Required) The name of the table, this needs to be unique within a region"
  type        = string
}
variable "billing_mode" {
  description = <<EOF
  (Optional) Controls how you are charged for read and write throughput and how you manage capacity.
  The valid values are PROVISIONED and PAY_PER_REQUEST. Defaults to PROVISIONED.
  EOF
  type        = string
  default     = "PAY_PER_REQUEST"
}

variable "hash_key" {
  description = <<EOF
  (Required, Forces new resource) The attribute to use as the hash (partition) key.
  Must also be defined as an attribute.
  EOF
  type        = string
  default     = null
}

variable "range_key" {
  description = "(Optional) The attribute to use as the range (sort) key. Must also be defined as an attribute."
  type        = string
  default     = null
}

variable "read_capacity" {
  description = "(Optional) The number of read units for this table. If the billing_mode is PROVISIONED, this field is required."
  type        = number
  default     = null
}

variable "write_capacity" {
  description = "(Optional) The number of write units for this table. If the billing_mode is PROVISIONED, this field is required."
  type        = number
  default     = null
}

variable "global_secondary_index" {
  description = <<EOF
  (Optional) Describe a GSI for the table; subject to the normal limits on the number of GSIs, projected attributes, etc.

   - name - (Required) The name of the index
   - write_capacity - (Optional) The number of write units for this index. Must be set if billing_mode is set to PROVISIONED.
   - read_capacity - (Optional) The number of read units for this index. Must be set if billing_mode is set to PROVISIONED.
   - hash_key - (Required) The name of the hash key in the index; must be defined as an attribute in the resource.
   - range_key - (Optional) The name of the range key; must be defined

   - projection_type - (Required) One of ALL, INCLUDE or KEYS_ONLY where ALL projects every attribute into the index,
     KEYS_ONLY projects just the hash and range key into the index,
     and INCLUDE projects only the keys specified in the non_key_attributes parameter.

   - non_key_attributes - (Optional) Only required with INCLUDE as a projection type;
     a list of attributes to project into the index. These do not need to be defined as attributes on the table.
  EOF

  type = object({
    name            = string,
    hash_key        = string,
    projection_type = string
  })
  default = null
}

variable "global_secondary_index_range_key" {
  type    = string
  default = null
}

variable "global_secondary_index_write_capacity" {
  type    = number
  default = 1
}

variable "global_secondary_index_read_capacity" {
  type    = number
  default = 1
}

variable "global_secondary_index_non_key_attributes" {
  type    = list(string)
  default = []
}

variable "ttl" {
  description = <<EOF
  (Optional) Defines ttl, has two properties, and can only be specified once:
   - enabled - (Required) Indicates whether ttl is enabled (true) or disabled (false).
   - attribute_name - (Required) The name of the table attribute to store the TTL
  EOF
  type        = object({
    attribute_name = string,
    enabled        = bool
  })
  default = {
    attribute_name = null
    enabled        = false
  }
}

variable "attributes" {
  description = <<EOF
  (Required) List of nested attribute definitions. Only required for hash_key and range_key attributes.
  Each attribute has two properties:
   - name - (Required) The name of the attribute
   - type - (Required) Attribute type, which must be a scalar type: S, N, or B for (S)tring, (N)umber or (B)inary data
  EOF
  type        = list(object({
    name = string,
    type = string
  }))
  default = null
}
