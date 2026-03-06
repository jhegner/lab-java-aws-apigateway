package com.example.apigateway.service;

import com.example.apigateway.core.AtivacaoClusterDTO;
import com.example.apigateway.core.AtivacaoClusterException;
import com.example.apigateway.infra.ApiGateway;
import com.example.apigateway.infra.AwsProperties;
import com.example.apigateway.infra.AwsProperties.ApiGatewayProperties;
import com.example.apigateway.infra.AwsProperties.GatewayInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static com.example.apigateway.infra.ApiGateway.VAR_NLB_DNS;
import static com.example.apigateway.infra.ApiGateway.VAR_VPC_LINK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtivacaoClusterServiceTest {

    private static final String BLUE_ID    = "blue-api-id";
    private static final String BLUE_STAGE = "v1";
    private static final String GREEN_ID   = "green-api-id";
    private static final String GREEN_STAGE = "v1";

    @Mock
    private ApiGateway apiGateway;

    private AtivacaoClusterService service;

    @BeforeEach
    void setUp() {
        GatewayInstance blue  = new GatewayInstance(BLUE_ID, BLUE_STAGE);
        GatewayInstance green = new GatewayInstance(GREEN_ID, GREEN_STAGE);
        AwsProperties props = new AwsProperties("us-east-1",
                new ApiGatewayProperties(blue, green));
        service = new AtivacaoClusterService(apiGateway, props);
    }

    // -------------------------------------------------------------------------
    // Validation tests
    // -------------------------------------------------------------------------

    @Test
    void ativar_nullDto_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.ativar(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void ativar_missingUsuario_throwsIllegalArgumentException() {
        var dto = new AtivacaoClusterDTO("", "CHG001", "vpc-123", "my.nlb.dns");
        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("usuario");
    }

    @Test
    void ativar_missingCodigoDeMudanca_throwsIllegalArgumentException() {
        var dto = new AtivacaoClusterDTO("john", null, "vpc-123", "my.nlb.dns");
        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("codigoDeMudanca");
    }

    @Test
    void ativar_missingVpcLink_throwsIllegalArgumentException() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "  ", "my.nlb.dns");
        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vpcLink");
    }

    @Test
    void ativar_missingNlbDns_throwsIllegalArgumentException() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "vpc-123", "");
        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nlbDns");
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    void ativar_validDto_updatesBothGateways() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "vpc-abc", "lb.example.com");

        when(apiGateway.getStageVariables(BLUE_ID,  BLUE_STAGE)).thenReturn(Map.of());
        when(apiGateway.getStageVariables(GREEN_ID, GREEN_STAGE)).thenReturn(Map.of());

        service.ativar(dto);

        verify(apiGateway).updateStageVariables(BLUE_ID,  BLUE_STAGE, "vpc-abc", "lb.example.com");
        verify(apiGateway).updateStageVariables(GREEN_ID, GREEN_STAGE, "vpc-abc", "lb.example.com");
    }

    // -------------------------------------------------------------------------
    // Rollback tests
    // -------------------------------------------------------------------------

    @Test
    void ativar_blueUpdateFails_throwsAndDoesNotUpdateGreen() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "vpc-abc", "lb.example.com");

        when(apiGateway.getStageVariables(anyString(), anyString())).thenReturn(Map.of());
        doThrow(new RuntimeException("blue error"))
                .when(apiGateway).updateStageVariables(eq(BLUE_ID), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(AtivacaoClusterException.class)
                .hasMessageContaining("blue");

        // green must NOT have been updated
        verify(apiGateway, never()).updateStageVariables(eq(GREEN_ID), anyString(), anyString(), anyString());
    }

    @Test
    void ativar_greenUpdateFails_rollsBackBlue() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "vpc-new", "new.nlb.dns");

        String prevVpc = "vpc-old";
        String prevDns = "old.nlb.dns";
        when(apiGateway.getStageVariables(BLUE_ID,  BLUE_STAGE))
                .thenReturn(Map.of(VAR_VPC_LINK, prevVpc, VAR_NLB_DNS, prevDns));
        when(apiGateway.getStageVariables(GREEN_ID, GREEN_STAGE)).thenReturn(Map.of());

        doNothing().when(apiGateway)
                .updateStageVariables(eq(BLUE_ID), anyString(), anyString(), anyString());
        doThrow(new RuntimeException("green error"))
                .when(apiGateway).updateStageVariables(eq(GREEN_ID), anyString(), anyString(), anyString());

        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(AtivacaoClusterException.class)
                .hasMessageContaining("green")
                .hasMessageContaining("rolled back");

        // Blue should have been rolled back to previous values
        verify(apiGateway).updateStageVariables(BLUE_ID, BLUE_STAGE, prevVpc, prevDns);
    }

    @Test
    void ativar_greenUpdateFailsAndRollbackAlsoFails_throwsOriginalException() {
        var dto = new AtivacaoClusterDTO("john", "CHG001", "vpc-new", "new.nlb.dns");

        when(apiGateway.getStageVariables(anyString(), anyString())).thenReturn(Map.of());

        // Blue update succeeds first call; rollback (second call) also throws
        doNothing()
                .doThrow(new RuntimeException("rollback error"))
                .when(apiGateway).updateStageVariables(eq(BLUE_ID), anyString(), anyString(), anyString());
        doThrow(new RuntimeException("green error"))
                .when(apiGateway).updateStageVariables(eq(GREEN_ID), anyString(), anyString(), anyString());

        // Exception from green update is still propagated even if rollback fails
        assertThatThrownBy(() -> service.ativar(dto))
                .isInstanceOf(AtivacaoClusterException.class)
                .hasMessageContaining("green");
    }
}
