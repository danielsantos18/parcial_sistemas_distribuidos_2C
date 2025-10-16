variable "catalog_update_lambda_file_name" {
  default = "catalog-update-lmb.zip"
}

variable "catalog_update_lambda_function_name" {
  type    = string
  default = "catalog-update-lambda"
}

variable "vpc_cidr" {
  type        = string
  description = "CIDR Block for vpc"
  default     = "10.0.0.0/16"
}

variable "private_subnet_cidrs" {
  type = list(string)
  default = ["10.0.101.0/24", "10.0.102.0/24"]
}

variable "public_subnet_cidrs" {
  type = list(string)
  default = ["10.0.1.0/24", "10.0.2.0/24"]
}

variable "redis_cluster_name" {
  type    = string
  default = "redis-cluster"
}

variable "redis_node_type" {
  type    = string
  default = "cache.t3.micro"
}

variable "redis_num_nodes" {
  type    = number
  default = 1
}

variable "catalog_stage" {
  type    = string
  default = "catalog"
}