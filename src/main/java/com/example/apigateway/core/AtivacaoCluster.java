package com.example.apigateway.core;

/**
 * Port (primary port) for the cluster-activation use-case.
 * <p>
 * The implementation must update the {@code vpcLinkId} and {@code nlbDns}
 * stage variables on <em>both</em> configured API Gateways atomically:
 * if the second update fails the first must be rolled back.
 */
public interface AtivacaoCluster {

    /**
     * Activates the cluster by updating the VPC Link and NLB DNS
     * stage variables on both blue and green API Gateways.
     *
     * @param dto request payload – must not be {@code null}
     * @throws IllegalArgumentException if required fields are missing
     * @throws AtivacaoClusterException if the AWS update fails and cannot be rolled back
     */
    void ativar(AtivacaoClusterDTO dto);
}
