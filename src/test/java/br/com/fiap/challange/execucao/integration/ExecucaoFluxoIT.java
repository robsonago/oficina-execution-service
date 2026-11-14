package br.com.fiap.challange.execucao.integration;

import br.com.fiap.challange.execucao.infrastructure.adapter.out.mensageria.RelayOutbox;
import br.com.fiap.challange.execucao.infrastructure.adapter.out.persistence.OutboxJpaRepository;
import br.com.fiap.challange.execucao.infrastructure.messaging.CanalEmMemoria;
import br.com.fiap.challange.execucao.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.execucao.infrastructure.messaging.ReceptorMensagens;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
@ActiveProfiles("test")
class ExecucaoFluxoIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    ReceptorMensagens receptor;
    @Autowired
    CanalEmMemoria canal;
    @Autowired
    RelayOutbox relay;
    @Autowired
    OutboxJpaRepository outbox;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper mapper;

    @BeforeEach
    void limpar() {
        canal.limpar();
        jdbc.execute("DELETE FROM execucao_itens");
        jdbc.execute("DELETE FROM execucao_historico");
        jdbc.execute("DELETE FROM execucoes");
        jdbc.execute("DELETE FROM outbox_mensagens");
        jdbc.execute("DELETE FROM mensagens_processadas");
    }

    private Mensagem executarOS(String sagaId, long osId, int prioridade) {
        return Mensagem.nova("EXECUTAR_OS", sagaId, osId, mapper.valueToTree(Map.of(
                "numeroOS", "OS-" + osId,
                "descricaoProblema", "Barulho na suspensão",
                "prioridade", prioridade,
                "itens", List.of(Map.of("tipo", "SERVICO", "descricao", "Troca de amortecedor", "quantidade", 2)))));
    }

    private Long idDaExecucao(long osId) throws Exception {
        String json = mvc.perform(get("/api/execucoes/ordem-servico/{os}", osId).header("Authorization", Tokens.mecanico()))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get(0).path("id").asLong();
    }

    @Test
    void osPagaEntraNaFilaEOMecanicoConclui() throws Exception {
        Mensagem comando = executarOS("saga-1", 1L, 0);
        receptor.receber(comando);
        receptor.receber(comando); // reentrega

        mvc.perform(get("/api/execucoes/fila").header("Authorization", Tokens.mecanico()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].itens[0].quantidade").value(2));

        mvc.perform(post("/api/execucoes/proxima").header("Authorization", Tokens.mecanico()))
                .andExpect(jsonPath("$.status").value("EM_DIAGNOSTICO"))
                .andExpect(jsonPath("$.responsavel").value("joao.mecanico"));
        Long id = idDaExecucao(1L);
        mvc.perform(post("/api/execucoes/{id}/reparo", id).header("Authorization", Tokens.mecanico()))
                .andExpect(jsonPath("$.status").value("EM_REPARO"));
        mvc.perform(post("/api/execucoes/{id}/finalizar", id).header("Authorization", Tokens.mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observacoes\":\"Amortecedores trocados\"}"))
                .andExpect(jsonPath("$.status").value("FINALIZADA"))
                .andExpect(jsonPath("$.historico.length()").value(4));

        // os eventos ficam na outbox até o relay rodar
        assertThat(canal.enviadas()).isEmpty();
        assertThat(outbox.countByPublicadaEmIsNull()).isEqualTo(3);
        relay.publicarPendentes();
        assertThat(outbox.countByPublicadaEmIsNull()).isZero();
        assertThat(canal.enviadas()).extracting(e -> e.mensagem().tipo())
                .containsExactly("EXECUCAO_ATUALIZADA", "EXECUCAO_ATUALIZADA", "EXECUCAO_FINALIZADA");
        assertThat(canal.enviadas().getFirst().destino()).isEqualTo("os-eventos");
    }

    @Test
    void falhaNaExecucaoAvisaParaCompensar() throws Exception {
        receptor.receber(executarOS("saga-2", 2L, 0));
        Long id = idDaExecucao(2L);

        mvc.perform(post("/api/execucoes/{id}/diagnostico", id).header("Authorization", Tokens.mecanico()))
                .andExpect(status().isOk());
        mvc.perform(post("/api/execucoes/{id}/falha", id).header("Authorization", Tokens.mecanico())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Bloco do motor trincado\"}"))
                .andExpect(jsonPath("$.status").value("FALHOU"))
                .andExpect(jsonPath("$.motivoFalha").value("Bloco do motor trincado"));

        relay.publicarPendentes();
        Mensagem falha = canal.doTipo("EXECUCAO_FALHOU").getFirst();
        assertThat(falha.sagaId()).isEqualTo("saga-2");
        assertThat(falha.dados().path("motivo").asText()).isEqualTo("Bloco do motor trincado");
    }

    @Test
    void prioridadeOrdenaAFila() throws Exception {
        receptor.receber(executarOS("saga-a", 3L, 0));
        receptor.receber(executarOS("saga-b", 4L, 5));

        mvc.perform(get("/api/execucoes/fila").header("Authorization", Tokens.admin()))
                .andExpect(jsonPath("$[0].ordemServicoId").value(4));
    }

    @Test
    void validacoesESeguranca() throws Exception {
        receptor.receber(executarOS("saga-3", 5L, 0));
        Long id = idDaExecucao(5L);

        mvc.perform(get("/api/execucoes/fila")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/execucoes/fila").header("Authorization", Tokens.cliente())).andExpect(status().isForbidden());
        mvc.perform(get("/api/execucoes/999").header("Authorization", Tokens.mecanico())).andExpect(status().isNotFound());
        mvc.perform(post("/api/execucoes/{id}/finalizar", id).header("Authorization", Tokens.mecanico()))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/execucoes/{id}/falha", id).header("Authorization", Tokens.mecanico())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/execucoes/{id}", id).header("Authorization", Tokens.mecanico()))
                .andExpect(jsonPath("$.status").value("NA_FILA"));
    }

    @Test
    void filaVaziaEComandoDesconhecido() throws Exception {
        receptor.receber(Mensagem.nova("OUTRO", "s", 1L, mapper.createObjectNode()));
        mvc.perform(post("/api/execucoes/proxima").header("Authorization", Tokens.mecanico()))
                .andExpect(status().isNoContent());
    }

    @Test
    void outboxSeguraOEventoQuandoOBrokerFalha() throws Exception {
        receptor.receber(executarOS("saga-4", 6L, 0));
        mvc.perform(post("/api/execucoes/proxima").header("Authorization", Tokens.mecanico())).andExpect(status().isOk());
        jdbc.update("UPDATE outbox_mensagens SET conteudo = 'nao é json'");

        relay.publicarPendentes();

        assertThat(outbox.countByPublicadaEmIsNull()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT tentativas FROM outbox_mensagens", Integer.class)).isEqualTo(1);
    }

    @Test
    void observabilidade() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus")).andExpect(status().isOk());
        mvc.perform(get("/execucao/api-docs")).andExpect(status().isOk());
    }
}
