package br.com.fiap.challange.execucao.infrastructure.adapter.in.rest;

import br.com.fiap.challange.execucao.domain.port.in.ExecucaoInputPort;
import br.com.fiap.challange.execucao.infrastructure.adapter.in.rest.dto.ExecucaoResponse;
import br.com.fiap.challange.execucao.infrastructure.adapter.in.rest.dto.FalhaRequest;
import br.com.fiap.challange.execucao.infrastructure.adapter.in.rest.dto.FinalizarRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/execucoes")
@RequiredArgsConstructor
@PreAuthorize("!hasRole('CLIENTE')")
@Tag(name = "Execuções", description = "Fila de trabalho da oficina: diagnóstico, reparo e conclusão das OS pagas")
public class ExecucaoController {

    private final ExecucaoInputPort execucoes;

    @GetMapping("/fila")
    @Operation(summary = "Fila de execução (maior prioridade primeiro, depois ordem de chegada)")
    public List<ExecucaoResponse> fila() {
        return execucoes.fila().stream().map(ExecucaoResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar execução por id")
    public ExecucaoResponse buscar(@PathVariable Long id) {
        return ExecucaoResponse.from(execucoes.buscar(id));
    }

    @GetMapping("/ordem-servico/{ordemServicoId}")
    @Operation(summary = "Execuções de uma OS")
    public List<ExecucaoResponse> porOrdemServico(@PathVariable Long ordemServicoId) {
        return execucoes.buscarPorOrdemServico(ordemServicoId).stream().map(ExecucaoResponse::from).toList();
    }

    @PostMapping("/proxima")
    @Operation(summary = "Mecânico pega a próxima OS da fila (já inicia o diagnóstico)")
    public ResponseEntity<ExecucaoResponse> proxima(Authentication auth) {
        return execucoes.puxarProxima(auth.getName())
                .map(e -> ResponseEntity.ok(ExecucaoResponse.from(e)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/diagnostico")
    @Operation(summary = "Iniciar diagnóstico (NA_FILA → EM_DIAGNOSTICO)")
    public ExecucaoResponse diagnostico(@PathVariable Long id, Authentication auth) {
        return ExecucaoResponse.from(execucoes.iniciarDiagnostico(id, auth.getName()));
    }

    @PostMapping("/{id}/reparo")
    @Operation(summary = "Iniciar reparo (→ EM_REPARO)")
    public ExecucaoResponse reparo(@PathVariable Long id, Authentication auth) {
        return ExecucaoResponse.from(execucoes.iniciarReparo(id, auth.getName()));
    }

    @PostMapping("/{id}/finalizar")
    @Operation(summary = "Concluir o serviço (EM_REPARO → FINALIZADA). Avisa o OS Service, que finaliza a OS")
    public ExecucaoResponse finalizar(@PathVariable Long id, Authentication auth,
                                      @RequestBody(required = false) FinalizarRequest request) {
        return ExecucaoResponse.from(execucoes.finalizar(id, auth.getName(),
                request == null ? null : request.observacoes()));
    }

    @PostMapping("/{id}/falha")
    @Operation(summary = "Registrar que o serviço não pôde ser executado. Dispara a compensação da saga (estorno do pagamento)")
    public ExecucaoResponse falha(@PathVariable Long id, Authentication auth, @Valid @RequestBody FalhaRequest request) {
        return ExecucaoResponse.from(execucoes.registrarFalha(id, auth.getName(), request.motivo()));
    }
}
