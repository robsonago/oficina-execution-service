package br.com.fiap.challange.execucao.domain.model;

import java.util.EnumSet;
import java.util.Set;

public enum StatusExecucao {
    NA_FILA,
    EM_DIAGNOSTICO,
    EM_REPARO,
    FINALIZADA,
    FALHOU;

    public boolean podeIrPara(StatusExecucao destino) {
        Set<StatusExecucao> permitidos = switch (this) {
            case NA_FILA -> EnumSet.of(EM_DIAGNOSTICO, EM_REPARO, FALHOU);
            case EM_DIAGNOSTICO -> EnumSet.of(EM_REPARO, FALHOU);
            case EM_REPARO -> EnumSet.of(FINALIZADA, FALHOU);
            case FINALIZADA, FALHOU -> EnumSet.noneOf(StatusExecucao.class);
        };
        return permitidos.contains(destino);
    }

    public boolean emAndamento() {
        return this == NA_FILA || this == EM_DIAGNOSTICO || this == EM_REPARO;
    }
}
