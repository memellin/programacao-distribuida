-- =============================================================
-- PulseAlert - Script de Inicialização do Banco de Dados
-- Banco: PostgreSQL 15
-- Aplicado automaticamente nos bancos Primário e Réplica
-- =============================================================
-- NOTA: O Hibernate (ddl-auto: update) cria as tabelas
-- automaticamente na primeira execução. Este script serve como
-- documentação do schema e pode ser usado para criação manual.
-- =============================================================

-- 1. Tabela de Sinais Vitais (dados brutos dos sensores)
CREATE TABLE IF NOT EXISTS vital_signs (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id            VARCHAR(255)    NOT NULL UNIQUE,
    correlation_id      VARCHAR(255)    NOT NULL,
    bed_id              VARCHAR(255)    NOT NULL,
    heart_rate          DOUBLE PRECISION NOT NULL,
    spo2                DOUBLE PRECISION NOT NULL,
    systolic_pressure   DOUBLE PRECISION NOT NULL,
    diastolic_pressure  DOUBLE PRECISION NOT NULL,
    temperature         DOUBLE PRECISION NOT NULL,
    lamport_clock       BIGINT          NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);

-- 2. Tabela de Análises (resultado do processamento dos workers)
CREATE TABLE IF NOT EXISTS analyses (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    vital_sign_id       UUID            NOT NULL UNIQUE REFERENCES vital_signs(id),
    worker_id           INTEGER         NOT NULL,
    arrhythmia_detected BOOLEAN         NOT NULL,
    shock_detected      BOOLEAN         NOT NULL,
    lamport_clock       BIGINT          NOT NULL,
    processed_at        TIMESTAMP       NOT NULL
);

-- 3. Tabela de Alertas Críticos (gerados quando arritmia ou choque são detectados)
CREATE TABLE IF NOT EXISTS alerts (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    analysis_id         UUID            NOT NULL UNIQUE REFERENCES analyses(id),
    type                VARCHAR(255)    NOT NULL,
    severity            VARCHAR(255)    NOT NULL,
    status              VARCHAR(255)    NOT NULL,
    created_at          TIMESTAMP       NOT NULL
);

-- Índices para consultas frequentes do Líder (verificação de duplicatas)
CREATE INDEX IF NOT EXISTS idx_vital_signs_event_id ON vital_signs(event_id);
CREATE INDEX IF NOT EXISTS idx_analyses_worker_id ON analyses(worker_id);
CREATE INDEX IF NOT EXISTS idx_alerts_status ON alerts(status);
