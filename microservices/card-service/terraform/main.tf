terraform {
  required_providers {
    aws = {
      version = "~> 6.12.0"
      source  = "hashicorp/aws"
    }
  }
}

//===============================SQS Y DLQ===================================
resource "aws_sqs_queue" "card_dlq" {
  name                      = "error-create-request-card-sqs"
  message_retention_seconds = 1209600
}

resource "aws_sqs_queue" "create_request_queue" {
  name                       = "create-request-card-sqs"
  visibility_timeout_seconds = 100
  redrive_policy = jsonencode({
    deadLetterTargetArn = aws_sqs_queue.card_dlq.arn
    maxReceiveCount     = 5
  })
}

//===============================DYNAMODB===================================
resource "aws_dynamodb_table" "card_table" {
  name           = "card-table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "uuid"
  range_key      = "createdAt"

  attribute {
    name = "uuid"
    type = "S"
  }
  attribute {
    name = "createdAt"
    type = "S"
  }
}


resource "aws_dynamodb_table" "card_table_error" {
  name           = "card-table-error"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "uuid"
  range_key      = "createdAt"

  attribute {
    name = "uuid"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }
}

resource "aws_dynamodb_table" "transaction_table" {
  name           = "transaction-table"
  billing_mode   = "PROVISIONED"
  read_capacity  = 20
  write_capacity = 20
  hash_key       = "uuid"
  range_key      = "createdAt"

  attribute {
    name = "uuid"
    type = "S"
  }

  attribute {
    name = "createdAt"
    type = "S"
  }
}


//===============================S3===================================
resource "aws_s3_bucket" "transactions_reports" {
  bucket = "inferno-bank-transactions-report-bucket"
}

resource "aws_iam_policy" "s3WriteAccess" {
  name        = "S3WriteAccessToCardBucket"
  description = "policy for write access to carbucket"
  policy      = data.aws_iam_policy_document.s3_policy.json
}

//===============================ROLES AND POLICY===================================

resource "null_resource" "lambda_build_trigger" {
  triggers = {
    build_number = timestamp()
  }
}

resource "aws_iam_role" "lambda_role" {
  name               = "ExecutionLambaCard"
  assume_role_policy = data.aws_iam_policy_document.assume_role.json
}

resource "aws_iam_role_policy_attachment" "lmb_policy_execution" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole"
}

resource "aws_iam_role_policy_attachment" "dynamo_access" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess"
}

resource "aws_iam_role_policy_attachment" "s3_access" {
  role       = aws_iam_role.lambda_role.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3FullAccess"
}

resource "aws_iam_role_policy" "sqs_access" {
  name = "SQSAccessPolicy"
  role = aws_iam_role.lambda_role.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Action = [
          "sqs:SendMessage",
          "sqs:ReceiveMessage",
          "sqs:DeleteMessage",
          "sqs:GetQueueAttributes",
          "sqs:GetQueueAttributes",
          "sqs:ChangeMessageVisibility"
        ]
        Resource = aws_sqs_queue.create_request_queue.arn
      }
    ]
  })
}

# Event Source Mapping para conectar SQS con Lambda
resource "aws_lambda_event_source_mapping" "sqs_to_approval_worker" {
  event_source_arn = aws_sqs_queue.create_request_queue.arn
  function_name    = aws_lambda_function.card_approval_worker.arn
  batch_size       = 1
  enabled          = true
}

resource "aws_lambda_permission" "allow_sqs" {
  statement_id  = "AllowExecutionFromSQS"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CreateCardLmb.function_name
  principal     = "sqs.amazonaws.com"
  source_arn    = aws_sqs_queue.create_request_queue.arn
}

//===============================LAMBDAS===================================

resource "aws_lambda_function" "CreateCardLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "create-card-request-lambda"
  handler          = "com.inferno.card_service.handler.CreateCardLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"

  environment {
    variables = {
      SQS_QUEUE_URL = aws_sqs_queue.create_request_queue.url
      S3_BUCKET     = aws_s3_bucket.transactions_reports.bucket
    }
  }
}

