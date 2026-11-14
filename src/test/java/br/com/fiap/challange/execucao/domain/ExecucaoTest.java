package br.com.fiap.challange.execucao.domain;

import br.com.fiap.challange.execucao.domain.exception.TransicaoInvalidaException;
import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.model.ItemExecucao;
import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecucaoTest {

    private Execucao nova() {
        return Execucao.naFila(1L, "OS-1", "saga-1", "Barulho no freio",
                List.of(new ItemExecucao("SERVICO", "Troca de pastilha", 1)), 0);
    }

    @Test
    void entraNaFila() {
        Execucao e = nova();
        assertThat(e.getStatus()).isEqualTo(StatusExecucao.NA_FILA);
        assertThat(e.getHistorico()).hasSize(1);
        assertThat(Execucao.naFila(1L, "OS", "s", null, null, 0).getItens()).isEmpty();
    }

    @Test
    void caminhoCompleto() {
        Execucao e = nova();
        e.iniciarDiagnostico("joao");
        var inicio = e.getInicioEm();
        e.iniciarReparo("joao");
        e.finalizar("joao", null);

        assertThat(e.getStatus()).isEqualTo(StatusExecucao.FINALIZADA);
        assertThat(e.getInicioEm()).isEqualTo(inicio);
        assertThat(e.getFimEm()).isNotNull();
        assertThat(e.getHistorico()).extracting(h -> h.status())
                .containsExactly(StatusExecucao.NA_FILA, StatusExecucao.EM_DIAGNOSTICO, StatusExecucao.EM_REPARO,
                        StatusExecucao.FINALIZADA);
    }

    @Test
    void reparoDiretoDaFilaMarcaInicio() {
        Execucao e = nova();
        e.iniciarReparo(null);
        assertThat(e.getInicioEm()).isNotNull();
        assertThat(e.getResponsavel()).isNull();
        e.finalizar("ana", "Tudo certo");
        assertThat(e.getObservacoes()).isEqualTo("Tudo certo");
    }

    @Test
    void falhaGuardaOMotivo() {
        Execucao e = nova();
        e.iniciarDiagnostico("joao");
        e.falhar("joao", "Peça errada");

        assertThat(e.getStatus()).isEqualTo(StatusExecucao.FALHOU);
        assertThat(e.getMotivoFalha()).isEqualTo("Peça errada");
    }

    @Test
    void transicoesInvalidas() {
        Execucao e = nova();
        assertThatThrownBy(() -> e.finalizar("x", null)).isInstanceOf(TransicaoInvalidaException.class);

        e.iniciarReparo("x");
        e.finalizar("x", null);
        assertThatThrownBy(() -> e.falhar("x", "tarde demais")).isInstanceOf(TransicaoInvalidaException.class);
        assertThatThrownBy(() -> e.iniciarDiagnostico("x")).isInstanceOf(TransicaoInvalidaException.class);
    }

    @Test
    void statusEmAndamento() {
        assertThat(StatusExecucao.NA_FILA.emAndamento()).isTrue();
        assertThat(StatusExecucao.EM_REPARO.emAndamento()).isTrue();
        assertThat(StatusExecucao.FINALIZADA.emAndamento()).isFalse();
        assertThat(StatusExecucao.FALHOU.podeIrPara(StatusExecucao.NA_FILA)).isFalse();
        assertThat(StatusExecucao.EM_DIAGNOSTICO.podeIrPara(StatusExecucao.FINALIZADA)).isFalse();
    }
}
