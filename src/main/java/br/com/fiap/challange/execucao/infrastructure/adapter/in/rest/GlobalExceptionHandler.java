package br.com.fiap.challange.execucao.infrastructure.adapter.in.rest;

import br.com.fiap.challange.execucao.domain.exception.RecursoNaoEncontradoException;
import br.com.fiap.challange.execucao.domain.exception.TransicaoInvalidaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErroResponse(String mensagem, int status, LocalDateTime timestamp, Map<String, String> campos) {
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return erro(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(TransicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> transicao(TransicaoInvalidaException ex) {
        return erro(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler({AccessDeniedException.class, AuthorizationDeniedException.class})
    public ResponseEntity<ErroResponse> negado(RuntimeException ex) {
        return erro(HttpStatus.FORBIDDEN, "Acesso restrito aos funcionários da oficina");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(e ->
                campos.put(e instanceof FieldError fe ? fe.getField() : e.getObjectName(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponse("Dados inválidos", 400, LocalDateTime.now(), campos));
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(mensagem, status.value(), LocalDateTime.now(), null));
    }
}
