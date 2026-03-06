package com.example.apigateway.core;

/**
 * Unchecked exception thrown when the cluster-activation operation fails.
 */
public class AtivacaoClusterException extends RuntimeException {

    public AtivacaoClusterException(String message) {
        super(message);
    }

    public AtivacaoClusterException(String message, Throwable cause) {
        super(message, cause);
    }
}
