package br.com.fiap.challange.execucao.infrastructure.metrics;

import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import br.com.fiap.challange.execucao.domain.port.out.ExecucaoRepositoryPort;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Tamanho da fila por status e mensagens presas na outbox (sinal de broker com problema). */
@Component
@RequiredArgsConstructor
public class ExecucaoMetrics {

    private final ExecucaoRepositoryPort repository;
    private final OutboxJpaRepository outbox;
    private final MeterRegistry registry;

    @PostConstruct
    void registrar() {
        for (StatusExecucao status : new StatusExecucao[]{StatusExecucao.NA_FILA, StatusExecucao.EM_DIAGNOSTICO,
                StatusExecucao.EM_REPARO}) {
            Gauge.builder("oficina.execucao.fila", this, m -> m.contar(status))
                    .tag("status", status.name())
                    .description("OS na fila de execução por status")
                    .register(registry);
        }
        Gauge.builder("oficina.outbox.pendentes", outbox, OutboxJpaRepository::countByPublicadaEmIsNull)
                .description("Eventos gravados e ainda não publicados no Pub/Sub")
                .register(registry);
    }

    private double contar(StatusExecucao status) {
        return repository.fila().stream().filter(e -> e.getStatus() == status).count();
    }
}
