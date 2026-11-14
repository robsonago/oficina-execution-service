package br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence;

import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity.MensagemProcessadaJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

interface MensagemProcessadaJpaRepository extends JpaRepository<MensagemProcessadaJpaEntity, String> {
}
