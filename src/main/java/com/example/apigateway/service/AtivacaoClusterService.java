package com.example.apigateway.service;

import com.example.apigateway.core.AtivacaoCluster;
import com.example.apigateway.core.AtivacaoClusterDTO;
import com.example.apigateway.core.AtivacaoClusterException;
import com.example.apigateway.infra.ApiGateway;
import com.example.apigateway.infra.AwsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Service that implements the cluster-activation use-case.
 *
 * <h3>Steps</h3>
 * <ol>
 *   <li>Validate mandatory fields in {@link AtivacaoClusterDTO}.</li>
 *   <li>Capture the current stage variables from <em>both</em> API Gateways
 *       (blue and green) so they can be restored if necessary.</li>
 *   <li>Update the blue API Gateway.</li>
 *   <li>Update the green API Gateway.
 *       If this step fails, the blue API Gateway is rolled back to its
 *       previous state before re-throwing.</li>
 * </ol>
 */
@Service
public class AtivacaoClusterService implements AtivacaoCluster {

    private static final Logger log = LoggerFactory.getLogger(AtivacaoClusterService.class);

    private final ApiGateway apiGateway;
    private final AwsProperties props;

    public AtivacaoClusterService(ApiGateway apiGateway, AwsProperties props) {
        this.apiGateway = apiGateway;
        this.props = props;
    }

    @Override
    public void ativar(AtivacaoClusterDTO dto) {
        validate(dto);

        String blueId    = props.getApigateway().getBlue().getId();
        String blueStage = props.getApigateway().getBlue().getStage();
        String greenId   = props.getApigateway().getGreen().getId();
        String greenStage = props.getApigateway().getGreen().getStage();

        log.info("Starting cluster activation – usuario={}, codigoDeMudanca={}",
                dto.usuario(), dto.codigoDeMudanca());

        // Step 1 – capture current state for potential rollback
        Map<String, String> previousBlue  = apiGateway.getStageVariables(blueId,  blueStage);
        Map<String, String> previousGreen = apiGateway.getStageVariables(greenId, greenStage);

        // Step 2 – update blue
        try {
            apiGateway.updateStageVariables(blueId, blueStage, dto.vpcLink(), dto.nlbDns());
        } catch (Exception ex) {
            throw new AtivacaoClusterException(
                    "Failed to update blue API Gateway (restApiId=" + blueId + "): " + ex.getMessage(), ex);
        }

        // Step 3 – update green; rollback blue on failure
        try {
            apiGateway.updateStageVariables(greenId, greenStage, dto.vpcLink(), dto.nlbDns());
        } catch (Exception greenEx) {
            log.error("Failed to update green API Gateway – initiating rollback of blue", greenEx);
            rollback(blueId, blueStage, previousBlue);
            throw new AtivacaoClusterException(
                    "Failed to update green API Gateway (restApiId=" + greenId
                            + "). Blue API Gateway has been rolled back.", greenEx);
        }

        log.info("Cluster activation successful – usuario={}, codigoDeMudanca={}",
                dto.usuario(), dto.codigoDeMudanca());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void validate(AtivacaoClusterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("AtivacaoClusterDTO must not be null");
        }
        if (isBlank(dto.usuario())) {
            throw new IllegalArgumentException("Field 'usuario' is mandatory");
        }
        if (isBlank(dto.codigoDeMudanca())) {
            throw new IllegalArgumentException("Field 'codigoDeMudanca' is mandatory");
        }
        if (isBlank(dto.vpcLink())) {
            throw new IllegalArgumentException("Field 'vpcLink' is mandatory");
        }
        if (isBlank(dto.nlbDns())) {
            throw new IllegalArgumentException("Field 'nlbDns' is mandatory");
        }
    }

    private void rollback(String restApiId, String stageName, Map<String, String> previous) {
        String prevVpcLink = previous.getOrDefault(ApiGateway.VAR_VPC_LINK, "");
        String prevNlbDns  = previous.getOrDefault(ApiGateway.VAR_NLB_DNS,  "");
        try {
            apiGateway.updateStageVariables(restApiId, stageName, prevVpcLink, prevNlbDns);
            log.info("Rollback successful – restApiId={}, stageName={}", restApiId, stageName);
        } catch (Exception rollbackEx) {
            log.error("Rollback FAILED for restApiId={}, stageName={} – manual intervention required",
                    restApiId, stageName, rollbackEx);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
