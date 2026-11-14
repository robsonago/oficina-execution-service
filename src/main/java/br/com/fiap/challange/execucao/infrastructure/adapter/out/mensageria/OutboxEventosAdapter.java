package br.com.fiap.challange.execucao.infrastructure.adapter.out.mensageria;

import br.com.fiap.challange.execucao.domain.port.out.EventosSagaPort;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity.OutboxJpaEntity;
import br.com.fiap.challange.execucao.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.execucao.infrastructure.messaging.Rastreamento;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Não publica direto no broker: grava na tabela de outbox, dentro da
 * transação de quem chamou. O {@link RelayOutbox} publica depois.
 * Os cabeçalhos de rastreamento são capturados agora, enquanto ainda estamos
 * dentro da requisição HTTP que originou o evento.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventosAdapter implements EventosSagaPort {

    static final String DESTINO = "os-eventos";

    private final OutboxJpaRepository outbox;
    private final ObjectMapper objectMapper;

    @Override
    public void publicar(String tipo, String sagaId, Long ordemServicoId, Map<String, Object> dados) {
        Mensagem mensagem = Mensagem.nova(tipo, sagaId, ordemServicoId, objectMapper.valueToTree(dados));
        OutboxJpaEntity registro = new OutboxJpaEntity();
        registro.setId(mensagem.id());
        registro.setDestino(DESTINO);
        registro.setTipo(tipo);
        registro.setSagaId(sagaId);
        try {
            registro.setConteudo(objectMapper.writeValueAsString(mensagem));
            registro.setCabecalhos(objectMapper.writeValueAsString(Rastreamento.cabecalhosAtuais(sagaId)));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar o evento " + tipo, e);
        }
        registro.setCriadaEm(LocalDateTime.now());
        outbox.save(registro);
    }
}