resource "aws_lambda_function" "CardPurchaseLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-purchase-lambda"
  handler          = "com.inferno.card_service.handler.CardPurchaseLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"

  environment {

  }
}

resource "aws_lambda_function" "GetCardLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "get-card-lambda"
  handler          = "com.inferno.card_service.handler.GetCardLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"

  environment {

  }
}

resource "aws_lambda_function" "CardTransactionSaveLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-transaction-save-lambda"
  handler          = "com.inferno.card_service.handler.CardTransactionSaveLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"
}

resource "aws_lambda_function" "CardPaidCreditLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-paid-credit-card-lambda"
  handler          = "com.inferno.card_service.handler.CardPaidCreditLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"
}

resource "aws_lambda_function" "CardGetReportLmb" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-get-report-lambda"
  handler          = "com.inferno.card_service.handler.CardGetReportLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"
}

# Lambda que consume SQS
resource "aws_lambda_function" "card_approval_worker" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-approval-worker"
  handler          = "com.inferno.card_service.handler.CardApprovalWorker::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"

  environment {
    variables = {
      USER_SERVICE_URL = "https://v7hjhcn0ej.execute-api.us-east-2.amazonaws.com"
    }
  }
}

resource "aws_lambda_function" "card_activate_lambda" {
  filename         = "../target/card-service-lambda-jar-with-dependencies.jar"
  function_name    = "card-activate-lambda"
  handler          = "com.inferno.card_service.handler.CardActivateLambda::handleRequest"
  runtime          = "java17"
  timeout          = 90
  memory_size      = 256
  role             = aws_iam_role.lambda_role.arn
  source_code_hash = "${filebase64sha256("../target/card-service-lambda-jar-with-dependencies.jar")}-${null_resource.lambda_build_trigger.id}"

  environment {
    variables = {
      DYNAMODB_TABLE = "card-table"
    }
  }
}


//===============================API GATEWAY===================================

#==========================API GATEWAY PARA CREAR TARJETAS===========================
resource "aws_api_gateway_rest_api" "CardCreateApi" {
  name        = "card-create-api"
  description = "API para creación de tarjetas (crédito/débito)"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso principal para cards
resource "aws_api_gateway_resource" "CardsResource" {
  rest_api_id = aws_api_gateway_rest_api.CardCreateApi.id
  parent_id   = aws_api_gateway_rest_api.CardCreateApi.root_resource_id
  path_part   = "cards"
}

# Recurso para /cards/request
resource "aws_api_gateway_resource" "CardRequestResource" {
  rest_api_id = aws_api_gateway_rest_api.CardCreateApi.id
  parent_id   = aws_api_gateway_resource.CardsResource.id
  path_part   = "request"
}

#=======================MÉTODO HTTP=======================

# POST /cards/request
resource "aws_api_gateway_method" "CardRequestMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CardCreateApi.id
  resource_id   = aws_api_gateway_resource.CardRequestResource.id
  http_method   = "POST"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

# Integración para create-card-request-lambda
resource "aws_api_gateway_integration" "CardRequestIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CardCreateApi.id
  resource_id             = aws_api_gateway_resource.CardRequestResource.id
  http_method             = aws_api_gateway_method.CardRequestMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.CreateCardLmb.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwCardCreate" {
  statement_id  = "AllowExecutionFromAPIGatewayCardCreate"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CreateCardLmb.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CardCreateApi.execution_arn}/*/POST/cards/request"
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "CardCreateApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.CardCreateApi.id
  depends_on = [aws_api_gateway_integration.CardRequestIntegration]
}

resource "aws_api_gateway_stage" "CardCreateApiStage" {
  deployment_id = aws_api_gateway_deployment.CardCreateApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.CardCreateApi.id
  stage_name    = "prod"
}

#=======================OUTPUTS=======================

output "card_create_endpoint" {
  value = "${aws_api_gateway_stage.CardCreateApiStage.invoke_url}/cards/request"
}

#==========================API GATEWAY PARA ACTIVAR TARJETAS===========================
resource "aws_api_gateway_rest_api" "CardActivateApi" {
  name        = "card-activate-api"
  description = "API para activación de tarjetas después de 10 transacciones"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso principal para card
resource "aws_api_gateway_resource" "CardResource" {
  rest_api_id = aws_api_gateway_rest_api.CardActivateApi.id
  parent_id   = aws_api_gateway_rest_api.CardActivateApi.root_resource_id
  path_part   = "card"
}

# Recurso para /card/activate
resource "aws_api_gateway_resource" "CardActivateResource" {
  rest_api_id = aws_api_gateway_rest_api.CardActivateApi.id
  parent_id   = aws_api_gateway_resource.CardResource.id
  path_part   = "activate"
}

# Método POST para /card/activate
resource "aws_api_gateway_method" "CardActivateMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CardActivateApi.id
  resource_id   = aws_api_gateway_resource.CardActivateResource.id
  http_method   = "POST"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

resource "aws_api_gateway_integration" "CardActivateIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CardActivateApi.id
  resource_id             = aws_api_gateway_resource.CardActivateResource.id
  http_method             = aws_api_gateway_method.CardActivateMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.card_activate_lambda.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwCardActivate" {
  statement_id  = "AllowExecutionFromAPIGatewayCardActivate"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.card_activate_lambda.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CardActivateApi.execution_arn}/*/POST/card/activate"
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "CardActivateApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.CardActivateApi.id
  depends_on = [aws_api_gateway_integration.CardActivateIntegration]
}

