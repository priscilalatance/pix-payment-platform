# ms-notificacao

Microsserviço responsável por consumir eventos de pagamentos realizados e registrar o resultado do envio simulado de notificações.

O serviço recebe eventos do Kafka pelo tópico `pagamento.realizado`, processa os dados da transação e persiste uma notificação no PostgreSQL.

## Responsabilidades

- Consumir eventos publicados no tópico Kafka `pagamento.realizado`;
- Desserializar o evento de pagamento;
- Simular o envio de uma notificação ao cliente;
- Persistir notificações com status `ENVIADA`;
- Executar novas tentativas em caso de falha;
- Encaminhar mensagens não processadas para a DLT;
- Persistir notificações com status `FALHA` quando um evento válido esgotar todas as tentativas.

## Tecnologias

- Java 17
- Spring Boot 3
- Spring Kafka
- Spring Data JPA
- PostgreSQL
- Maven
- Docker e Docker Compose
- Lombok
- Jackson

## Arquitetura do fluxo

```text
ms-comprovantes
       |
       | publica evento
       v
Kafka: pagamento.realizado
       |
       v
PagamentoRealizadoConsumer
       |
       v
NotificacaoService
       |
       v
NotificacaoRepository
       |
       v
PostgreSQL: db_notificacao
```

Em caso de erro:

```text
pagamento.realizado
       |
       v
retry após 1 segundo
       |
       v
retry após 2 segundos
       |
       v
retry após 4 segundos
       |
       v
pagamento.realizado-dlt
       |
       v
Persistência com status FALHA
```

## Estrutura principal

```text
src/main/java/com/notificacao
├── application
│   └── service
│       └── NotificacaoService.java
├── domain
│   ├── entity
│   │   └── Notificacao.java
│   ├── enums
│   │   └── StatusNotificacao.java
│   └── repository
│       └── NotificacaoRepository.java
├── dto
│   └── PagamentoRealizadoEvent.java
└── infrastructure
    └── kafka
        └── PagamentoRealizadoConsumer.java
```

## Evento consumido

O serviço consome mensagens JSON no seguinte formato:

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

## Tópicos Kafka

O microsserviço utiliza os seguintes tópicos:

| Tópico                           | Finalidade                              |
| -------------------------------- | --------------------------------------- |
| `pagamento.realizado`            | Tópico principal consumido pelo serviço |
| `pagamento.realizado-retry-1000` | Retry após aproximadamente 1 segundo    |
| `pagamento.realizado-retry-2000` | Retry após aproximadamente 2 segundos   |
| `pagamento.realizado-retry-4000` | Retry após aproximadamente 4 segundos   |
| `pagamento.realizado-dlt`        | Mensagens que esgotaram as tentativas   |

A configuração atual realiza:

```text
1 tentativa inicial + 3 retries = 4 execuções totais
```

## Persistência

A tabela `notificacao` contém os seguintes dados:

| Campo                       | Descrição                                       |
| --------------------------- | ----------------------------------------------- |
| `id`                        | Identificador UUID da notificação               |
| `identificador_comprovante` | Identificador do comprovante recebido no evento |
| `nome_destinatario`         | Nome do destinatário                            |
| `valor`                     | Valor da transação                              |
| `status`                    | `ENVIADA` ou `FALHA`                            |
| `tentativas`                | Quantidade de tentativas realizadas             |
| `data_envio`                | Data e hora do envio bem-sucedido               |

### Fluxo de sucesso

Quando a mensagem é processada corretamente:

```text
status = ENVIADA
tentativas = 1
data_envio = preenchida
```

### Fluxo de falha

Quando um evento válido esgota todas as tentativas:

```text
status = FALHA
tentativas = 4
data_envio = null
```

Mensagens com payload inválido são encaminhadas à DLT e registradas em log, mas não geram uma entidade `Notificacao`, pois não possuem dados confiáveis para persistência.

## Configuração local

O serviço utiliza, por padrão:

| Recurso    | Endereço                |
| ---------- | ----------------------- |
| Aplicação  | `http://localhost:8082` |
| Kafka      | `localhost:9092`        |
| PostgreSQL | `localhost:5434`        |
| Banco      | `db_notificacao`        |

