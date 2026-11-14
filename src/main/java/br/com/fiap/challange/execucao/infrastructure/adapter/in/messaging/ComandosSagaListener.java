package br.com.fiap.challange.execucao.infrastructure.adapter.in.messaging;

import br.com.fiap.challange.execucao.domain.model.ItemExecucao;
import br.com.fiap.challange.execucao.domain.port.in.ExecucaoInputPort;
import br.com.fiap.challange.execucao.domain.port.in.command.AgendarExecucaoCommand;
import br.com.fiap.challange.execucao.domain.port.out.MensagensProcessadasPort;
import br.com.fiap.challange.execucao.infrastructure.messaging.Mensagem;
import br.com.fiap.challange.execucao.infrastructure.messaging.ReceptorMensagens;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static br.com.fiap.challange.execucao.domain.model.TiposMensagem.EXECUTAR_OS;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComandosSagaListener implements ReceptorMensagens {

    private final ExecucaoInputPort execucoes;
    private final MensagensProcessadasPort processadas;

    @Override
    @Transactional
    public void receber(Mensagem mensagem) {
        if (processadas.jaProcessada(mensagem.id())) {
            log.info("Mensagem {} ({}) repetida, ignorando", mensagem.id(), mensagem.tipo());
            return;
        }
        if (EXECUTAR_OS.equals(mensagem.tipo())) {
            execucoes.agendar(paraComando(mensagem));
        } else {
            log.warn("Tipo de mensagem desconhecido para o Execution Service: {}", mensagem.tipo());
        }
        processadas.registrar(mensagem.id(), mensagem.tipo());
    }

    private AgendarExecucaoCommand paraComando(Mensagem mensagem) {
        JsonNode dados = mensagem.dados();
        List<ItemExecucao> itens = new ArrayList<>();
        for (JsonNode item : dados.path("itens")) {
            itens.add(new ItemExecucao(item.path("tipo").asText(), item.path("descricao").asText(),
                    item.path("quantidade").asInt(1)));
        }
        return new AgendarExecucaoCommand(mensagem.sagaId(), mensagem.ordemServicoId(),
                dados.path("numeroOS").asText(), dados.path("descricaoProblema").asText(null),
                itens, dados.path("prioridade").asInt(0));
    }
}
