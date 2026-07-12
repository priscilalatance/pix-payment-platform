# MS Comprovantes — Documentação

## Visão Geral

Microsserviço responsável por **gerar comprovantes** de pagamento PIX. Recebe a solicitação do MS Pagamento, grava o comprovante de forma **assíncrona** (via RabbitMQ), disponibiliza a **consulta com cache Redis** e fecha o ciclo avisando os demais serviços:

- **Callback da SAGA** (RabbitMQ → MS Pagamento): marca a fatura como PAGA/FALHA.
- **Evento Kafka** `pagamento.realizado` (→ MS Notificação): dispara a notificação do cliente.

É também o lado **provider** dos testes de contrato PACT.

---

## Arquitetura

**Padrão:** DDD (Domain-Driven Design), espelhando o `ms-pagamento`.

```
com.comprovante/
├── controller/           → Entrada HTTP (REST) + GlobalExceptionHandler
├── application/
│   ├── service/          → Casos de uso (solicitar, processar, persistir, consultar)
│   └── exception/        → ComprovanteNaoEncontradoException
├── domain/
│   ├── entity/           → Comprovante (aggregate root)
│   ├── valueobject/      → DadosTransacao, DadosDestinatario (@Embeddable)
│   └── repository/       → ComprovanteRepository
├── dto/                  → Objetos de transferência e mensagens
└── infrastructure/
    ├── config/           → RabbitMQ, Redis, Kafka
    └── messaging/        → Producer, Listener e Publishers
```

Separação de responsabilidades dos serviços:
- `ComprovanteService` — orquestra o **POST**: gera UUID e enfileira.
- `ComprovanteProcessingService` — orquestra o **consumer**: idempotência + grava + publica saga/kafka.
- `ComprovantePersistenceService` — escrita `@Transactional` no banco.
- `ComprovanteConsultaService` — **GET**: cache Redis + retry no banco.

---

## Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 17 | Linguagem (**ver nota de build abaixo**) |
| Spring Boot | 3.2.5 | Framework |
| Spring Data JPA | 3.2.5 | Persistência |
| Spring AMQP | 3.1.x | RabbitMQ (producer + consumer) |
| Spring Data Redis | 3.2.5 | Cache de consulta |
| Spring Kafka | 3.1.x | Producer do tópico `pagamento.realizado` |
| Spring Validation | 3.2.5 | Validação de entrada |
| PostgreSQL | 15 | Banco de dados (Docker) |
| Redis | 7 | Cache (Docker) |
| Kafka | 7.6 (KRaft) | Broker de eventos (Docker) |
| H2 | — | Banco de testes (in-memory) |
| Lombok | (parent) | Redução de boilerplate |
| PACT | 4.6.7 | Testes de contrato (provider) |

> **Nota de build (importante):** o Lombok fixado pelo parent 3.2.5 **não funciona com o JDK 25**. Compile/rode com **JDK 17** (target do projeto). Se o `JAVA_HOME` padrão não for o 17, aponte-o para uma instalação do JDK 17 antes do build:
> ```bash
> # Linux/macOS
> export JAVA_HOME=/caminho/para/jdk-17
> # Windows (PowerShell)
> $env:JAVA_HOME = "C:\caminho\para\jdk-17"
> ```
> Depois: `mvn test`.

---

## Endpoints

### POST /comprovantes

Recebe a solicitação de comprovante, valida, gera o UUID e enfileira para gravação assíncrona.

**Request:**
```
POST http://localhost:8081/comprovantes
Content-Type: application/json
```
```json
{
  "nome": "Giovanni Vicente",
  "tipo_documento": "CPF",
  "numero_documento": "50329291076",
  "numero_agencia": "2022",
  "numero_conta": "00276",
  "digito_verificador_conta": "0",
  "valor_transacao": 23.99,
  "tipo_chave_pix_destino": "CELULAR",
  "chave_pix_destino": "11948755536",
  "nome_cliente_destino": "Fernando Augusto",
  "identificacao_pix": "Segue pagamento da minha cota no churrasco de domingo",
  "data_hora_transacao": "2022-04-10T20:03:57.116061100"
}
```

**Response 202 (Accepted):**
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "data_hora_requisicao": "2022-04-10T20:03:57.116061100"
}
```

**Response 400 (validação):**
```json
{
  "timestamp": "2022-04-10T20:03:57",
  "status": 400,
  "erros": ["nome: must not be blank", "valor_transacao: must not be null"]
}
```

### GET /comprovantes/{id}

Consulta um comprovante pelo identificador (UUID). Fluxo: **cache Redis → banco (até 3 tentativas) → 404**.

**Response 200:**
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "nome": "Giovanni Vicente",
  "tipo_documento": "CPF",
  "numero_documento": "50329291076",
  "numero_agencia": "2022",
  "numero_conta": "00276",
  "digito_verificador_conta": "0",
  "valor_transacao": 23.99,
  "tipo_chave_pix_destino": "CELULAR",
  "chave_pix_destino": "11948755536",
  "nome_cliente_destino": "Fernando Augusto",
  "identificacao_pix": "Segue pagamento",
  "data_hora_transacao": "2022-04-10T20:03:57.116061100"
}
```

