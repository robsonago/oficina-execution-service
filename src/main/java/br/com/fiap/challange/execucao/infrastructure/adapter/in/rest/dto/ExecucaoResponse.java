package br.com.fiap.challange.execucao.infrastructure.adapter.in.rest.dto;

import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.model.HistoricoExecucao;
import br.com.fiap.challange.execucao.domain.model.ItemExecucao;
import br.com.fiap.challange.execucao.domain.model.StatusExecucao;

import java.time.LocalDateTime;
import java.util.List;

public record ExecucaoResponse(Long id, Long ordemServicoId, String numeroOS, String sagaId, StatusExecucao status,
                               String descricaoProblema, int prioridade, String responsavel, String observacoes,
                               String motivoFalha, List<ItemExecucao> itens, List<HistoricoExecucao> historico,
                               LocalDateTime entradaFilaEm, LocalDateTime inicioEm, LocalDateTime fimEm) {

    public static ExecucaoResponse from(Execucao e) {
        return new ExecucaoResponse(e.getId(), e.getOrdemServicoId(), e.getNumeroOS(), e.getSagaId(), e.getStatus(),
                e.getDescricaoProblema(), e.getPrioridade(), e.getResponsavel(), e.getObservacoes(),
                e.getMotivoFalha(), e.getItens(), e.getHistorico(), e.getEntradaFilaEm(), e.getInicioEm(), e.getFimEm());
    }
}
