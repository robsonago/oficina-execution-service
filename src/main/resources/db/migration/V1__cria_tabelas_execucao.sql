CREATE TABLE execucoes (
    id                  BIGSERIAL PRIMARY KEY,
    ordem_servico_id    BIGINT       NOT NULL,
    numero_os           VARCHAR(50)  NOT NULL,
    saga_id             VARCHAR(64)  NOT NULL UNIQUE,
    status              VARCHAR(30)  NOT NULL,
    descricao_problema  TEXT,
    prioridade          INTEGER      NOT NULL DEFAULT 0,
    responsavel         VARCHAR(100),
    observacoes         TEXT,
    motivo_falha        TEXT,
    entrada_fila_em     TIMESTAMP    NOT NULL,
    inicio_em           TIMESTAMP,
    fim_em              TIMESTAMP,
    versao              BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX idx_execucoes_status ON execucoes (status);
CREATE INDEX idx_execucoes_ordem_servico ON execucoes (ordem_servico_id);

CREATE TABLE execucao_itens (
    execucao_id BIGINT       NOT NULL REFERENCES execucoes (id) ON DELETE CASCADE,
    posicao     INTEGER      NOT NULL,
    tipo        VARCHAR(20)  NOT NULL,
    descricao   VARCHAR(255) NOT NULL,
    quantidade  INTEGER      NOT NULL,
    PRIMARY KEY (execucao_id, posicao)
);

CREATE TABLE execucao_historico (
    execucao_id BIGINT       NOT NULL REFERENCES execucoes (id) ON DELETE CASCADE,
    posicao     INTEGER      NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    descricao   TEXT,
    responsavel VARCHAR(100),
    ocorrido_em TIMESTAMP    NOT NULL,
    PRIMARY KEY (execucao_id, posicao)
);

-- Transactional outbox: o evento é gravado junto com a mudança de status e
-- um job separado publica no Pub/Sub.
CREATE TABLE outbox_mensagens (
    id            VARCHAR(36)  PRIMARY KEY,
    destino       VARCHAR(50)  NOT NULL,
    tipo          VARCHAR(50)  NOT NULL,
    saga_id       VARCHAR(64),
    conteudo      TEXT         NOT NULL,
    cabecalhos    TEXT,
    criada_em     TIMESTAMP    NOT NULL,
    publicada_em  TIMESTAMP,
    tentativas    INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX idx_outbox_pendentes ON outbox_mensagens (publicada_em, criada_em);

CREATE TABLE mensagens_processadas (
    id            VARCHAR(36) PRIMARY KEY,
    tipo          VARCHAR(50) NOT NULL,
    processada_em TIMESTAMP   NOT NULL
);