**Response 404 (após 3 tentativas sem encontrar):**
```json
{
  "timestamp": "2022-04-10T20:03:57",
  "status": 404,
  "erro": "Comprovante nao encontrado: <id>"
}
```

---

## Fluxos

### POST (síncrono)
```
1. @Valid ComprovanteRequest → falha → 400
2. Gera UUID v4 (identificador_comprovante) + data_hora_requisicao
3. Publica ComprovanteMessage em comprovante.gerar
4. Retorna 202 (sem tocar no banco)
```

### Consumer (assíncrono) — fila `comprovante.gerar`
```
1. Recebe ComprovanteMessage
2. Idempotência: se o comprovante já existe → ignora (protege contra redelivery)
3. grava Comprovante no banco (@Transactional)
     - falha na gravação → publica saga.comprovante.falha → MS Pagamento marca fatura FALHA; encerra
4. gravou com sucesso:
     - publica saga.comprovante.sucesso → MS Pagamento marca fatura PAGA
     - publica evento Kafka pagamento.realizado → MS Notificação (best-effort:
       falha do Kafka só é logada, NÃO dispara compensação depois do SUCESSO)
5. ack (sem requeue)
```

### GET (cache + retry)
```
1. Redis (chave "comprovante:{id}") → hit → retorna
2. Miss → busca no banco em até 3 tentativas (delay entre elas)
3. Achou → grava no Redis (TTL) → retorna
4. Não achou após 3 tentativas → 404
```

---

## Mensageria

### RabbitMQ

| Item | Valor |
|------|-------|
| Exchange (gravação) | `comprovante.exchange` (direct) |
| Fila | `comprovante.gerar` (durable) |
| Routing key | `comprovante.routing-key` |
| Exchange (SAGA) | `saga.exchange` (direct) |
| Routing key sucesso | `saga.sucesso.routing-key` |
| Routing key falha | `saga.falha.routing-key` |

As constantes da SAGA batem exatamente com o `RabbitMQConfig` do `ms-pagamento`. O payload `SagaCallbackMessage` (`identificador_comprovante`, `status`, `motivo`, `data_hora_processamento`) é idêntico ao consumido lá.

### Kafka

| Item | Valor |
|------|-------|
| Tópico | `pagamento.realizado` |

