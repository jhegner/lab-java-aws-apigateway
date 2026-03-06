# lab-java-aws-apigateway

Lab with Java and API Gateway

## Overview

Spring Boot 3.5.x / Maven application that updates the **VPC Link** and **NLB DNS** stage variables on two AWS API Gateways (blue and green) atomically.  
If the update on the second gateway fails the first is rolled back, leaving both gateways in their previous state.

## Architecture

```
listener/
  AtivacaoClusterListener   – @EventListener – receives AtivacaoClusterEvent, calls AtivacaoCluster.ativar()

core/
  AtivacaoCluster           – interface (primary port)
  AtivacaoClusterDTO        – Java record (usuario, codigoDeMudanca, vpcLink, nlbDns)
  AtivacaoClusterEvent      – Spring application event carrying AtivacaoClusterDTO
  AtivacaoClusterException  – unchecked exception for activation failures

service/
  AtivacaoClusterService    – implements AtivacaoCluster; validates, then delegates to ApiGateway

infra/
  ApiGateway                – @Component wrapping AWS SDK v2 (getStageVariables / updateStageVariables)
  AwsConfig                 – @Configuration providing ApiGatewayClient bean (DefaultCredentialsProvider)
  AwsProperties             – @ConfigurationProperties (aws.region, aws.apigateway.blue/green.id/stage)
```

## Configuration

| Property | Env var | Default |
|---|---|---|
| `aws.region` | `AWS_REGION` | `us-east-1` |
| `aws.apigateway.blue.id` | `AWS_APIGATEWAY_BLUE_ID` | `blue-api-id` |
| `aws.apigateway.blue.stage` | `AWS_APIGATEWAY_BLUE_STAGE` | `v1` |
| `aws.apigateway.green.id` | `AWS_APIGATEWAY_GREEN_ID` | `green-api-id` |
| `aws.apigateway.green.stage` | `AWS_APIGATEWAY_GREEN_STAGE` | `v1` |

## Running locally

```bash
# AWS credentials resolved from ~/.aws/credentials (or env vars)
export AWS_REGION=us-east-1
export AWS_APIGATEWAY_BLUE_ID=<your-blue-api-id>
export AWS_APIGATEWAY_GREEN_ID=<your-green-api-id>
mvn spring-boot:run
```

## Running in Kubernetes

The application uses `DefaultCredentialsProvider`, which automatically picks up IRSA (IAM Roles for Service Accounts) / EKS Pod Identity credentials injected as environment variables (`AWS_WEB_IDENTITY_TOKEN_FILE`, `AWS_ROLE_ARN`) by the EKS pod-identity mutating webhook. No code changes are needed.

## Triggering an activation

Publish an `AtivacaoClusterEvent` via `ApplicationEventPublisher`:

```java
@Autowired ApplicationEventPublisher publisher;

publisher.publishEvent(new AtivacaoClusterEvent(
    new AtivacaoClusterDTO("alice", "CHG-001", "vpc-0abc1234", "my-nlb.example.com")));
```

## Building & testing

```bash
mvn test          # unit tests (no AWS credentials required – all AWS calls are mocked)
mvn package       # creates target/apigateway-0.0.1-SNAPSHOT.jar
```
