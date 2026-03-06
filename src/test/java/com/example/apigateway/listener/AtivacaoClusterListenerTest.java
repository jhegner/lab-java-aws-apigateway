package com.example.apigateway.listener;

import com.example.apigateway.core.AtivacaoCluster;
import com.example.apigateway.core.AtivacaoClusterDTO;
import com.example.apigateway.core.AtivacaoClusterEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AtivacaoClusterListenerTest {

    @Mock
    private AtivacaoCluster ativacaoCluster;

    @InjectMocks
    private AtivacaoClusterListener listener;

    @Test
    void onAtivacaoCluster_delegatesToService() {
        var dto   = new AtivacaoClusterDTO("alice", "CHG-42", "vpc-111", "lb.test");
        var event = new AtivacaoClusterEvent(dto);

        listener.onAtivacaoCluster(event);

        verify(ativacaoCluster).ativar(dto);
    }

    @Test
    void onAtivacaoCluster_propagatesExceptionFromService() {
        var dto   = new AtivacaoClusterDTO("alice", "CHG-42", "vpc-111", "lb.test");
        var event = new AtivacaoClusterEvent(dto);

        doThrow(new RuntimeException("service error"))
                .when(ativacaoCluster).ativar(dto);

        assertThatThrownBy(() -> listener.onAtivacaoCluster(event))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("service error");
    }

    @Test
    void event_nullDto_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new AtivacaoClusterEvent(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
