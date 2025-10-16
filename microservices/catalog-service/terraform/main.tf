resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true
}

//Internet Gateway
resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id
}

//Public Subnet
resource "aws_subnet" "public" {
  vpc_id     = aws_vpc.main.id
  count = length(var.public_subnet_cidrs)
  cidr_block = var.public_subnet_cidrs[count.index]
}

//Private Subnet (for redis and lambda)
//redis no es publica
resource "aws_subnet" "private" {
  vpc_id            = aws_vpc.main.id
  count = length(var.private_subnet_cidrs)
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = data.aws_availability_zones.available.names[count.index]
}

//Route Table for public subnets
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }
}

//Route Table for private subnets
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id
}

//rouete table association for public subnets
resource "aws_route_table_association" "public" {
  route_table_id = aws_route_table.public.id
  subnet_id      = aws_subnet.public[count.index].id
  count = length(aws_subnet.public)
}

//route table association for privates subnets
resource "aws_route_table_association" "private" {
  route_table_id = aws_route_table.private.id
  subnet_id      = aws_subnet.private[count.index].id
  count = length(aws_subnet.private)
}

//Cluster de redis
resource "aws_elasticache_cluster" "redis_cluster" {
  cluster_id           = var.redis_cluster_name
  engine               = "redis"
  node_type            = var.redis_node_type
  num_cache_nodes      = var.redis_num_nodes
  parameter_group_name = "default.redis7"
  port                 = 6379
  security_group_ids = [aws_security_group.redis_sg.id]
  subnet_group_name    = aws_elasticache_subnet_group.redis_subnet.id
}

resource "aws_elasticache_subnet_group" "redis_subnet" {
  name       = "${var.redis_cluster_name}-subnet-group"
  subnet_ids = aws_subnet.private[*].id
}

resource "aws_security_group" "redis_sg" {
  name        = "${var.redis_cluster_name}-sg"
  description = "security group for redis cluster"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "Redis for lambda"
    from_port   = 6379
    to_port     = 6379
    protocol    = "tcp"
    security_groups = [aws_security_group.lambda_sg.id] //to create
  }
  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

//S3 BUCKET
resource "aws_s3_bucket" "catalog_bucket" {
  bucket        = "inferno-catalog-bucket"
  force_destroy = true
}

output "catalog_bucket_name" {
  value = aws_s3_bucket.catalog_bucket.bucket
}

//COLAS SQS
resource "aws_sqs_queue" "start_payment_queue" {
  name                       = "start-payment-queue"
  delay_seconds              = 5
  visibility_timeout_seconds = 60
}

resource "aws_sqs_queue" "check_balance_queue" {
  name                       = "check-balance-queue"
  delay_seconds              = 5
  visibility_timeout_seconds = 60
}

resource "aws_sqs_queue" "transaction_queue" {
  name                       = "transaction-queue"
  delay_seconds              = 5
  visibility_timeout_seconds = 60
}

//ROLES
resource "aws_iam_role" "lambda_role" {
  name = "ExecutionLambaCatalog"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Principal = {
          Service = "lambda.amazonaws.com"
        }
      }
    ]
  })
}

resource "aws_iam_role_policy" "lambda_policy" {
  name   = "catalog-lambda-policy"
  role   = aws_iam_role.lambda_role.id
  policy = data.aws_iam_policy_document.lambda_policy_document.json
}

resource "aws_security_group" "lambda_sg" {
  name        = "catalog-lambda-sg"
  description = "Security group for lambda function"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port = 0
    to_port   = 0
    protocol  = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}


resource "null_resource" "lambda_build_trigger" {
  triggers = {
    build_number = timestamp()
  }
}