resource "aws_api_gateway_stage" "CardActivateApiStage" {
  deployment_id = aws_api_gateway_deployment.CardActivateApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.CardActivateApi.id
  stage_name    = "prod"
}

#=======================OUTPUTS=======================

output "card_activate_endpoint" {
  value = "${aws_api_gateway_stage.CardActivateApiStage.invoke_url}/card/activate"
}

#==========================API GATEWAY PARA REALIZAR TRANSACCIONES PURCHASE===========================
resource "aws_api_gateway_rest_api" "CardTransactionPurchaseApi" {
  name        = "card-transaction-purchase-api"
  description = "API para transacciones purchase"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso para /transactions/purchase
resource "aws_api_gateway_resource" "CardTransactionPurchaseResource" {
  rest_api_id = aws_api_gateway_rest_api.CardTransactionPurchaseApi.id
  parent_id   = aws_api_gateway_rest_api.CardTransactionPurchaseApi.root_resource_id
  path_part   = "purchase"
}

#=======================METODO HTTP=======================

resource "aws_api_gateway_method" "CardTransactionPurchaseMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CardTransactionPurchaseApi.id
  resource_id   = aws_api_gateway_resource.CardTransactionPurchaseResource.id
  http_method   = "POST"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

resource "aws_api_gateway_integration" "CardTransactionPurchaseIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CardTransactionPurchaseApi.id
  resource_id             = aws_api_gateway_resource.CardTransactionPurchaseResource.id
  http_method             = aws_api_gateway_method.CardTransactionPurchaseMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.CardPurchaseLmb.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwCardTransactionPurchase" {
  statement_id  = "AllowExecutionFromAPIGatewayCardTransactionPurchase"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CardPurchaseLmb.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CardTransactionPurchaseApi.execution_arn}/*/POST/purchase"
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "CardTransactionPurchaseApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.CardTransactionPurchaseApi.id
  depends_on = [
    aws_api_gateway_integration.CardTransactionPurchaseIntegration
  ]
}

resource "aws_api_gateway_stage" "CardTransactionPurchaseApiStage" {
  deployment_id = aws_api_gateway_deployment.CardTransactionPurchaseApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.CardTransactionPurchaseApi.id
  stage_name    = var.transaction_stage
}

#=======================OUTPUTS=======================

output "card_transaction_purchase_endpoint" {
  value = "${aws_api_gateway_stage.CardTransactionPurchaseApiStage.invoke_url}/${aws_api_gateway_resource.CardTransactionPurchaseResource.path_part}"
}

#==========================API GATEWAY PARA REALIZAR RECARGAS A DEBITO===========================
resource "aws_api_gateway_rest_api" "CardTransactionSaveApi" {
  name        = "card-transaction-save-api"
  description = "API para transacciones save a debito"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso para /transactions/purchase
resource "aws_api_gateway_resource" "CardTransactionSaveResource" {
  rest_api_id = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  parent_id   = aws_api_gateway_rest_api.CardTransactionSaveApi.root_resource_id
  path_part   = "save"
}

# Recurso para /transactions/save/{card_id}
resource "aws_api_gateway_resource" "CardTransactionSaveCardIdResource" {
  rest_api_id = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  parent_id   = aws_api_gateway_resource.CardTransactionSaveResource.id
  path_part   = "{card_id}"
}

#=======================METODO HTTP=======================

resource "aws_api_gateway_method" "CardTransactionSaveMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  resource_id   = aws_api_gateway_resource.CardTransactionSaveCardIdResource.id
  http_method   = "POST"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

resource "aws_api_gateway_integration" "CardTransactionSaveIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  resource_id             = aws_api_gateway_resource.CardTransactionSaveCardIdResource.id
  http_method             = aws_api_gateway_method.CardTransactionSaveMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.CardTransactionSaveLmb.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwCardTransactionSave" {
  statement_id  = "AllowExecutionFromAPIGatewayCardTransactionSave"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CardTransactionSaveLmb.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CardTransactionSaveApi.execution_arn}/*/POST/save/{card_id}"
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "CardTransactionSaveApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  depends_on = [
    aws_api_gateway_integration.CardTransactionSaveIntegration
  ]
}

