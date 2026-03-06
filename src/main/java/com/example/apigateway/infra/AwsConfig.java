package com.example.apigateway.infra;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;

/**
 * AWS infrastructure configuration.
 * <p>
 * {@link DefaultCredentialsProvider} is used so that the application works:
 * <ul>
 *   <li>locally – via {@code ~/.aws/credentials} or environment variables</li>
 *   <li>in Kubernetes – via IRSA (IAM Roles for Service Accounts) or
 *       EKS Pod Identity, both of which inject a web-identity token that
 *       {@code DefaultCredentialsProvider} picks up automatically</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsConfig {

    @Bean
    public ApiGatewayClient apiGatewayClient(AwsProperties props) {
        return ApiGatewayClient.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
