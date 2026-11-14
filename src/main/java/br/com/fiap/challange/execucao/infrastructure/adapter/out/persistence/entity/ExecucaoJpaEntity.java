package br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.entity;

import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "execucoes")
@Getter
@Setter
public class ExecucaoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ordem_servico_id", nullable = false)
    private Long ordemServicoId;

    @Column(name = "numero_os", nullable = false, length = 50)
    private String numeroOS;

    @Column(name = "saga_id", nullable = false, unique = true, length = 64)
    private String sagaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusExecucao status;

    @Column(name = "descricao_problema", columnDefinition = "TEXT")
    private String descricaoProblema;

    @Column(nullable = false)
    private int prioridade;

    @Column(length = 100)
    private String responsavel;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "motivo_falha", columnDefinition = "TEXT")
    private String motivoFalha;

    @Column(name = "entrada_fila_em", nullable = false)
    private LocalDateTime entradaFilaEm;

    @Column(name = "inicio_em")
    private LocalDateTime inicioEm;

    @Column(name = "fim_em")
    private LocalDateTime fimEm;

    @Version
    private Long versao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "execucao_itens", joinColumns = @JoinColumn(name = "execucao_id"))
    @OrderColumn(name = "posicao")
    private List<ItemEmbeddable> itens = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "execucao_historico", joinColumns = @JoinColumn(name = "execucao_id"))
    @OrderColumn(name = "posicao")
    private List<HistoricoEmbeddable> historico = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    public static class ItemEmbeddable {
        @Column(nullable = false, length = 20)
        private String tipo;
        @Column(nullable = false)
        private String descricao;
        @Column(nullable = false)
        private int quantidade;
    }

    @Embeddable
    @Getter
    @Setter
    public static class HistoricoEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 30)
        private StatusExecucao status;
        @Column(columnDefinition = "TEXT")
        private String descricao;
        @Column(length = 100)
        private String responsavel;
        @Column(name = "ocorrido_em", nullable = false)
        private LocalDateTime ocorridoEm;
    }
}
