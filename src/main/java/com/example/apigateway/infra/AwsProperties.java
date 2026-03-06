package com.example.apigateway.infra;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Typed configuration for both API Gateways (blue and green) and the AWS region.
 *
 * <pre>
 * aws:
 *   region: us-east-1
 *   apigateway:
 *     blue:
 *       id: abc123def4
 *       stage: v1
 *     green:
 *       id: xyz789uvw0
 *       stage: v1
 * </pre>
 */
@ConfigurationProperties(prefix = "aws")
public class AwsProperties {

    private final String region;
    private final ApiGatewayProperties apigateway;

    @ConstructorBinding
    public AwsProperties(String region, ApiGatewayProperties apigateway) {
        this.region = region;
        this.apigateway = apigateway;
    }

    public String getRegion() {
        return region;
    }

    public ApiGatewayProperties getApigateway() {
        return apigateway;
    }

    public static class ApiGatewayProperties {

        private final GatewayInstance blue;
        private final GatewayInstance green;

        @ConstructorBinding
        public ApiGatewayProperties(GatewayInstance blue, GatewayInstance green) {
            this.blue = blue;
            this.green = green;
        }

        public GatewayInstance getBlue() {
            return blue;
        }

        public GatewayInstance getGreen() {
            return green;
        }
    }

    public static class GatewayInstance {

        private final String id;
        private final String stage;

        @ConstructorBinding
        public GatewayInstance(String id, String stage) {
            this.id = id;
            this.stage = stage;
        }

        public String getId() {
            return id;
        }

        public String getStage() {
            return stage;
        }
    }
}
