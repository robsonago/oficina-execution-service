package br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity.ExecucaoJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

interface ExecucaoJpaRepository extends JpaRepository<ExecucaoJpaEntity, Long> {
    Optional<ExecucaoJpaEntity> findBySagaId(String sagaId);
    List<ExecucaoJpaEntity> findByOrdemServicoIdOrderByEntradaFilaEmDesc(Long ordemServicoId);
    List<ExecucaoJpaEntity> findByStatusInOrderByPrioridadeDescEntradaFilaEmAsc(Collection<StatusExecucao> status);
}
