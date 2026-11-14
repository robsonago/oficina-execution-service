package br.com.fiap.challange.execucao.domain.port.out;

import br.com.fiap.challange.execucao.domain.model.Execucao;

import java.util.List;
import java.util.Optional;

public interface ExecucaoRepositoryPort {
    Execucao salvar(Execucao execucao);
    Optional<Execucao> buscarPorId(Long id);
    Optional<Execucao> buscarPorSagaId(String sagaId);
    List<Execucao> buscarPorOrdemServico(Long ordemServicoId);
    /** Execuções em andamento, da maior prioridade para a menor e por ordem de chegada. */
    List<Execucao> fila();
}
