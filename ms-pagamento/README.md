# MS Pagamento — Documentação

## Visão Geral

Microsserviço responsável por receber requisições de pagamento via PIX e orquestrar o fluxo utilizando o padrão SAGA, garantindo que uma fatura só seja marcada como paga se o comprovante for gerado com sucesso pelo MS Comprovantes.

---

## Arquitetura

**Padrão:** DDD (Domain-Driven Design)

```
com.pagamento/
├── controller/          → Entrada HTTP (REST)
├── application/service/ → Casos de uso e orquestração
├── domain/
│   ├── entity/          → Entidades do domínio
│   ├── enums/           → Enumerações
│   ├── valueobject/     → Objetos de valor (@Embeddable)
│   └── repository/      → Contratos de persistência
├── dto/                 → Objetos de transferência
└── infrastructure/
    ├── client/          → Comunicação HTTP com outros MS
    ├── config/          → Configurações (RabbitMQ, RestTemplate)
    └── messaging/       → Listeners de mensageria
```

---

## Tecnologias

| Tecnologia | Versão | Uso |
|------------|--------|-----|
| Java | 17 | Linguagem |
| Spring Boot | 3.2.5 | Framework |
| Spring Data JPA | 3.2.5 | Persistência |
| Spring AMQP | 3.1.4 | RabbitMQ (consumer) |
| Spring Validation | 3.2.5 | Validação de entrada |
| PostgreSQL | 15 | Banco de dados (Docker) |
| H2 | — | Banco de testes (in-memory) |
| Lombok | 1.18.32 | Redução de boilerplate |
| PACT | 4.6.7 | Testes de contrato |

---

## Endpoints

### POST /pagamentos

Recebe uma requisição de pagamento PIX e inicia a orquestração SAGA.

