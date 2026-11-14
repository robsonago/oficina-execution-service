package br.com.fiap.challange.execucao.infrastructure.messaging;

public class FalhaMensageriaException extends RuntimeException {

    public FalhaMensageriaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
