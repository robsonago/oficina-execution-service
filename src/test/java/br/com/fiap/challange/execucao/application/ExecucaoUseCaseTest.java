package br.com.fiap.challange.execucao.application;

import br.com.fiap.challange.execucao.application.usecase.ExecucaoUseCase;
import br.com.fiap.challange.execucao.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.execucao.domain.model.Execucao;
import br.com.fiap.challange.execucao.domain.model.StatusExecucao;
import br.com.fiap.challange.execucao.domain.port.in.command.AgendarExecucaoCommand;
import br.com.fiap.challange.execucao.domain.port.out.EventosSagaPort;
import br.com.fiap.challange.execucao.domain.port.out.ExecucaoRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static br.com.fiap.challange.execucao.domain.model.TiposMensagem.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecucaoUseCaseTest {

    @Mock
    ExecucaoRepositoryPort repository;
    @Mock
    EventosSagaPort eventos;

    @InjectMocks
    ExecucaoUseCase useCase;

    private Execucao naFila(long id) {
        Execucao e = Execucao.naFila(10L, "OS-10", "saga-10", null, List.of(), 0);
        e.setId(id);
        return e;
    }

    private void salvarDevolveOMesmo() {
        when(repository.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void agendaUmaVezSo() {
        when(repository.buscarPorSagaId("saga-10")).thenReturn(Optional.empty(), Optional.of(naFila(1)));
        salvarDevolveOMesmo();
        var cmd = new AgendarExecucaoCommand("saga-10", 10L, "OS-10", "Motor", List.of(), 1);

        Execucao primeira = useCase.agendar(cmd);
        Execucao repetida = useCase.agendar(cmd);

        assertThat(primeira.getPrioridade()).isEqualTo(1);
        assertThat(repetida.getId()).isEqualTo(1L);
        verify(repository, times(1)).salvar(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void cadaMudancaAvisaOOrquestrador() {
        Execucao e = naFila(1);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(e));
        salvarDevolveOMesmo();

        useCase.iniciarDiagnostico(1L, "joao");
        useCase.iniciarReparo(1L, "joao");
        useCase.finalizar(1L, "joao", "ok");

        verify(eventos, times(2)).publicar(eq(EXECUCAO_ATUALIZADA), eq("saga-10"), eq(10L), anyMap());
        verify(eventos).publicar(eq(EXECUCAO_FINALIZADA), eq("saga-10"), eq(10L),
                argThat(d -> "ok".equals(d.get("observacoes")) && "FINALIZADA".equals(d.get("statusExecucao"))));
    }

    @Test
    void finalizarSemObservacao() {
        Execucao e = naFila(1);
        e.iniciarReparo(null);
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(e));
        salvarDevolveOMesmo();

        useCase.finalizar(1L, null, null);

        verify(eventos).publicar(eq(EXECUCAO_FINALIZADA), any(), any(), argThat(d -> !d.containsKey("observacoes")));
    }

    @Test
    void falhaPublicaOMotivo() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(naFila(1)));
        salvarDevolveOMesmo();

        Execucao e = useCase.registrarFalha(1L, "joao", "Motor fundido, sem conserto");

        assertThat(e.getStatus()).isEqualTo(StatusExecucao.FALHOU);
        verify(eventos).publicar(eq(EXECUCAO_FALHOU), eq("saga-10"), eq(10L),
                argThat(d -> "Motor fundido, sem conserto".equals(d.get("motivo"))));
    }

    @Test
    void puxarProximaPegaOPrimeiroDaFilaQueNinguemPegou() {
        Execucao emAndamento = naFila(1);
        emAndamento.iniciarDiagnostico("outro");
        Execucao livre = naFila(2);
        when(repository.fila()).thenReturn(List.of(emAndamento, livre));
        when(repository.buscarPorId(2L)).thenReturn(Optional.of(livre));
        salvarDevolveOMesmo();

        Optional<Execucao> pega = useCase.puxarProxima("joao");

        assertThat(pega).get().extracting(Execucao::getId).isEqualTo(2L);
        assertThat(pega.get().getResponsavel()).isEqualTo("joao");
    }

    @Test
    void filaVazia() {
        when(repository.fila()).thenReturn(List.of());
        assertThat(useCase.puxarProxima("joao")).isEmpty();
    }

    @Test
    void consultas() {
        when(repository.buscarPorId(1L)).thenReturn(Optional.of(naFila(1)));
        when(repository.buscarPorId(9L)).thenReturn(Optional.empty());
        when(repository.buscarPorOrdemServico(10L)).thenReturn(List.of(naFila(1)));
        when(repository.fila()).thenReturn(List.of(naFila(1)));

        assertThat(useCase.buscar(1L).getNumeroOS()).isEqualTo("OS-10");
        assertThat(useCase.buscarPorOrdemServico(10L)).hasSize(1);
        assertThat(useCase.fila()).hasSize(1);
        assertThatThrownBy(() -> useCase.buscar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
