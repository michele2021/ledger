data "aws_iam_policy_document" "s3_bucket_get_policy" {

  statement {
    sid    = "BucketGetPolicy"
    effect = "Allow"

    principals {
      type        = "AWS"
      identifiers = ["*"]
    }
    resources = ["${aws_s3_bucket.s3_bucket.arn}/*"]
    actions   = ["s3:GetObject"]
  }
}