//LAMBDAS
resource "aws_lambda_function" "CatalogUpdateLambda" {
  filename         = "../target/catalog-service-lambda-jar-with-dependencies.jar"
  function_name    = var.catalog_update_lambda_function_name
  handler          = "com.inferno.catalog_service.handler.CatalogUpdateLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  source_code_hash = "${filebase64sha256("../target/catalog-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"
  role             = aws_iam_role.lambda_role.arn

  vpc_config {
    subnet_ids = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda_sg.id]
  }

  environment {
    variables = {
      REDIS_URI         = "redis://${aws_elasticache_cluster.redis_cluster.cache_nodes[0].address}:${aws_elasticache_cluster.redis_cluster.port}"
      BUCKET_NAME       = aws_s3_bucket.catalog_bucket.bucket
      REDIS_ENDPOINT    = aws_elasticache_cluster.redis_cluster.cache_nodes[0].address
      REDIS_PORT        = aws_elasticache_cluster.redis_cluster.port
      CATALOG_REDIS_KEY = "catalog:services"
    }
  }

  depends_on = [
    aws_iam_role_policy.lambda_policy,
    data.archive_file.lambda,
    aws_elasticache_cluster.redis_cluster
  ]
}

//API GATEWAY
resource "aws_api_gateway_rest_api" "CatalogUpdateApi" {
  name        = "catalog update api "
  description = "esta api gateway sirve para subir el csv del catalogo de servicios a redis"
}

//Resource Api Gateway
resource "aws_api_gateway_resource" "CatalogUpdateResource" {
  rest_api_id = aws_api_gateway_rest_api.CatalogUpdateApi.id
  parent_id   = aws_api_gateway_rest_api.CatalogUpdateApi.root_resource_id
  path_part   = "update"
}

//Method Del Api Gateway
resource "aws_api_gateway_method" "MethodGetUser" {
  resource_id = aws_api_gateway_resource.CatalogUpdateResource.id
  //Debe Apuntar Al Recurso Donde Define El Parameter uuid
  rest_api_id   = aws_api_gateway_rest_api.CatalogUpdateApi.id
  http_method   = "POST"
  authorization = "NONE"
  request_models = {
    "multipart/form-data" = "Empty"
  }
}

//Connect Del Api A Lambda
resource "aws_api_gateway_integration" "IntegrationCatalogUpdate" {
  rest_api_id = aws_api_gateway_rest_api.CatalogUpdateApi.id
  resource_id = aws_api_gateway_resource.CatalogUpdateResource.id
  //Debe Apuntar Al Recurso Donde Define El Parameter uuid
  http_method = aws_api_gateway_method.MethodGetUser.http_method
  //consultar metodos de integracion entre api y lambda
  integration_http_method = "POST"  // Lambda siempre usa POST
  type        = "AWS_PROXY"
  uri         = aws_lambda_function.CatalogUpdateLambda.invoke_arn
}

//Connect De La Lambda A Api
resource "aws_lambda_permission" "ApiGwLambdaCatalogUpdate" {
  statement_id  = "AllowExcutionFromAPIGateway"
  action        = "lambda:InvokeFunction"
  function_name = var.catalog_update_lambda_function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CatalogUpdateApi.execution_arn}/*/POST/${aws_api_gateway_resource.CatalogUpdateResource.path_part}/*"
}

//Deploy De La Api
resource "aws_api_gateway_deployment" "deploymentCatalogUpdateEndpoint" {
  rest_api_id = aws_api_gateway_rest_api.CatalogUpdateApi.id
  depends_on = [aws_api_gateway_integration.IntegrationCatalogUpdate, aws_lambda_permission.ApiGwLambdaCatalogUpdate]
}

//stage -> dev,qa,pre-production
resource "aws_api_gateway_stage" "StagCatalogUpdate" {
  deployment_id = aws_api_gateway_deployment.deploymentCatalogUpdateEndpoint.id
  rest_api_id   = aws_api_gateway_rest_api.CatalogUpdateApi.id
  stage_name    = var.catalog_stage
}

output "apiUrlCatalogUpdate" {
  value = "${aws_api_gateway_stage.StagCatalogUpdate.invoke_url}/${aws_api_gateway_resource.CatalogUpdateResource.path_part}"
}

//LAMBDA PARA OBTENER CATALOGO
resource "aws_lambda_function" "CatalogGetLambda" {
  filename      = "../target/catalog-service-lambda-jar-with-dependencies.jar"
  function_name = "catalog-get-lambda"
  handler       = "com.inferno.catalog_service.handler.CatalogGetLambda::handleRequest"
  runtime       = "java17"
  timeout       = 30
  memory_size   = 256
  role          = aws_iam_role.lambda_role.arn

  vpc_config {
    subnet_ids = aws_subnet.private[*].id
    security_group_ids = [aws_security_group.lambda_sg.id]
  }

  environment {
    variables = {
      REDIS_ENDPOINT    = aws_elasticache_cluster.redis_cluster.cache_nodes[0].address
      REDIS_URI         = "redis://${aws_elasticache_cluster.redis_cluster.cache_nodes[0].address}:${aws_elasticache_cluster.redis_cluster.port}"
      REDIS_PORT        = aws_elasticache_cluster.redis_cluster.port
      CATALOG_REDIS_KEY = "catalog:services"
    }
  }

  depends_on = [
    aws_iam_role_policy.lambda_policy,
    aws_elasticache_cluster.redis_cluster
  ]
}

//API GATEWAY PARA OBTENER CATALOGO

resource "aws_api_gateway_rest_api" "CatalogGetApi" {
  name        = "catalog get api "
  description = "esta api gateway sirve para obtener el csv del catalogo de servicios a redis"
}

# API Gateway principal (puedes reutilizar el existente)
resource "aws_api_gateway_resource" "CatalogGetResource" {
  rest_api_id = aws_api_gateway_rest_api.CatalogGetApi.id
  parent_id   = aws_api_gateway_rest_api.CatalogGetApi.root_resource_id
  path_part   = "catalog"
}

# Método GET
resource "aws_api_gateway_method" "CatalogGetMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CatalogGetApi.id
  resource_id   = aws_api_gateway_resource.CatalogGetResource.id
  http_method   = "GET"
  authorization = "NONE"
}

# Integración con Lambda
resource "aws_api_gateway_integration" "CatalogGetIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CatalogGetApi.id
  resource_id             = aws_api_gateway_resource.CatalogGetResource.id
  http_method             = aws_api_gateway_method.CatalogGetMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.CatalogGetLambda.invoke_arn
}

# Permiso para que API Gateway invoque la Lambda
resource "aws_lambda_permission" "ApiGwLambdaCatalogGet" {
  statement_id  = "AllowExecutionFromApiGatewayGetCatalog"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CatalogGetLambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CatalogGetApi.execution_arn}/*/GET/catalog"
}

# Deployment
resource "aws_api_gateway_deployment" "deploymentCatalogGet" {
  rest_api_id = aws_api_gateway_rest_api.CatalogGetApi.id
  depends_on = [
    aws_api_gateway_integration.CatalogGetIntegration,
    aws_lambda_permission.ApiGwLambdaCatalogGet
  ]
}

# Stage
resource "aws_api_gateway_stage" "StageCatalogGet" {
  deployment_id = aws_api_gateway_deployment.deploymentCatalogGet.id
  rest_api_id   = aws_api_gateway_rest_api.CatalogGetApi.id
  stage_name    = "dev"
}

output "apiUrlCatalogGet" {
  value = "${aws_api_gateway_stage.StageCatalogGet.invoke_url}/catalog"
}

//SQS FLUJO

