package br.com.fiap.challange.execucao.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.NotBlank;

public record FalhaRequest(@NotBlank(message = "Descreva o motivo da falha") String motivo) {
}
