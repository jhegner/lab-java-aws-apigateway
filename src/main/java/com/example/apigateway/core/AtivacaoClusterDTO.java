package com.example.apigateway.core;

/**
 * DTO carrying all the information required to activate a cluster
 * (switch VPC Link and NLB DNS on the API Gateway stage variables).
 *
 * @param usuario          user / requester identifier (mandatory)
 * @param codigoDeMudanca  change-ticket / change-order code (mandatory)
 * @param vpcLink          new VPC Link ID to set on both API Gateways (mandatory)
 * @param nlbDns           new NLB DNS name to set on both API Gateways (mandatory)
 */
public record AtivacaoClusterDTO(
        String usuario,
        String codigoDeMudanca,
        String vpcLink,
        String nlbDns) {
}
