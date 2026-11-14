package br.com.fiap.challange.execucao.domain.model;

import java.time.LocalDateTime;

public record HistoricoExecucao(StatusExecucao status, String descricao, String responsavel, LocalDateTime ocorridoEm) {
}
