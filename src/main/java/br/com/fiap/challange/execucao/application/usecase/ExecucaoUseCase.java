package br.com.fiap.challange.execucao.application.usecase;

import br.com.fiap.challange.execucao.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import br.com.fiap.challange.execucao.domain.port.in.ExecucaoInputPort;
import br.com.fiap.challange.execucao.domain.port.in.command.AgendarExecucaoCommand;
import br.com.fiap.challange.execucao.domain.port.out.EventosSagaPort;
import br.com.fiap.challange.execucao.domain.port.out.ExecucaoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.fiap.challange.execucao.domain.model.TiposMensagem.*;

/**
 * Cada mudança de status gera um evento para o OS Service. Como o evento vai
 * para a tabela de outbox na mesma transação da execução, ou os dois são
 * gravados ou nenhum é — não existe "mudou o status mas não avisou".
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ExecucaoUseCase implements ExecucaoInputPort {

    private final ExecucaoRepositoryPort repository;
    private final EventosSagaPort eventos;

    @Override
    public Execucao agendar(AgendarExecucaoCommand command) {
        Optional<Execucao> existente = repository.buscarPorSagaId(command.sagaId());
        if (existente.isPresent()) {
            log.info("OS {} já estava na fila (saga {})", command.numeroOS(), command.sagaId());
            return existente.get();
        }
        Execucao execucao = repository.salvar(Execucao.naFila(command.ordemServicoId(), command.numeroOS(),
                command.sagaId(), command.descricaoProblema(), command.itens(), command.prioridade()));
        log.info("OS {} entrou na fila de execução id={}", execucao.getNumeroOS(), execucao.getId());
        return execucao;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Execucao> fila() {
        return repository.fila();
    }

    @Override
    @Transactional(readOnly = true)
    public Execucao buscar(Long id) {
        return carregar(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Execucao> buscarPorOrdemServico(Long ordemServicoId) {
        return repository.buscarPorOrdemServico(ordemServicoId);
    }

    @Override
    public Optional<Execucao> puxarProxima(String responsavel) {
        return repository.fila().stream()
                .filter(e -> e.getStatus() == StatusExecucao.NA_FILA)
                .findFirst()
                .map(e -> iniciarDiagnostico(e.getId(), responsavel));
    }

    @Override
    public Execucao iniciarDiagnostico(Long id, String responsavel) {
        Execucao execucao = carregar(id);
        execucao.iniciarDiagnostico(responsavel);
        return salvarEAvisar(execucao, EXECUCAO_ATUALIZADA, Map.of());
    }

    @Override
    public Execucao iniciarReparo(Long id, String responsavel) {
        Execucao execucao = carregar(id);
        execucao.iniciarReparo(responsavel);
        return salvarEAvisar(execucao, EXECUCAO_ATUALIZADA, Map.of());
    }

    @Override
    public Execucao finalizar(Long id, String responsavel, String observacoes) {
        Execucao execucao = carregar(id);
        execucao.finalizar(responsavel, observacoes);
        return salvarEAvisar(execucao, EXECUCAO_FINALIZADA,
                observacoes == null ? Map.of() : Map.of("observacoes", observacoes));
    }

    @Override
    public Execucao registrarFalha(Long id, String responsavel, String motivo) {
        Execucao execucao = carregar(id);
        execucao.falhar(responsavel, motivo);
        return salvarEAvisar(execucao, EXECUCAO_FALHOU, Map.of("motivo", motivo));
    }

    private Execucao salvarEAvisar(Execucao execucao, String tipo, Map<String, Object> extras) {
        Execucao salva = repository.salvar(execucao);
        Map<String, Object> dados = new HashMap<>(extras);
        dados.put("execucaoId", salva.getId());
        dados.put("statusExecucao", salva.getStatus().name());
        if (salva.getResponsavel() != null) {
            dados.put("responsavel", salva.getResponsavel());
        }
        eventos.publicar(tipo, salva.getSagaId(), salva.getOrdemServicoId(), dados);
        log.info("Execução {} da OS {} agora está {}", salva.getId(), salva.getNumeroOS(), salva.getStatus());
        return salva;
    }

    private Execucao carregar(Long id) {
        return repository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Execução não encontrada: " + id));
    }
}
