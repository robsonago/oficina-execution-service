package br.com.fiap.challange.execucao.domain.model;

/** Precisam bater com os nomes usados pelo OS Service (orquestrador). */
public final class TiposMensagem {

    // comando recebido
    public static final String EXECUTAR_OS = "EXECUTAR_OS";

    // eventos publicados
    public static final String EXECUCAO_ATUALIZADA = "EXECUCAO_ATUALIZADA";
    public static final String EXECUCAO_FINALIZADA = "EXECUCAO_FINALIZADA";
    public static final String EXECUCAO_FALHOU = "EXECUCAO_FALHOU";

    private TiposMensagem() {
    }
}
