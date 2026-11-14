package br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.model.HistoricoExecucao;
import br.com.fiap.challange.execucao.domain.model.ItemExecucao;
import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import br.com.fiap.challange.execucao.domain.port.out.ExecucaoRepositoryPort;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity.ExecucaoJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
class ExecucaoRepositoryAdapter implements ExecucaoRepositoryPort {

    private final ExecucaoJpaRepository jpa;

    @Override
    public Execucao salvar(Execucao execucao) {
        ExecucaoJpaEntity entity = execucao.getId() == null
                ? new ExecucaoJpaEntity()
                : jpa.findById(execucao.getId()).orElseGet(ExecucaoJpaEntity::new);
        copiar(execucao, entity);
        return paraDominio(jpa.save(entity));
    }

    @Override
    public Optional<Execucao> buscarPorId(Long id) {
        return jpa.findById(id).map(ExecucaoRepositoryAdapter::paraDominio);
    }

    @Override
    public Optional<Execucao> buscarPorSagaId(String sagaId) {
        return jpa.findBySagaId(sagaId).map(ExecucaoRepositoryAdapter::paraDominio);
    }

    @Override
    public List<Execucao> buscarPorOrdemServico(Long ordemServicoId) {
        return jpa.findByOrdemServicoIdOrderByEntradaFilaEmDesc(ordemServicoId).stream()
                .map(ExecucaoRepositoryAdapter::paraDominio).toList();
    }

    @Override
    public List<Execucao> fila() {
        return jpa.findByStatusInOrderByPrioridadeDescEntradaFilaEmAsc(
                        EnumSet.of(StatusExecucao.NA_FILA, StatusExecucao.EM_DIAGNOSTICO, StatusExecucao.EM_REPARO))
                .stream().map(ExecucaoRepositoryAdapter::paraDominio).toList();
    }

    private static void copiar(Execucao e, ExecucaoJpaEntity j) {
        j.setOrdemServicoId(e.getOrdemServicoId());
        j.setNumeroOS(e.getNumeroOS());
        j.setSagaId(e.getSagaId());
        j.setStatus(e.getStatus());
        j.setDescricaoProblema(e.getDescricaoProblema());
        j.setPrioridade(e.getPrioridade());
        j.setResponsavel(e.getResponsavel());
        j.setObservacoes(e.getObservacoes());
        j.setMotivoFalha(e.getMotivoFalha());
        j.setEntradaFilaEm(e.getEntradaFilaEm());
        j.setInicioEm(e.getInicioEm());
        j.setFimEm(e.getFimEm());
        j.getItens().clear();
        for (ItemExecucao item : e.getItens()) {
            ExecucaoJpaEntity.ItemEmbeddable emb = new ExecucaoJpaEntity.ItemEmbeddable();
            emb.setTipo(item.tipo());
            emb.setDescricao(item.descricao());
            emb.setQuantidade(item.quantidade());
            j.getItens().add(emb);
        }
        j.getHistorico().clear();
        for (HistoricoExecucao h : e.getHistorico()) {
            ExecucaoJpaEntity.HistoricoEmbeddable emb = new ExecucaoJpaEntity.HistoricoEmbeddable();
            emb.setStatus(h.status());
            emb.setDescricao(h.descricao());
            emb.setResponsavel(h.responsavel());
            emb.setOcorridoEm(h.ocorridoEm());
            j.getHistorico().add(emb);
        }
    }

    private static Execucao paraDominio(ExecucaoJpaEntity j) {
        return Execucao.builder()
                .id(j.getId())
                .ordemServicoId(j.getOrdemServicoId())
                .numeroOS(j.getNumeroOS())
                .sagaId(j.getSagaId())
                .status(j.getStatus())
                .descricaoProblema(j.getDescricaoProblema())
                .prioridade(j.getPrioridade())
                .responsavel(j.getResponsavel())
                .observacoes(j.getObservacoes())
                .motivoFalha(j.getMotivoFalha())
                .entradaFilaEm(j.getEntradaFilaEm())
                .inicioEm(j.getInicioEm())
                .fimEm(j.getFimEm())
                .itens(new ArrayList<>(j.getItens().stream()
                        .map(i -> new ItemExecucao(i.getTipo(), i.getDescricao(), i.getQuantidade())).toList()))
                .historico(new ArrayList<>(j.getHistorico().stream()
                        .map(h -> new HistoricoExecucao(h.getStatus(), h.getDescricao(), h.getResponsavel(), h.getOcorridoEm()))
                        .toList()))
                .build();
    }
}
