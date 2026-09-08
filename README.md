# PulseAlert - Sistema Distribuído de Monitoramento de UTI

## 👥 Integrantes
* **Enock De Oliveira Memelli Junior** - Matrícula: 6-2211755
* **Felipe Sabino** - Matrícula: 
* **Samuel Mota** - Matrícula:
* **Pedro Muchelin** - Matrícula:

## 🏥 Tema e Domínio de Negócio
O **PulseAlert** é um sistema distribuído voltado para a área da Saúde. Seu objetivo é realizar a ingestão, o monitoramento e o processamento em tempo real de sinais vitais de pacientes internados em Unidades de Terapia Intensiva (UTIs).

O sistema simula sensores de leito que enviam dados contínuos de telemetria (frequência cardíaca, pressão arterial, saturação) via comunicação síncrona de baixa latência (**gRPC**). Esses dados são repassados a um buffer distribuído (**RabbitMQ**) para garantir desacoplamento. Um cluster de nós trabalhadores (**Workers**) consome os dados de forma concorrente, aplicando regras de diagnóstico (arritmia e choque hipovolêmico) e utilizando **Relógios de Lamport** para ordenação causal. A persistência é feita utilizando o padrão Double-Write em um banco de dados Primário e uma Réplica (**PostgreSQL**), orquestrada por um Líder eleito via **Algoritmo em Anel (Ring)**.

## 🏗️ Arquitetura e Fluxo de Mensagens

O diagrama abaixo detalha os componentes, as portas expostas e o fluxo de dados arquitetural do sistema.

```mermaid
graph TD
    %% Entidades Externas
    Sensor[Sensores UTI / Postman]

    %% Gateway
    subgraph "Camada Ingress (Gateway)"
        Gateway[PulseAlert Gateway]
    end

    %% Mensageria
    subgraph "Middleware (Buffer Distribuído)"
        MQ[(RabbitMQ)]
        F1[Fila: vital_signals]
        F2[Fila: processed_events]
        MQ --- F1
        MQ --- F2
    end

    %% Workers
    subgraph "Processamento Concorrente (Workers)"
        W1[Worker 1 - Porta 8080]
        W2[Worker 2 - Porta 8081]
        W3[Worker 3 - Líder - Porta 8082]
    end

    %% Persistência
    subgraph "Camada de Dados (PostgreSQL)"
        DB1[(Primário - Porta 5432)]
        DB2[(Réplica - Porta 5433)]
    end

    %% Fluxo de Entrada
    Sensor -- "gRPC (Porta 9090)" --> Gateway
    Gateway -- "Publica JSON Assíncrono" --> F1

    %% Fluxo de Processamento (Competing Consumers)
    F1 -. "Consome (Concorrente)" .-> W1
    F1 -. "Consome (Concorrente)" .-> W2
    F1 -. "Consome (Concorrente)" .-> W3

    %% Atualização Lamport e Repasse
    W1 -- "Publica Diagnóstico (Lamport L+1)" --> F2
    W2 -- "Publica Diagnóstico (Lamport L+1)" --> F2

    %% Fluxo de Persistência (Líder)
    F2 ==>|Consumo Exclusivo| W3
    W3 == "Double-Write (TransactionTemplate)" ==> DB1
    W3 == "Double-Write (TransactionTemplate)" ==> DB2

    %% Algoritmo em Anel
    W1 -. "REST /api/ring/election" .-> W2
    W2 -. "REST /api/ring/election" .-> W3
    W3 -. "REST /api/ring/election" .-> W1