terraform {
  backend "local" {
    path = ".state/terraform.tfstate"
  }

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 4.8"
    }
  }
}


locals {
  default_endpoint = "http://localhost:4566"
}

provider "aws" {
  region     = "eu-central-1"
  access_key = "mock-access-key"
  secret_key = "mock-secret-key"

  s3_use_path_style           = true
  skip_requesting_account_id  = true
  skip_credentials_validation = true
  skip_metadata_api_check     = true

  endpoints {
    s3       = local.default_endpoint
    sqs      = local.default_endpoint
    sns      = local.default_endpoint
    dynamodb = local.default_endpoint
  }
}

module "reports_bucket" {
  source      = "./modules/s3_bucket"
  bucket_name = "clevercards-development-ledger-data"
  acl         = "public-read"
}

module "queues" {
  source                      = "./modules/sqs_queue"
  for_each                    = var.sqs_queues
  name                        = each.key
  fifo_queue                  = true
  content_based_deduplication = true
}

module "sns" {
  source                      = "./modules/sns"
  for_each                    = var.sns_topics
  name                        = each.key
}


module "dynamodb_tables" {
  source     = "./modules/dynamodb"
  for_each   = var.dynamodb_tables
  name       = each.key
  hash_key   = "MessageId"
  attributes = [
    { name = "MessageId", type = "S" },
    { name = "Status", type = "S" },
    { name = "CreatedAt", type = "N" },
  ]

  global_secondary_index = {
    name            = "${each.key}_global_index"
    hash_key        = "Status"
    projection_type = "KEYS_ONLY"
  }
  global_secondary_index_range_key = "CreatedAt"

  ttl = { enabled = true, attribute_name = "ExpirationTime" }
}
