#!/bin/bash
set -e
echo "Starting deployment..."

echo "Building angular app..."
cd app
ng build --configuration=production
cd ..

echo "Getting infrastructure details..."
BUCKET_NAME=$(terraform output -raw s3_bucket_name)
DISTRIBUTION_ID=$(terraform output -raw cloudfront_distribution_id)

echo "S3 Bucket: $BUCKET_NAME"
echo "CloudFront ID: $DISTRIBUTION_ID"

echo "Uploading to s3 ... "
# Ajusta la ruta según tu build output real
aws s3 sync ./app/dist/myfront/browser/ s3://$BUCKET_NAME --delete

echo "Invalidating CloudFront cache..."
aws cloudfront create-invalidation \
  --distribution-id $DISTRIBUTION_ID \
  --paths "/*"

echo "Deployment completed!"
echo "Your app is alive at https://$(terraform output -raw cloudfront_domain_name)"
