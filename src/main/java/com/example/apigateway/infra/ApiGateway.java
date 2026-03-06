package com.example.apigateway.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.apigateway.ApiGatewayClient;
import software.amazon.awssdk.services.apigateway.model.GetStageRequest;
import software.amazon.awssdk.services.apigateway.model.GetStageResponse;
import software.amazon.awssdk.services.apigateway.model.Op;
import software.amazon.awssdk.services.apigateway.model.PatchOperation;
import software.amazon.awssdk.services.apigateway.model.UpdateStageRequest;

import java.util.Map;

/**
 * Infrastructure component that encapsulates all AWS API Gateway SDK v2 calls.
 * <p>
 * This class is intentionally kept thin: it translates domain requests into AWS
 * SDK calls and returns raw results.  Business logic (validation, rollback
 * orchestration) lives in the service layer.
 */
@Component
public class ApiGateway {

    private static final Logger log = LoggerFactory.getLogger(ApiGateway.class);

    /** Stage-variable key for the VPC Link ID. */
    public static final String VAR_VPC_LINK = "vpcLinkId";

    /** Stage-variable key for the NLB DNS name. */
    public static final String VAR_NLB_DNS = "nlbDns";

    private final ApiGatewayClient client;

    public ApiGateway(ApiGatewayClient client) {
        this.client = client;
    }

    /**
     * Returns the current stage-variable map for the given API Gateway and stage.
     *
     * @param restApiId  REST API identifier
     * @param stageName  stage name (e.g. {@code "v1"})
     * @return immutable copy of the current stage variables
     */
    public Map<String, String> getStageVariables(String restApiId, String stageName) {
        log.debug("Fetching stage variables – restApiId={}, stageName={}", restApiId, stageName);
        GetStageResponse response = client.getStage(
                GetStageRequest.builder()
                        .restApiId(restApiId)
                        .stageName(stageName)
                        .build());
        return Map.copyOf(response.variables());
    }

    /**
     * Updates the {@code vpcLinkId} and {@code nlbDns} stage variables on a
     * single API Gateway stage.
     *
     * @param restApiId  REST API identifier
     * @param stageName  stage name
     * @param vpcLinkId  new VPC Link ID value
     * @param nlbDns     new NLB DNS value
     */
    public void updateStageVariables(String restApiId, String stageName,
                                     String vpcLinkId, String nlbDns) {
        log.debug("Updating stage variables – restApiId={}, stageName={}, vpcLinkId={}, nlbDns={}",
                restApiId, stageName, vpcLinkId, nlbDns);
        client.updateStage(UpdateStageRequest.builder()
                .restApiId(restApiId)
                .stageName(stageName)
                .patchOperations(
                        PatchOperation.builder()
                                .op(Op.REPLACE)
                                .path("/variables/" + VAR_VPC_LINK)
                                .value(vpcLinkId)
                                .build(),
                        PatchOperation.builder()
                                .op(Op.REPLACE)
                                .path("/variables/" + VAR_NLB_DNS)
                                .value(nlbDns)
                                .build())
                .build());
        log.info("Stage variables updated – restApiId={}, stageName={}", restApiId, stageName);
    }
}