Configuração esperada no `application.yml`:

```yaml
server:
  port: 8082

spring:
  application:
    name: ms-notificacao

  datasource:
    url: jdbc:postgresql://localhost:5434/db_notificacao
    driver-class-name: org.postgresql.Driver
    username: notificacao
    password: notificacao123

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect

  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: ms-notificacao-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.apache.kafka.common.serialization.StringDeserializer

app:
  kafka:
    topic:
      pagamento-realizado: pagamento.realizado
      pagamento-realizado-dlt: pagamento.realizado-dlt
```

## Pré-requisitos

Antes de iniciar o microsserviço, instale:

- Java 17;
- Maven;
- Docker;
- Docker Compose.

A infraestrutura compartilhada do projeto deve estar em execução, principalmente:

- Kafka;
- ZooKeeper;
- PostgreSQL do `ms-notificacao`.

## Como executar

Na raiz do projeto principal, suba a infraestrutura:

```powershell
docker compose up -d
```

Confira os containers:

```powershell
docker compose ps
```

Depois, dentro da pasta `ms-notificacao`, execute:

```powershell
mvn spring-boot:run
```

A aplicação será iniciada na porta `8082`.

## Como executar os testes

Dentro da pasta do microsserviço:

```powershell
mvn clean test
```

Para apenas compilar:

```powershell
mvn clean compile
```

## Teste manual do fluxo de sucesso

Com o serviço e a infraestrutura em execução, publique uma mensagem no Kafka.

No PowerShell:

```powershell
$evento = @{
    identificador_comprovante = "b819cc65-f6f0-478c-8bb7-69ee1c4f6402"
    nome = "Giovanni Vicente"
    numero_documento = "50329291076"
    valor_transacao = 23.99
    chave_pix_destino = "11948755536"
    nome_cliente_destino = "Fernando Augusto"
    data_hora_transacao = "2026-07-11T19:00:00"
}

$json = $evento | ConvertTo-Json -Compress

$json | docker compose exec -T kafka kafka-console-producer `
  --bootstrap-server localhost:9092 `
  --topic pagamento.realizado
```

O log esperado é semelhante a:

```text
Evento de pagamento recebido. Comprovante: b819cc65-f6f0-478c-8bb7-69ee1c4f6402
Notificacao enviada para Giovanni Vicente - PIX de R$ 23.99 realizado com sucesso
```

## Consultar as notificações

Na raiz onde está o `docker-compose.yml` compartilhado:

```powershell
docker compose exec postgres-notificacao psql `
  -U notificacao `
  -d db_notificacao `
  -c "SELECT identificador_comprovante, nome_destinatario, status, tentativas, data_envio, valor FROM notificacao;"
```

Resultado esperado para o fluxo de sucesso:

```text
status = ENVIADA
tentativas = 1
data_envio preenchida
```

## Consultar os tópicos Kafka

```powershell
docker compose exec kafka kafka-topics `
  --bootstrap-server localhost:9092 `
  --list
```

Devem existir tópicos semelhantes a:

```text
pagamento.realizado
pagamento.realizado-retry-1000
pagamento.realizado-retry-2000
pagamento.realizado-retry-4000
pagamento.realizado-dlt
```

## Teste integrado

O fluxo integrado pode ser iniciado pelo endpoint do `ms-comprovantes`:

```http
POST http://localhost:8081/comprovantes
```

Após o processamento, o `ms-comprovantes` publica o evento no tópico `pagamento.realizado`.

O `ms-notificacao` deve consumir a mensagem e persistir uma linha com:

```text
status = ENVIADA
tentativas = 1
```

Fluxo validado:

```text
POST /comprovantes
→ ms-comprovantes
→ Kafka
→ ms-notificacao
→ PostgreSQL
```

## Observações

- O microsserviço não possui endpoint HTTP de negócio;
- Sua entrada principal ocorre por eventos Kafka;
- Os tópicos de retry e DLT são configurados pelo `@RetryableTopic`;
- O processamento da DLT não relança exceções de desserialização;
- Payloads inválidos permanecem disponíveis na DLT para análise;
- O serviço utiliza banco de dados próprio, separado dos demais microsserviços.
