data "aws_caller_identity" "current" {}

data "archive_file" "lambda" {
  type        = "zip"
  source_file = "${path.module}./target/catalog-service-lambda-jar-with-dependencies.jar"
  output_path = "${path.module}/${var.catalog_update_lambda_file_name}"
}

data "aws_availability_zones" "available" {
  state = "available"
}

data "aws_iam_policy_document" "assume_role" {
  statement {
    effect = "Allow"
    actions = [
      "sts:AssumeRole"
    ]
    principals {
      type = "Service"
      identifiers = ["lambda.amazonaws.com"]
    }
  }
}

data "aws_iam_policy_document" "lambda_policy_document" {
  # Permisos para logs de CloudWatch
  statement {
    effect = "Allow"
    actions = [
      "logs:CreateLogGroup",
      "logs:CreateLogStream",
      "logs:PutLogEvents"
    ]
    resources = ["arn:aws:logs:*:*:*"]
  }

  # Permisos de red necesarios para Lambdas dentro de VPC
  statement {
    effect = "Allow"
    actions = [
      "ec2:CreateNetworkInterface",
      "ec2:DescribeNetworkInterfaces", # ← corregido (plural)
      "ec2:DeleteNetworkInterface",
      "ec2:AttachNetworkInterface",
      "ec2:DetachNetworkInterface"
    ]
    resources = ["*"]
  }

  # (Opcional) Acceso al bucket S3 de tu Lambda
  statement {
    effect = "Allow"
    actions = [
      "s3:GetObject",
      "s3:PutObject",
      "s3:ListBucket"
    ]
    resources = [
      aws_s3_bucket.catalog_bucket.arn,
      "${aws_s3_bucket.catalog_bucket.arn}/*"
    ]
  }

  # (Opcional) Acceso de solo lectura a ElastiCache (Redis)
  statement {
    effect = "Allow"
    actions = [
      "elasticache:DescribeCacheClusters",
      "elasticache:DescribeReplicationGroups"
    ]
    resources = ["*"]
  }
}
