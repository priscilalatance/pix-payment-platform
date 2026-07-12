# pix-payment-platform

Ecossistema distribuído de microsserviços para processamento de pagamentos de faturas via PIX, com DDD, comunicação assíncrona, cache, resiliência e testes de contrato.

## Arquitetura

Três microsserviços independentes, cada um com seu próprio banco (database-per-service), que se comunicam de forma assíncrona por RabbitMQ (SAGA) e Kafka (eventos):

| Serviço | Porta | Responsabilidade | Infra que usa |
|---------|:-----:|------------------|---------------|
| **ms-pagamento** | 8080 | Orquestrador da SAGA: recebe o pedido PIX e garante que a fatura só é paga se o comprovante for gerado. | PostgreSQL (5432) + RabbitMQ |
| **ms-comprovantes** | 8081 | Gera o comprovante (gravação assíncrona), consulta com cache, e avisa os demais serviços. | PostgreSQL (5433) + Redis + Kafka + RabbitMQ |
| **ms-notificacao** | 8082 | Consome o evento de pagamento realizado e "notifica" o cliente, com resiliência (`@RetryableTopic` + DLT). | PostgreSQL (5434) + Kafka |

### Fluxo ponta a ponta

```
1.  Cliente chama POST /pagamentos                                  (ms-pagamento)
2.  ms-pagamento salva a fatura como PENDENTE
3.  ms-pagamento chama POST /comprovantes                           (ms-comprovantes)
4.  ms-comprovantes valida, gera o UUID e retorna 202
5.  ms-comprovantes publica na fila RabbitMQ (gravação assíncrona)
6.  ms-pagamento atualiza a fatura para COMPROVANTE_SOLICITADO
7.  ms-comprovantes consome a fila e grava o comprovante no banco
8.  ms-comprovantes publica em saga.comprovante.sucesso            (avisa ms-pagamento)
9.  ms-comprovantes publica no tópico Kafka pagamento.realizado    (avisa ms-notificacao)
10. ms-pagamento recebe o callback de sucesso → fatura vira PAGA ✅
11. ms-notificacao consome o tópico Kafka → notifica o cliente
12. Falha em qualquer ponto → compensação → fatura vira FALHA ❌
```

Os contratos completos (payloads JSON, filas, routing keys e tópicos) estão em [`DIVISAO_PROJETO.md`](DIVISAO_PROJETO.md).

## Infraestrutura compartilhada

Toda a infra sobe por **um único `docker-compose.yml` na raiz** do projeto (não há compose por serviço). Ele provê:

| Componente | Porta(s) | Uso |
|------------|----------|-----|
| RabbitMQ | 5672 (AMQP), 15672 (painel web) | SAGA entre ms-pagamento e ms-comprovantes |
| Redis | 6379 | Cache de consulta do ms-comprovantes |
| Kafka (**KRaft**) | 9092 | Evento `pagamento.realizado` (ms-comprovantes → ms-notificacao) |
| PostgreSQL — ms-pagamento | 5432 | `db_pagamento` |
| PostgreSQL — ms-comprovantes | 5433 | `db_comprovantes` |
| PostgreSQL — ms-notificacao | 5434 | `db_notificacao` |

## Pré-requisitos

- **Java 17**, Maven e Docker/Docker Compose.

## Como rodar

**1. Suba a infraestrutura compartilhada** (na raiz do projeto):

```bash
docker-compose up -d
```

Para subir só o necessário, informe os serviços: `docker-compose up -d rabbitmq postgres-pagamento`.

**2. Rode cada microsserviço** (em terminais separados, dentro da pasta do serviço):

```bash
cd ms-pagamento     && mvn spring-boot:run   # http://localhost:8080
cd ms-comprovantes  && mvn spring-boot:run   # http://localhost:8081
cd ms-notificacao   && mvn spring-boot:run   # http://localhost:8082
```

**3. Pare a infraestrutura** quando terminar:

```bash
docker-compose down
```

## Testes

Os testes rodam contra H2 em memória, sem precisar de Docker. A partir da pasta de cada serviço:

```bash
mvn test
```

## Documentação por serviço

- [`ms-pagamento/README.md`](ms-pagamento/README.md) — SAGA, endpoints e guia de teste manual dos cenários.
- [`ms-comprovantes/README.md`](ms-comprovantes/README.md) — geração/consulta de comprovante, cache Redis, PACT.
- [`ms-notificacao/README.md`](ms-notificacao/README.md) — consumer Kafka, `@RetryableTopic` e DLT.
