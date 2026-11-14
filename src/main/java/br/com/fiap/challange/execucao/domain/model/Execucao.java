package br.com.fiap.challange.execucao.domain.model;

import br.com.fiap.challange.execucao.domain.exception.TransicaoInvalidaException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Execucao {

    private Long id;
    private Long ordemServicoId;
    private String numeroOS;
    private String sagaId;
    private StatusExecucao status;
    private String descricaoProblema;

    @Builder.Default
    private int prioridade = 0;

    private String responsavel;
    private String observacoes;
    private String motivoFalha;

    @Builder.Default
    private List<ItemExecucao> itens = new ArrayList<>();

    @Builder.Default
    private List<HistoricoExecucao> historico = new ArrayList<>();

    private LocalDateTime entradaFilaEm;
    private LocalDateTime inicioEm;
    private LocalDateTime fimEm;

    public static Execucao naFila(Long ordemServicoId, String numeroOS, String sagaId, String descricaoProblema,
                                  List<ItemExecucao> itens, int prioridade) {
        Execucao execucao = Execucao.builder()
                .ordemServicoId(ordemServicoId)
                .numeroOS(numeroOS)
                .sagaId(sagaId)
                .descricaoProblema(descricaoProblema)
                .itens(itens == null ? new ArrayList<>() : new ArrayList<>(itens))
                .prioridade(prioridade)
                .status(StatusExecucao.NA_FILA)
                .entradaFilaEm(LocalDateTime.now())
                .build();
        execucao.historico.add(new HistoricoExecucao(StatusExecucao.NA_FILA, "Entrou na fila de execução", null,
                execucao.entradaFilaEm));
        return execucao;
    }

    public void iniciarDiagnostico(String responsavel) {
        mudar(StatusExecucao.EM_DIAGNOSTICO, "Diagnóstico iniciado", responsavel);
        if (inicioEm == null) {
            inicioEm = LocalDateTime.now();
        }
    }

    public void iniciarReparo(String responsavel) {
        mudar(StatusExecucao.EM_REPARO, "Reparo iniciado", responsavel);
        if (inicioEm == null) {
            inicioEm = LocalDateTime.now();
        }
    }

    public void finalizar(String responsavel, String observacoes) {
        mudar(StatusExecucao.FINALIZADA, observacoes == null ? "Serviço concluído" : observacoes, responsavel);
        this.observacoes = observacoes;
        this.fimEm = LocalDateTime.now();
    }

    public void falhar(String responsavel, String motivo) {
        mudar(StatusExecucao.FALHOU, "Falha: " + motivo, responsavel);
        this.motivoFalha = motivo;
        this.fimEm = LocalDateTime.now();
    }

    private void mudar(StatusExecucao novo, String descricao, String quem) {
        if (!status.podeIrPara(novo)) {
            throw new TransicaoInvalidaException(String.format(
                    "Execução da OS %s não pode ir de %s para %s", numeroOS, status, novo));
        }
        this.status = novo;
        if (quem != null) {
            this.responsavel = quem;
        }
        historico.add(new HistoricoExecucao(novo, descricao, quem, LocalDateTime.now()));
    }
}
