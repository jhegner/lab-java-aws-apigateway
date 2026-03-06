package com.example.apigateway.listener;

import com.example.apigateway.core.AtivacaoCluster;
import com.example.apigateway.core.AtivacaoClusterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link AtivacaoClusterEvent} published anywhere inside the
 * Spring application context and delegates the activation to
 * {@link AtivacaoCluster}.
 * <p>
 * In production the event can be published via
 * {@code ApplicationEventPublisher#publishEvent(Object)} from any bean
 * (e.g. a REST controller, an SQS consumer, a scheduled job, etc.).
 */
@Component
public class AtivacaoClusterListener {

    private static final Logger log = LoggerFactory.getLogger(AtivacaoClusterListener.class);

    private final AtivacaoCluster ativacaoCluster;

    public AtivacaoClusterListener(AtivacaoCluster ativacaoCluster) {
        this.ativacaoCluster = ativacaoCluster;
    }

    @EventListener
    public void onAtivacaoCluster(AtivacaoClusterEvent event) {
        log.info("AtivacaoClusterEvent received – usuario={}, codigoDeMudanca={}",
                event.getDto().usuario(), event.getDto().codigoDeMudanca());
        ativacaoCluster.ativar(event.getDto());
        log.info("Cluster activation completed – codigoDeMudanca={}",
                event.getDto().codigoDeMudanca());
    }
}
