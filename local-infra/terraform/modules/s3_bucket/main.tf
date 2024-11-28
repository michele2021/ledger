resource "aws_s3_bucket" "s3_bucket" {
  bucket = var.bucket_name
}

resource "aws_s3_bucket_acl" "example_bucket_acl" {
  bucket = aws_s3_bucket.s3_bucket.id
  acl    = var.acl
  depends_on = [aws_s3_bucket.s3_bucket]
}

resource "aws_s3_bucket_policy" "s3_bucket_policy" {
  bucket = aws_s3_bucket.s3_bucket.id
  policy = data.aws_iam_policy_document.s3_bucket_get_policy.json
  depends_on = [aws_s3_bucket.s3_bucket]
}