**Request:**
```
POST http://localhost:8080/pagamentos
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

**Response 202 (sucesso):**
```json
{
  "fatura_id": "uuid-da-fatura",
  "status": "COMPROVANTE_SOLICITADO",
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "data_hora_requisicao": "2022-04-10T20:03:57.116061100"
}
```

**Response 202 (falha na chamada ao MS Comprovantes):**
```json
{
  "fatura_id": "uuid-da-fatura",
  "status": "FALHA"
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

---

## Padrão SAGA (Orquestração)

### Fluxo de Estados da Fatura

```
PENDENTE → COMPROVANTE_SOLICITADO → PAGA ✅
    │               │
    └── FALHA ◄─────┘ (compensação ou timeout)
```

### Fluxo Detalhado

```
1. Recebe POST /pagamentos
2. Salva fatura com status PENDENTE
3. Chama POST /comprovantes no MS Comprovantes
4. Se recebeu 202:
   - Cria registro de Pagamento (com dados PIX)
   - Atualiza fatura para COMPROVANTE_SOLICITADO
   - Define timeout de 30 minutos
5. Se falhou a chamada HTTP:
   - Atualiza fatura para FALHA (compensação imediata)
6. Aguarda callback via RabbitMQ:
   - Fila saga.comprovante.sucesso → marca fatura como PAGA
   - Fila saga.comprovante.falha → marca fatura como FALHA
7. Se nenhum callback chegar em 30 minutos:
   - SagaTimeoutScheduler marca fatura como FALHA
```

### Idempotência

Os callbacks são idempotentes: se a fatura já não estiver em `COMPROVANTE_SOLICITADO` (ex: já foi marcada como PAGA ou FALHA), o callback é ignorado com log de warning. Isso protege contra redelivery do RabbitMQ.

### Timeout (30 minutos)

O `SagaTimeoutScheduler` roda a cada 60 segundos e verifica faturas com status `COMPROVANTE_SOLICITADO` cujo `timeoutEm` já passou. Essas faturas são marcadas como FALHA automaticamente.

### Compensação

A fatura é marcada como FALHA em três cenários:
- O MS Comprovantes está indisponível (erro HTTP)
- O MS Comprovantes não conseguiu gravar o comprovante (callback de falha via RabbitMQ)
- Timeout de 30 minutos sem callback (scheduler)

---

## Mensageria (RabbitMQ)

Este microsserviço atua como **consumer** das filas de callback da SAGA.

| Fila | Ação |
|------|------|
| `saga.comprovante.sucesso` | Marca fatura como PAGA |
| `saga.comprovante.falha` | Marca fatura como FALHA (compensação) |

**Configuração:**

| Config | Valor |
|--------|-------|
| Exchange | `saga.exchange` (direct) |
| Routing Key (sucesso) | `saga.sucesso.routing-key` |
| Routing Key (falha) | `saga.falha.routing-key` |

**Payload recebido:**
```json
{
  "identificador_comprovante": "b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
  "status": "SUCESSO",
  "motivo": null,
  "data_hora_processamento": "2022-04-10T20:03:58.000000000"
}
```

---

## Modelo de Domínio

### Entidades

**Fatura** — Representa a cobrança/invoice
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| nome | String | Nome do pagador |
| tipoDocumento | String | CPF/CNPJ |
| numeroDocumento | String | Número do documento |
| valorTransacao | BigDecimal | Valor do PIX |
| status | StatusFatura | Estado atual da SAGA |
| criadoEm | LocalDateTime | Timestamp de criação |
| atualizadoEm | LocalDateTime | Timestamp de atualização |
| timeoutEm | LocalDateTime | Deadline para callback (30 min) |

**Pagamento** — Representa o ato do pagamento PIX vinculado à fatura
| Campo | Tipo | Descrição |
|-------|------|-----------|
| id | UUID | Identificador único |
| fatura | Fatura | Fatura associada (FK, @ManyToOne LAZY) |
| identificadorComprovante | String | UUID do comprovante gerado |
| dadosBancarios | DadosBancarios | Agência, conta, dígito (@Embedded) |
| chavePix | ChavePix | Tipo e chave destino (@Embedded) |
| nomeClienteDestino | String | Nome do recebedor |
| identificacaoPix | String | Descrição do PIX |
| dataHoraTransacao | LocalDateTime | Data/hora da transação |
| criadoEm | LocalDateTime | Timestamp de criação |

### Value Objects

**DadosBancarios** (@Embeddable)
- numeroAgencia
- numeroConta
- digitoVerificadorConta

**ChavePix** (@Embeddable)
- tipoChavePixDestino (CELULAR, CPF, EMAIL, ALEATORIA)
- chavePixDestino

### Enumeração

**StatusFatura**
- `PENDENTE` — fatura criada, aguardando processamento
- `COMPROVANTE_SOLICITADO` — chamada ao MS Comprovantes feita com sucesso
- `PAGA` — comprovante gerado com sucesso (callback recebido)
- `FALHA` — erro em qualquer etapa (compensação)

---

## Testes de Contrato (PACT)

### Consumer Side

O teste PACT valida o contrato entre este microsserviço (consumer) e o MS Comprovantes (provider).

**O que é testado:**
- Ao enviar o payload correto para `POST /comprovantes`, espera-se receber:
  - HTTP 202 Accepted
  - Body com `identificador_comprovante` (UUID) e `data_hora_requisicao`

**Arquivo gerado:** `target/pacts/ms-pagamento-ms-comprovantes.json`

Este arquivo deve ser compartilhado com a Pessoa 2 (MS Comprovantes) para validação no lado provider.

---

## Testes

| Classe | Testes | Cobertura |
|--------|:------:|-----------|
| PagamentoServiceTest | 3 | Sucesso, falha por indisponibilidade, falha por erro HTTP |
| SagaServiceTest | 6 | Completar saga, compensar saga, idempotência sucesso, idempotência falha, not found (2x) |
| PagamentoControllerTest | 3 | Retorno 202, validação 400, body vazio |
| ComprovanteContractTest | 1 | Contrato PACT consumer |
| **Total** | **13** | |

---

## Banco de Dados

| Config | Valor |
|--------|-------|
| Tipo | PostgreSQL 15 (Docker) |
| Host | localhost:5432 |
| Database | db_pagamento |
| Usuário | pagamento |
| Senha | pagamento123 |
| DDL | auto (update) |

### Tabelas

**faturas**
```sql
CREATE TABLE faturas (
    id UUID PRIMARY KEY,
    nome VARCHAR,
    tipo_documento VARCHAR,
    numero_documento VARCHAR,
    valor_transacao DECIMAL,
    status VARCHAR,
    criado_em TIMESTAMP,
    atualizado_em TIMESTAMP,
    timeout_em TIMESTAMP
);
```

**pagamentos**
```sql
CREATE TABLE pagamentos (
    id UUID PRIMARY KEY,
    fatura_id UUID NOT NULL REFERENCES faturas(id),
    identificador_comprovante VARCHAR,
    numero_agencia VARCHAR,
    numero_conta VARCHAR,
    digito_verificador_conta VARCHAR,
    tipo_chave_pix_destino VARCHAR,
    chave_pix_destino VARCHAR,
    nome_cliente_destino VARCHAR,
    identificacao_pix VARCHAR,
    data_hora_transacao TIMESTAMP,
    criado_em TIMESTAMP
);
```

---

## Como Executar

### Pré-requisitos
- Java 17+
- Maven 3.8+
- Docker e Docker Compose

### 1. Subir infraestrutura (PostgreSQL + RabbitMQ)
```bash
cd ms-pagamento
docker-compose up -d
```

Verifique se subiu:
```bash
docker ps
```
Deve mostrar `postgres-pagamento` e `rabbitmq` rodando.

### 2. Rodar a aplicação
```bash
mvn spring-boot:run
```
Aguarde até ver: `Started MsPagamentoApplication`

### 3. Rodar testes unitários
```bash
mvn test
```
Esperado: `Tests run: 13, Failures: 0, Errors: 0`

### 4. Parar infraestrutura
```bash
docker-compose down
```

---

## Guia de Teste Manual (Passo a Passo)

Como o MS Comprovantes não está disponível, é possível testar todos os cenários da SAGA usando um mock HTTP e publicação manual no RabbitMQ:

### Cenário 1 — Compensação por falha HTTP (MS Comprovantes offline)

Este é o cenário padrão quando você roda sozinho.

**Passo 1:** Suba a infra e a aplicação:
```bash
docker-compose up -d
mvn spring-boot:run
```

**Passo 2:** Envie o pagamento:
```bash
curl -s -X POST http://localhost:8080/pagamentos \
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
  }' | python3 -m json.tool
```

**Resultado esperado:**
```json
{
  "fatura_id": "uuid-gerado",
  "status": "FALHA"
}
```

**O que aconteceu:** A fatura foi criada como PENDENTE, a chamada HTTP ao MS Comprovantes falhou (Connection refused), e a SAGA compensou marcando como FALHA.

**Verificar no banco:**
```bash
docker exec -it postgres-pagamento psql -U pagamento -d db_pagamento -c "SELECT id, nome, status, criado_em FROM faturas;"
```

---

### Cenário 2 — Fluxo completo (PENDENTE → COMPROVANTE_SOLICITADO → PAGA)

Para testar o ciclo inteiro sem o MS Comprovantes, usamos um mock HTTP + publicação manual no RabbitMQ.

**Passo 1:** Suba a infra e a aplicação:
```bash
docker-compose up -d
mvn spring-boot:run
```

**Passo 2:** Suba um mock do MS Comprovantes na porta 8081.

Usando Python (em outro terminal):
```bash
python3 -c "
import json
from http.server import HTTPServer, BaseHTTPRequestHandler
from datetime import datetime
import uuid

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        self.send_response(202)
        self.send_header('Content-Type', 'application/json')
        self.end_headers()
        response = {
            'identificador_comprovante': str(uuid.uuid4()),
            'data_hora_requisicao': datetime.now().isoformat()
        }
        self.wfile.write(json.dumps(response).encode())
        print(f'Comprovante gerado: {response["identificador_comprovante"]}')

print('Mock MS Comprovantes rodando em http://localhost:8081')
HTTPServer(('', 8081), Handler).serve_forever()
"
```

**Passo 3:** Envie o pagamento:
```bash
curl -s -X POST http://localhost:8080/pagamentos \
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
  }' | python3 -m json.tool
```

**Resultado esperado:**
```json
{
  "fatura_id": "uuid-gerado",
  "status": "COMPROVANTE_SOLICITADO",
  "identificador_comprovante": "uuid-do-comprovante",
  "data_hora_requisicao": "2022-04-10T20:03:57.116061100"
}
```

**Passo 4:** Anote o `identificador_comprovante` da resposta. Verifique no banco:
```bash
docker exec -it postgres-pagamento psql -U pagamento -d db_pagamento -c "SELECT id, status, timeout_em FROM faturas ORDER BY criado_em DESC LIMIT 1;"
```
Deve mostrar status `COMPROVANTE_SOLICITADO`.

**Passo 5:** Simule o callback de sucesso via RabbitMQ.

**Opção A — Via terminal (curl):**

Substitua `COLE_O_UUID_AQUI` pelo `identificador_comprovante` recebido no passo 3:
```bash
curl -s -u guest:guest -X POST http://localhost:15672/api/exchanges/%2F/saga.exchange/publish \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {"content_type": "application/json"},
    "routing_key": "saga.sucesso.routing-key",
    "payload": "{\"identificador_comprovante\":\"COLE_O_UUID_AQUI\",\"status\":\"SUCESSO\",\"data_hora_processamento\":\"2022-04-10T20:04:00.000000000\"}",
    "payload_encoding": "string"
  }'
```
Resposta esperada: `{"routed":true}`

**Opção B — Via interface web (RabbitMQ Management):**

Acesse http://localhost:15672 (login: guest / guest).

1. Vá em **Exchanges** → clique em `saga.exchange`
2. Na seção **Publish message**:
   - Routing key: `saga.sucesso.routing-key`
   - Properties: em **content_type** coloque `application/json`
   - Payload:
```json
{
  "identificador_comprovante": "COLE_O_UUID_AQUI",
  "status": "SUCESSO",
  "data_hora_processamento": "2022-04-10T20:04:00.000000000"
}
```
3. Clique **Publish message**

**Passo 6:** Verifique no banco:
```bash
docker exec -it postgres-pagamento psql -U pagamento -d db_pagamento -c "SELECT id, status, timeout_em FROM faturas ORDER BY criado_em DESC LIMIT 1;"
```
**Resultado esperado:** status = `PAGA`, timeout_em = `NULL`

---

### Cenário 3 — Compensação por callback de falha

Repita os passos 1-4 do Cenário 2, mas no passo 5 publique na routing key de falha:

**Opção A — Via terminal (curl):**
```bash
curl -s -u guest:guest -X POST http://localhost:15672/api/exchanges/%2F/saga.exchange/publish \
  -H "Content-Type: application/json" \
  -d '{
    "properties": {"content_type": "application/json"},
    "routing_key": "saga.falha.routing-key",
    "payload": "{\"identificador_comprovante\":\"COLE_O_UUID_AQUI\",\"status\":\"FALHA\",\"motivo\":\"Erro ao gravar comprovante no banco\",\"data_hora_processamento\":\"2022-04-10T20:04:00.000000000\"}",
    "payload_encoding": "string"
  }'
```

**Opção B — Via interface web (RabbitMQ Management):**

1. Vá em **Exchanges** → `saga.exchange` → **Publish message**
   - Routing key: `saga.falha.routing-key`
   - Properties: em **content_type** coloque `application/json`
   - Payload:
```json
{
  "identificador_comprovante": "COLE_O_UUID_AQUI",
  "status": "FALHA",
  "motivo": "Erro ao gravar comprovante no banco",
  "data_hora_processamento": "2022-04-10T20:04:00.000000000"
}
```

**Resultado esperado:** status = `FALHA`, timeout_em = `NULL`

---

### Cenário 4 — Timeout da SAGA (30 minutos)

Para testar sem esperar 30 minutos, você pode alterar temporariamente o timeout em `FaturaPersistenceService.java`:

```java
fatura.setTimeoutEm(LocalDateTime.now().plusSeconds(90)); // 1.5 min para teste
```

Depois repita os passos 1-4 do Cenário 2 (sem publicar callback). Aguarde ~2 minutos e verifique:
```bash
docker exec -it postgres-pagamento psql -U pagamento -d db_pagamento -c "SELECT id, status FROM faturas ORDER BY criado_em DESC LIMIT 1;"
```
**Resultado esperado:** status = `FALHA` (scheduler compensou)

---

### Cenário 5 — Validação de campos obrigatórios

```bash
curl -s -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool
```

**Resultado esperado:** HTTP 400 com lista de erros de validação.

---

### Resumo dos Cenários

| # | Cenário | O que testa | Resultado |
|---|---------|-------------|-----------|
| 1 | MS Comprovantes offline | Compensação HTTP | FALHA |
| 2 | Mock + callback sucesso | Fluxo completo SAGA | PAGA |
| 3 | Mock + callback falha | Compensação assíncrona | FALHA |
| 4 | Mock + sem callback | Timeout 30 min | FALHA |
| 5 | Body vazio/inválido | Validação de entrada | 400 |

---

## Teste Integrado (com MS Comprovantes real)

Quando o MS Comprovantes (Pessoa 2) estiver pronto e rodando na porta 8081, o fluxo funciona automaticamente sem mock.

### Pré-requisitos
- MS Pagamento rodando (porta 8080)
- MS Comprovantes rodando (porta 8081)
- PostgreSQL e RabbitMQ rodando (docker-compose up -d)
- MS Comprovantes configurado para publicar callbacks no `saga.exchange`

### Passo 1 — Subir infraestrutura e ambos os microsserviços

```bash
# Terminal 1: infra
cd ms-pagamento
docker-compose up -d

# Terminal 2: MS Pagamento
cd ms-pagamento
mvn spring-boot:run

# Terminal 3: MS Comprovantes
cd ms-comprovantes
mvn spring-boot:run
```

### Passo 2 — Enviar pagamento

```bash
curl -s -X POST http://localhost:8080/pagamentos \
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
  }' | python3 -m json.tool
```

**Resultado esperado:**
```json
{
  "fatura_id": "uuid-gerado",
  "status": "COMPROVANTE_SOLICITADO",
  "identificador_comprovante": "uuid-do-comprovante",
  "data_hora_requisicao": "..."
}
```

### Passo 3 — Aguardar callback automático

O MS Comprovantes vai processar a solicitação e publicar automaticamente no RabbitMQ:
- Se sucesso → publica em `saga.comprovante.sucesso` com routing key `saga.sucesso.routing-key`
- Se falha → publica em `saga.comprovante.falha` com routing key `saga.falha.routing-key`

Não é necessário fazer nada manualmente. O callback chega automaticamente.

### Passo 4 — Verificar resultado no banco

```bash
docker exec -it postgres-pagamento psql -U pagamento -d db_pagamento -c "SELECT id, nome, status, timeout_em FROM faturas ORDER BY criado_em DESC LIMIT 1;"
```

**Resultado esperado (fluxo feliz):** status = `PAGA`, timeout_em = `NULL`

### Passo 5 — Verificar o comprovante no MS Comprovantes

Use o `identificador_comprovante` retornado no passo 2:
```bash
curl -s http://localhost:8081/comprovantes/{identificador_comprovante} | python3 -m json.tool
```

### Fluxo completo esperado

```
Cliente → POST /pagamentos (MS Pagamento)
  → MS Pagamento cria Fatura (PENDENTE)
  → MS Pagamento chama POST /comprovantes (MS Comprovantes)
  → MS Comprovantes retorna 202 + identificador_comprovante
  → MS Pagamento cria Pagamento + atualiza Fatura (COMPROVANTE_SOLICITADO)
  → MS Comprovantes processa e publica callback no RabbitMQ
  → MS Pagamento recebe callback e marca Fatura como PAGA ✅
```

### Troubleshooting

| Problema | Causa provável | Solução |
|----------|---------------|---------|
| Status fica em COMPROVANTE_SOLICITADO | MS Comprovantes não publicou callback | Verificar logs do MS Comprovantes |
| Status vai para FALHA após 30 min | Callback não chegou a tempo | Verificar se RabbitMQ está conectado nos dois MS |
| Connection refused na porta 8081 | MS Comprovantes não está rodando | Subir o MS Comprovantes |
| Callback chega mas fatura não muda | Routing key errada | Verificar se MS Comprovantes usa `saga.sucesso.routing-key` |

---

## Integração com Outros Microsserviços

| MS | Comunicação | Direção |
|----|-------------|---------|
| MS Comprovantes | HTTP POST /comprovantes | Este MS → MS Comprovantes |
| MS Comprovantes | RabbitMQ (saga.comprovante.sucesso) | MS Comprovantes → Este MS |
| MS Comprovantes | RabbitMQ (saga.comprovante.falha) | MS Comprovantes → Este MS |

---

## Configurações (application.yml)

| Propriedade | Valor | Descrição |
|-------------|-------|-----------|
| server.port | 8080 | Porta do serviço |
| spring.datasource.url | jdbc:postgresql://localhost:5432/db_pagamento | Banco PostgreSQL |
| spring.datasource.username | pagamento | Usuário do banco |
| spring.datasource.password | pagamento123 | Senha do banco |
| spring.rabbitmq.host | localhost | Host do RabbitMQ |
| spring.rabbitmq.port | 5672 | Porta AMQP |
| app.comprovantes.url | http://localhost:8081/comprovantes | URL do MS Comprovantes |
