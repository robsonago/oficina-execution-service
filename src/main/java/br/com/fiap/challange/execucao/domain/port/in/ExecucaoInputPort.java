package br.com.fiap.challange.execucao.domain.port.in;

import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.port.in.command.AgendarExecucaoCommand;

import java.util.List;
import java.util.Optional;

public interface ExecucaoInputPort {
    Execucao agendar(AgendarExecucaoCommand command);
    List<Execucao> fila();
    Execucao buscar(Long id);
    List<Execucao> buscarPorOrdemServico(Long ordemServicoId);
    Optional<Execucao> puxarProxima(String responsavel);
    Execucao iniciarDiagnostico(Long id, String responsavel);
    Execucao iniciarReparo(Long id, String responsavel);
    Execucao finalizar(Long id, String responsavel, String observacoes);
    Execucao registrarFalha(Long id, String responsavel, String motivo);
}
