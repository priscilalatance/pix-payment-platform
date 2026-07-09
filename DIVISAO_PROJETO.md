# Projeto Pagamento PIX — Arquitetura e Contratos

---

# PARTE 1 — CONTRATOS E CONFIGURAÇÕES

---

## Portas dos serviços

| Serviço | Porta |
|---------|:-----:|
| ms-pagamento | 8080 |
| ms-comprovantes | 8081 |
| ms-notificacao | 8082 |
| RabbitMQ (AMQP) | 5672 |
| RabbitMQ (painel web) | 15672 |
| Redis | 6379 |
| Kafka | 9092 |

---

## Contrato REST — POST /comprovantes

MS Pagamento (Priscila) chama esse endpoint. MS Comprovantes (Fabio) implementa.

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

**Response (sucesso):**
```
HTTP 202 Accepted
```
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "data_hora_requisicao": "2022-04-10T20:03:57.116061100"
}
```

**Response (erro de validação):**
```
HTTP 400 Bad Request
```

---

## Fila RabbitMQ — Gravar comprovante

MS Comprovantes (Fabio) usa internamente (publica no POST, consome para gravar no banco).

| Config | Valor |
|--------|-------|
| Fila | `comprovante.gerar` |
| Exchange | `comprovante.exchange` (tipo: direct) |
| Routing Key | `comprovante.routing-key` |

**Mensagem na fila** (payload do POST + UUID gerado):
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
  "identificacao_pix": "Segue pagamento da minha cota no churrasco de domingo",
  "data_hora_transacao": "2022-04-10T20:03:57.116061100"
}
```

---

## Callback da SAGA — MS Comprovantes avisa MS Pagamento

MS Comprovantes (Fabio) publica nessas filas. MS Pagamento (Priscila) consome.

| Config | Valor |
|--------|-------|
| Fila de sucesso | `saga.comprovante.sucesso` |
| Fila de falha | `saga.comprovante.falha` |
| Exchange | `saga.exchange` (tipo: direct) |
| Routing Key (sucesso) | `saga.sucesso.routing-key` |
| Routing Key (falha) | `saga.falha.routing-key` |

**Mensagem de sucesso** (publicada após gravar no banco):
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "status": "SUCESSO",
  "data_hora_processamento": "2022-04-10T20:03:58.000000000"
}
```

**Mensagem de falha** (publicada se der erro ao gravar):
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "status": "FALHA",
  "motivo": "Erro ao gravar no banco de dados",
  "data_hora_processamento": "2022-04-10T20:03:58.000000000"
}
```

---

## Tópico Kafka — Evento de pagamento realizado

MS Comprovantes (Fabio) publica. MS Notificação (Raimundo) consome.

| Config | Valor |
|--------|-------|
| Tópico | `pagamento.realizado` |
| DLT (Dead Letter Topic) | `pagamento.realizado-dlt` |

**Mensagem no tópico** (publicada após gravar comprovante com sucesso):
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

# PARTE 2 — RESPONSABILIDADES

---

## Priscila — MS Pagamento

**Resumo:** Recebe o pedido de pagamento PIX, orquestra o fluxo (SAGA) e garante que a fatura só é paga se o comprovante for gerado com sucesso. Também escreve os testes de contrato PACT.

---

### O que construir:

**1. Endpoint POST /pagamentos**
- Recebe a requisição de pagamento PIX do cliente
- Persiste a fatura no banco com status `PENDENTE`
- Faz uma chamada HTTP para `POST http://localhost:8081/comprovantes` (MS Comprovantes)
- Se receber 202 → atualiza status da fatura para `COMPROVANTE_SOLICITADO`
- Se der erro na chamada → atualiza para `FALHA`

**2. SAGA — Escutar o resultado**
- Listener na fila `saga.comprovante.sucesso`:
  - Ao receber → atualiza fatura para `PAGA` ✅
- Listener na fila `saga.comprovante.falha`:
  - Ao receber → atualiza fatura para `FALHA` ❌ (compensação)

**3. Banco de dados próprio (db-pagamento)**
- Tabela `faturas`: id, nome, tipo_documento, numero_documento, valor_transacao, status, criado_em, atualizado_em, timeout_em
- Tabela `pagamentos`: id, fatura_id, identificador_comprovante, dados bancários, chave PIX, dados do destinatário

**4. DDD**
- Entities: Fatura, Pagamento
- Value Objects: ChavePix, DadosBancarios
- Repositories: FaturaRepository, PagamentoRepository

**5. Testes de Contrato PACT (consumer side)**
- Teste que valida: "quando eu envio esse JSON para POST /comprovantes, espero receber 202 com identificador_comprovante e data_hora_requisicao"
- O PACT gera um arquivo JSON com esse contrato → versionado no repositório para o Fabio usar

---

### Tecnologias:
- Spring Boot
- Spring Data JPA + PostgreSQL (Docker)
- Spring AMQP (consumer das filas da SAGA)
- RestTemplate (chamada HTTP ao MS Comprovantes)
- PACT (pact-jvm-consumer-junit5)

---

## Fabio — MS Comprovantes

**Resumo:** Recebe pedidos de comprovante, grava de forma assíncrona via RabbitMQ, disponibiliza consulta com cache Redis, e avisa os outros serviços (SAGA + Kafka).

---

### O que construir:

