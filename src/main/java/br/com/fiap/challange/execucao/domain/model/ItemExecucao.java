package br.com.fiap.challange.execucao.domain.model;

/** O que precisa ser feito no veículo (serviços e peças aprovados no orçamento). */
public record ItemExecucao(String tipo, String descricao, int quantidade) {
}
