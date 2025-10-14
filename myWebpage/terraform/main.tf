resource "random_id" "bucket_prefix" {
  byte_length = 8
}

resource "aws_s3_bucket" "angular_app" {
  bucket        = "${var.project_name}-${random_id.bucket_prefix.hex}"
  force_destroy = true
}


resource "aws_s3_bucket_website_configuration" "angular_app" {
  bucket = aws_s3_bucket.angular_app.id

  index_document {
    suffix = "index.html"
  }

  error_document {
    key = "index.html"
  }
}

resource "aws_s3_bucket_public_access_block" "angular_app" {
  bucket = aws_s3_bucket.angular_app.id

  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  bucket = aws_s3_bucket.angular_app.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}


data "aws_iam_policy_document" "s3_policy_document" {
  statement {
    sid     = "S3GetObjectForCloudFront"
    actions = [
      "s3:GetObject",
      "s3:ListBucket"
    ]
    resources = [
      aws_s3_bucket.angular_app.arn,
      "${aws_s3_bucket.angular_app.arn}/*"
    ]

    principals {
      type        = "AWS"
      identifiers = [aws_cloudfront_origin_access_identity.angular_app.iam_arn]
    }
  }
}

resource "aws_s3_bucket_policy" "angular_app" {
  bucket = aws_s3_bucket.angular_app.id
  policy = data.aws_iam_policy_document.s3_policy_document.json
}


resource "aws_cloudfront_origin_access_identity" "angular_app" {
  comment = "OAI for ${var.project_name}"
}

resource "aws_cloudfront_distribution" "angular_app" {
  enabled             = true
  is_ipv6_enabled     = true
  comment             = "${var.project_name} distribution"
  default_root_object = "index.html"

  origin {
    domain_name = aws_s3_bucket.angular_app.bucket_regional_domain_name
    origin_id   = "S3-angular-app" # 🔧 ID fijo y estable

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.angular_app.cloudfront_access_identity_path
    }
  }

  default_cache_behavior {
    allowed_methods  = ["DELETE", "GET", "HEAD", "OPTIONS", "PATCH", "POST", "PUT"]
    cached_methods   = ["GET", "HEAD"]
    target_origin_id = "S3-angular-app" # 🔧 debe coincidir con origin_id
    compress         = true

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    viewer_protocol_policy = "redirect-to-https"
    min_ttl                = 0
    default_ttl            = 3600
    max_ttl                = 86400
  }

  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 300
  }

  custom_error_response {
    error_code            = 404
    response_page_path    = "/index.html"
    error_caching_min_ttl = 300
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    cloudfront_default_certificate = true
  }
}

output "website_url" {
  description = "Full URL of the website"
  value       = "https://${aws_cloudfront_distribution.angular_app.domain_name}"
}

output "s3_bucket_name" {
  value = aws_s3_bucket.angular_app.bucket
}

output "cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.angular_app.id
}

output "cloudfront_domain_name" {
  value = aws_cloudfront_distribution.angular_app.domain_name
}