**1. Endpoint POST /comprovantes**
- Recebe o JSON (12 campos) e valida que todos estão preenchidos
- Se algum campo faltando → retorna 400
- Se tudo OK:
  - Gera um UUID v4
  - Publica a mensagem na fila RabbitMQ `comprovante.gerar`
  - Retorna 202 com `identificador_comprovante` + `data_hora_requisicao`

**2. Consumer RabbitMQ (listener na fila `comprovante.gerar`)**
- Lê a mensagem da fila
- Grava o comprovante no banco de dados
- Se gravou com sucesso:
  - Publica mensagem na fila `saga.comprovante.sucesso` (avisa MS Pagamento)
  - Publica evento no tópico Kafka `pagamento.realizado` (avisa MS Notificação)
- Se falhou ao gravar:
  - Publica mensagem na fila `saga.comprovante.falha` (avisa MS Pagamento)

**3. Endpoint GET /comprovantes/{id}**
- Recebe o UUID do comprovante
- Fluxo:
  1. Busca no Redis (cache)
  2. Achou no Redis → retorna direto
  3. Não achou → busca no banco de dados
  4. Achou no banco → salva no Redis → retorna
  5. Não achou → tenta de novo (até 3 tentativas no total)
  6. Após 3 tentativas sem achar → retorna 404

**4. Testes de Contrato PACT (provider side)**
- Pega o arquivo Pact gerado pela Priscila
- Roda o teste de verificação: "minha API realmente retorna o que o contrato diz?"

**5. Banco de dados próprio (db-comprovantes)**
- Tabela `comprovante`: id (UUID), nome, tipo_documento, numero_documento, numero_agencia, numero_conta, digito_verificador_conta, valor_transacao, tipo_chave_pix_destino, chave_pix_destino, nome_cliente_destino, identificacao_pix, data_hora_transacao

**6. DDD**
- Aggregate: Comprovante
- Value Objects: DadosTransacao, DadosDestinatario
- Repository: ComprovanteRepository

---

### Tecnologias:
- Spring Boot
- Spring Data JPA + PostgreSQL (Docker)
- Spring AMQP (RabbitMQ — producer e consumer)
- Spring Data Redis (cache)
- Spring Kafka (producer)
- Bean Validation (@NotNull, @NotBlank, etc.)
- PACT (pact-jvm-provider-junit5)

---

## Raimundo — MS Notificação + Infra + Doc Cloud

**Resumo:** Consome eventos do Kafka, "notifica" o cliente, implementa resiliência com @RetryableTopic, monta a infraestrutura Docker e escreve o documento de Cloud.

---

### O que construir:

**1. Consumer Kafka (subscriber do tópico `pagamento.realizado`)**
- Listener que consome mensagens do tópico
- Ao receber:
  - Deserializa o JSON
  - Salva no banco uma notificação com status ENVIADA
  - Simula o envio (log no console: "Notificação enviada para [nome] — PIX de R$ [valor] realizado com sucesso")

**2. @RetryableTopic (resiliência)**
- Anotar o listener com `@RetryableTopic`
- Configurar:
  - 3 tentativas
  - Backoff exponencial (1s → 2s → 4s)
  - Se falhar todas → mensagem vai para `pagamento.realizado-dlt` (Dead Letter Topic)

**3. Banco de dados próprio (db-notificacao)**
- Tabela `notificacao`: id, identificador_comprovante, nome_destinatario, valor, status (ENVIADA, FALHA), tentativas, data_envio

**4. DDD**
- Aggregate: Notificacao
- Repository: NotificacaoRepository

**5. Docker Compose (infraestrutura para todos)**
- Criar o arquivo `docker-compose.yml` que sobe tudo:
  - RabbitMQ (com painel de gerenciamento)
  - Redis
  - Kafka + Zookeeper
  - 3 bancos PostgreSQL (um para cada MS)
- Documentar: `docker-compose up -d` e pronto

**6. Documento de Replicação em Cloud (README_CLOUD.md)**
- Pesquisa teórica explicando como colocar essa arquitetura na nuvem:
  - Onde rodar os microsserviços (ECS Fargate, EKS, etc.)
  - Substituto do RabbitMQ na cloud (Amazon MQ)
  - Substituto do Redis na cloud (Amazon ElastiCache)
  - Substituto do Kafka na cloud (Amazon MSK)
  - Banco de dados na cloud (Amazon RDS)
  - Diagrama da arquitetura
  - Estratégia de escalabilidade e alta disponibilidade

---

### Tecnologias:
- Spring Boot
- Spring Kafka (consumer + @RetryableTopic)
- Spring Data JPA + PostgreSQL (Docker)
- Docker / Docker Compose

---

# PARTE 3 — FLUXO COMPLETO

## Fluxo completo

```
1. Cliente chama POST /pagamentos (Priscila)
2. MS Pagamento salva fatura como PENDENTE
3. MS Pagamento chama POST /comprovantes (Fabio)
4. MS Comprovantes valida, gera UUID, retorna 202
5. MS Comprovantes publica na fila RabbitMQ
6. MS Pagamento atualiza fatura para COMPROVANTE_SOLICITADO
7. Consumer RabbitMQ (Fabio) lê a fila e grava no banco
8. MS Comprovantes publica na fila saga.comprovante.sucesso (avisa Priscila)
9. MS Comprovantes publica no tópico Kafka pagamento.realizado (avisa Raimundo)
10. MS Pagamento recebe callback de sucesso → atualiza fatura para PAGA ✅
11. MS Notificação consome tópico Kafka → notifica cliente
12. Se falhar em qualquer ponto → compensação → fatura fica como FALHA ❌
```
