package br.com.fiap.challange.execucao.domain.port.in.command;

import br.com.fiap.challange.execucao.domain.model.ItemExecucao;

import java.util.List;

public record AgendarExecucaoCommand(String sagaId, Long ordemServicoId, String numeroOS, String descricaoProblema,
                                     List<ItemExecucao> itens, int prioridade) {
}
