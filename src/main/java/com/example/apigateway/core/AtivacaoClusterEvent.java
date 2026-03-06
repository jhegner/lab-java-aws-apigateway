package com.example.apigateway.core;

/**
 * Simple application event that signals the need to activate a cluster
 * (update VPC Link and NLB DNS on both API Gateways).
 * <p>
 * Publish this event via Spring's {@code ApplicationEventPublisher} and the
 * {@link com.example.apigateway.listener.AtivacaoClusterListener} will pick it
 * up and delegate to {@link AtivacaoCluster#ativar(AtivacaoClusterDTO)}.
 */
public class AtivacaoClusterEvent {

    private final AtivacaoClusterDTO dto;

    public AtivacaoClusterEvent(AtivacaoClusterDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }
        this.dto = dto;
    }

    public AtivacaoClusterDTO getDto() {
        return dto;
    }
}
