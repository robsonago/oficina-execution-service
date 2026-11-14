package br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity.OutboxJpaEntity;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxJpaRepository extends JpaRepository<OutboxJpaEntity, String> {
    List<OutboxJpaEntity> findByPublicadaEmIsNullOrderByCriadaEmAsc(Limit limite);
    long countByPublicadaEmIsNull();
}
