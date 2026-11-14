package br.com.fiap.challange.execucao.domain.port.out;

import java.util.Map;

public interface EventosSagaPort {

    void publicar(String tipo, String sagaId, Long ordemServicoId, Map<String, Object> dados);
}