**Payload do evento:**
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "nome": "Giovanni Vicente",
  "numero_documento": "50329291076",
  "valor_transacao": 23.99,
  "chave_pix_destino": "11948755536",
  "nome_cliente_destino": "Fernando Augusto",
  "data_hora_transacao": "2022-04-10T20:03:57.116061100"
}
```

---

## Modelo de Domínio

**Comprovante** (aggregate root, tabela `comprovante`)

| Campo | Tipo | Observação |
|-------|------|-----------|
| id | UUID | `identificador_comprovante` — atribuído no POST (não `@GeneratedValue`) |
| nome | String | pagador |
| tipoDocumento / numeroDocumento | String | documento do pagador |
| numeroAgencia / numeroConta / digitoVerificadorConta | String | dados bancários do pagador |
| dadosTransacao | DadosTransacao (@Embedded) | valorTransacao, identificacaoPix, dataHoraTransacao |
| dadosDestinatario | DadosDestinatario (@Embedded) | tipoChavePixDestino, chavePixDestino, nomeClienteDestino |
| criadoEm | LocalDateTime | `@PrePersist` |

> O UUID é gerado no `POST /comprovantes` e devolvido ao cliente **antes** da gravação; o consumer grava com esse mesmo id — por isso ele é atribuído explicitamente, sem `@GeneratedValue`.

---

## Cache (Redis)

- Chave: `comprovante:{id}`
- Valor: `ComprovanteConsultaResponse` (JSON, via `Jackson2JsonRedisSerializer` com suporte a datas)
- TTL: `app.cache.comprovante.ttl-minutes` (padrão 10 min)
- Populado no primeiro GET que encontra o comprovante no banco.

---

## Testes de Contrato (PACT) — Provider

O `ms-pagamento` (consumer) gera o contrato; este serviço (provider) o verifica.

1. Gere o contrato no `ms-pagamento`:
   ```bash
   cd ms-pagamento && mvn test    # gera target/pacts/ms-pagamento-ms-comprovantes.json
   ```
2. O arquivo está **versionado** em `ms-comprovantes/src/test/resources/pacts/` (atualize-o quando o contrato mudar).
3. `ComprovanteProviderPactTest` sobe a aplicação e verifica que `POST /comprovantes` responde conforme o contrato (202 + `identificador_comprovante`). O `ComprovanteProducer` é mockado para não exigir RabbitMQ.

---

## Testes

| Classe | Testes | Cobertura |
|--------|:------:|-----------|
| ComprovanteControllerTest | 5 | POST 202, POST 400 (2x), GET 200, GET 404 |
| ComprovanteProcessingServiceTest | 4 | grava+sucesso+kafka, falha→compensação, idempotência, falha do Kafka não compensa |
| ComprovanteConsultaServiceTest | 4 | cache hit, miss→banco→cacheia, 404 após 3 tentativas, UUID inválido→404 |
| ComprovanteProviderPactTest | 1 | Contrato PACT provider |
| **Total** | **14** | |

Rodar (com JDK 17):
```bash
mvn test
```

---

## Banco de Dados

| Config | Valor |
|--------|-------|
| Tipo | PostgreSQL 15 (Docker) |
| Host | localhost:5433 |
| Database | db_comprovantes |
| Usuário / Senha | comprovantes / comprovantes123 |
| DDL | auto (update) |

Tabela `comprovante`: `id` (UUID PK), dados do pagador, dados bancários, dados da transação e do destinatário (colunas mapeadas dos VOs), `criado_em`.

---

## Como Executar

### Pré-requisitos
- **JDK 17** (ver nota de build)
- Maven 3.8+
- Docker e Docker Compose

### 1. Subir infraestrutura

> A infraestrutura é um **único `docker-compose.yml` na raiz do projeto** — todos os comandos `docker-compose` deste guia rodam a partir da raiz, não da pasta do serviço. Para subir só o que o ms-comprovantes usa: `docker-compose up -d postgres-comprovantes redis kafka rabbitmq`.

```bash
docker-compose up -d
```

### 2. Rodar a aplicação
```bash
mvn spring-boot:run
```

### 3. Rodar testes
```bash
mvn test    # 14 testes
```

### 4. Parar infraestrutura
```bash
docker-compose down
```

---

## Guia de Teste Manual

### Cenário 1 — Gerar e consultar comprovante

**Passo 1:** Suba a infra e a aplicação.

**Passo 2:** Solicite o comprovante:
```bash
curl -s -X POST http://localhost:8081/comprovantes \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Giovanni Vicente",
    "tipo_documento": "CPF",
    "numero_documento": "50329291076",
    "numero_agencia": "2022",
    "numero_conta": "00276",
    "digito_verificador_conta": "0",
    "valor_transacao": 23.99,
    "tipo_chave_pix_destino": "CELULAR",
    "chave_pix_destino": "11948755536",
    "nome_cliente_destino": "Fernando Augusto",
    "identificacao_pix": "Segue pagamento",
    "data_hora_transacao": "2022-04-10T20:03:57.116061100"
  }'
```
Resposta: **202** com `identificador_comprovante`. Anote o UUID.

**Passo 3:** Verifique a gravação assíncrona no banco:
```bash
docker exec -it postgres-comprovantes psql -U comprovantes -d db_comprovantes -c "SELECT id, nome FROM comprovante;"
```

**Passo 4:** Consulte (a 1ª vez busca no banco e popula o cache; a 2ª vem do Redis):
```bash
curl -s http://localhost:8081/comprovantes/COLE_O_UUID_AQUI
```

### Cenário 2 — Callback da SAGA e evento Kafka

Com o `ms-pagamento` no ar, o `saga.comprovante.sucesso` publicado no Passo 2 leva a fatura a **PAGA**. Para inspecionar o evento Kafka:
```bash
docker exec -it kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic pagamento.realizado --from-beginning
```

### Cenário 3 — 404
```bash
curl -s -i http://localhost:8081/comprovantes/00000000-0000-0000-0000-000000000000
```
Após 3 tentativas → **404**.

### Cenário 4 — Validação
```bash
curl -s -i -X POST http://localhost:8081/comprovantes -H "Content-Type: application/json" -d '{}'
```
Resposta: **400** com a lista de `erros`.

---

## Integração com Outros Microsserviços

| MS | Comunicação | Direção |
|----|-------------|---------|
| MS Pagamento | HTTP POST /comprovantes | MS Pagamento → Este MS |
| MS Pagamento | RabbitMQ (saga.comprovante.sucesso/falha) | Este MS → MS Pagamento |
| MS Notificação | Kafka (pagamento.realizado) | Este MS → MS Notificação |
