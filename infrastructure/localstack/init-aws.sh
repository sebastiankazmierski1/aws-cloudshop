#!/bin/bash
# LocalStack initialization script
# Creates AWS resources for local development

set -e

echo "============================================"
echo "Initializing LocalStack AWS Resources"
echo "============================================"

# Wait for LocalStack to be ready
echo "Waiting for LocalStack..."
sleep 5

# Create S3 bucket for product images
echo "Creating S3 bucket for product images..."
awslocal s3 mb s3://cloudshop-product-images-local

# Set bucket policy to allow public read (for development)
awslocal s3api put-bucket-policy --bucket cloudshop-product-images-local --policy '{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::cloudshop-product-images-local/*"
    }
  ]
}'

echo "S3 bucket created: cloudshop-product-images-local"

# Create SQS queues (for future weeks)
echo "Creating SQS queues..."
awslocal sqs create-queue --queue-name order-processing-queue
awslocal sqs create-queue --queue-name order-processing-dlq

echo "SQS queues created"

# Create SNS topics (for future weeks)
echo "Creating SNS topics..."
awslocal sns create-topic --name order-notifications
awslocal sns create-topic --name inventory-alerts

echo "SNS topics created"

# Create DynamoDB tables (for future weeks)
echo "Creating DynamoDB tables..."
awslocal dynamodb create-table \
    --table-name Orders \
    --attribute-definitions \
        AttributeName=customerId,AttributeType=S \
        AttributeName=orderId,AttributeType=S \
        AttributeName=status,AttributeType=S \
    --key-schema \
        AttributeName=customerId,KeyType=HASH \
        AttributeName=orderId,KeyType=RANGE \
    --global-secondary-indexes \
        "[{\"IndexName\": \"status-index\",\"KeySchema\":[{\"AttributeName\":\"status\",\"KeyType\":\"HASH\"}],\"Projection\":{\"ProjectionType\":\"ALL\"},\"ProvisionedThroughput\":{\"ReadCapacityUnits\":5,\"WriteCapacityUnits\":5}}]" \
    --provisioned-throughput \
        ReadCapacityUnits=5,WriteCapacityUnits=5

echo "DynamoDB tables created"

# Create Secrets Manager secrets
echo "Creating Secrets Manager secrets..."
awslocal secretsmanager create-secret \
    --name cloudshop/database \
    --secret-string '{"username":"cloudshop","password":"cloudshop123","host":"postgres","port":"5432","dbname":"cloudshop"}'

echo "Secrets created"

echo "============================================"
echo "LocalStack initialization complete!"
echo "============================================"
echo ""
echo "Available resources:"
echo "  S3 Bucket: cloudshop-product-images-local"
echo "  SQS Queues: order-processing-queue, order-processing-dlq"
echo "  SNS Topics: order-notifications, inventory-alerts"
echo "  DynamoDB: Orders table"
echo "  Secrets: cloudshop/database"
echo ""
echo "LocalStack endpoint: http://localhost:4566"