resource "aws_api_gateway_stage" "CardTransactionSaveApiStage" {
  deployment_id = aws_api_gateway_deployment.CardTransactionSaveApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.CardTransactionSaveApi.id
  stage_name    = var.transaction_stage
}

#=======================OUTPUTS=======================

output "card_transaction_save_endpoint" {
  value = "${aws_api_gateway_stage.CardTransactionSaveApiStage.invoke_url}/${aws_api_gateway_resource.CardTransactionSaveResource.path_part}/${aws_api_gateway_resource.CardTransactionSaveCardIdResource.path_part}"
}


#==========================API GATEWAY PARA PAGAR CREDITO===========================
resource "aws_api_gateway_rest_api" "CardPaidApi" {
  name        = "card-transaction-paid-api"
  description = "API para pagar credito"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso para /transactions/purchase
resource "aws_api_gateway_resource" "CardPaidResource" {
  rest_api_id = aws_api_gateway_rest_api.CardPaidApi.id
  parent_id   = aws_api_gateway_rest_api.CardPaidApi.root_resource_id
  path_part   = "paid"
}

# Recurso para /transactions/save/{card_id}
resource "aws_api_gateway_resource" "CardPaidCardIdResource" {
  rest_api_id = aws_api_gateway_rest_api.CardPaidApi.id
  parent_id   = aws_api_gateway_resource.CardPaidResource.id
  path_part   = "{card_id}"
}

#=======================METODO HTTP=======================

resource "aws_api_gateway_method" "CardPaidMethod" {
  rest_api_id   = aws_api_gateway_rest_api.CardPaidApi.id
  resource_id   = aws_api_gateway_resource.CardPaidCardIdResource.id
  http_method   = "POST"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

resource "aws_api_gateway_integration" "CardPaidIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.CardPaidApi.id
  resource_id             = aws_api_gateway_resource.CardPaidCardIdResource.id
  http_method             = aws_api_gateway_method.CardPaidMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.CardPaidCreditLmb.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwCardPaid" {
  statement_id  = "AllowExecutionFromAPIGatewayCardPaid"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.CardPaidCreditLmb.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.CardPaidApi.execution_arn}/*/POST/paid/{card_id}" //no va el stage aqui
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "CardPaidApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.CardPaidApi.id
  depends_on = [
    aws_api_gateway_integration.CardPaidIntegration
  ]
}

resource "aws_api_gateway_stage" "CardPaidApiStage" {
  deployment_id = aws_api_gateway_deployment.CardPaidApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.CardPaidApi.id
  stage_name    = var.card_stage
}

#=======================OUTPUTS=======================

output "card_paid_endpoint" {
  value = "${aws_api_gateway_stage.CardPaidApiStage.invoke_url}/${aws_api_gateway_resource.CardPaidResource.path_part}/${aws_api_gateway_resource.CardPaidCardIdResource.path_part}"
}

#==========================API GATEWAY PARA OBTENER TARJETA===========================
resource "aws_api_gateway_rest_api" "GetCardApi" {
  name        = "get-card-api"
  description = "API para obtener informacion de tarjetas (crédito/débito)"
}

#=======================RECURSOS Y MÉTODOS=======================

# Recurso para /cards/request
resource "aws_api_gateway_resource" "GetCardResource" {
  rest_api_id = aws_api_gateway_rest_api.GetCardApi.id
  parent_id   = aws_api_gateway_rest_api.GetCardApi.root_resource_id
  path_part   = "profile"
}

# Recurso para /cards/request
resource "aws_api_gateway_resource" "GetCardIdResource" {
  rest_api_id = aws_api_gateway_rest_api.GetCardApi.id
  parent_id   = aws_api_gateway_resource.GetCardResource.id
  path_part   = "{card_id}"
}

#=======================MÉTODO HTTP=======================

# POST /cards/request
resource "aws_api_gateway_method" "GetCardMethod" {
  rest_api_id   = aws_api_gateway_rest_api.GetCardApi.id
  resource_id   = aws_api_gateway_resource.GetCardIdResource.id
  http_method   = "GET"
  authorization = "NONE"
}

#=======================INTEGRACIÓN CON LAMBDA=======================

# Integración para create-card-request-lambda
resource "aws_api_gateway_integration" "GetCardIntegration" {
  rest_api_id             = aws_api_gateway_rest_api.GetCardApi.id
  resource_id             = aws_api_gateway_resource.GetCardIdResource.id
  http_method             = aws_api_gateway_method.GetCardMethod.http_method
  integration_http_method = "POST"
  type                    = "AWS_PROXY"
  uri                     = aws_lambda_function.GetCardLmb.invoke_arn
}

#=======================PERMISOS LAMBDA=======================

resource "aws_lambda_permission" "ApiGwGetCard" {
  statement_id  = "AllowExecutionFromAPIGatewayGetCard"
  action        = "lambda:InvokeFunction"
  function_name = aws_lambda_function.GetCardLmb.function_name
  principal     = "apigateway.amazonaws.com"
  source_arn    = "${aws_api_gateway_rest_api.GetCardApi.execution_arn}/*/GET/profile/*"
}

#=======================DEPLOYMENT & STAGE=======================

resource "aws_api_gateway_deployment" "GetCardApiDeployment" {
  rest_api_id = aws_api_gateway_rest_api.GetCardApi.id
  depends_on = [aws_api_gateway_integration.GetCardIntegration]
}

resource "aws_api_gateway_stage" "GetCardApiStage" {
  deployment_id = aws_api_gateway_deployment.GetCardApiDeployment.id
  rest_api_id   = aws_api_gateway_rest_api.GetCardApi.id
  stage_name    = var.card_stage
}

#=======================OUTPUTS=======================

output "get_card_endpoint" {
  value = "${aws_api_gateway_stage.GetCardApiStage.invoke_url}/${aws_api_gateway_resource.GetCardResource.path_part}/${aws_api_gateway_resource.GetCardIdResource.path_part}"
}
