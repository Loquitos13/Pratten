# Pratten

SaaS B2B multi-tenant para restauração. Cada restaurante (tenant) tem o seu gestor,
que cria os perfis da equipa e organiza mesas, pedidos, cozinha, faturação e reservas.
O website público de reservas e a app mobile dos empregados falam com a mesma API,
sempre em sincronia.

Este repositório tem duas partes:

- `back/` – API REST em Spring Boot (é o servidor).
- `front/` – app em Flutter que consome a API (empregado, cozinha e gestor).

## Requisitos

- Java 21
- Não precisas de instalar o Maven – usa o wrapper (`mvnw`) que vem no `back/`.
- Para desenvolvimento não é preciso base de dados: corre em H2 na memória.

## Arrancar o servidor (desenvolvimento)

A partir da pasta `back/`:

```powershell
cd back
.\mvnw.cmd spring-boot:run
```

Fica disponível em `http://localhost:8080/api` (o `/api` faz parte de todos os caminhos).

Ao arrancar, o perfil `dev` cria uma base de dados em memória e semeia um restaurante
de demonstração. Nos logs aparecem as credenciais prontas a usar:

- `ana@demo.pt` – OWNER (o gestor)
- `joao@demo.pt` – WAITER (empregado de mesa)
- `maria@demo.pt` – KITCHEN (cozinha)

A password de todos é `demo1234` e o tenant tem o slug `demo`.

**Superadmin da plataforma** (consola de suporte, fora do tenant):

- Email: `superadmin@pratten.pt`
- Password: `superadmin1234`
- Login: `POST /api/platform/auth/login` (sem slug de restaurante)

Com o token de superadmin podes gerir tenants em `/api/platform/tenants/*`. Para **resolver
problemas dentro de um restaurante**, abre uma sessão remota:

```powershell
# 1. Login superadmin → token platform
# 2. Iniciar sessão remota (devolve token OWNER temporário)
POST /api/platform/tenants/{tenantId}/remote-session
{ "reason": "Cliente bloqueado nas mesas", "durationMinutes": 60 }

# 3. Usa o token devolvido em /tables, /orders, /reports, etc.
# 4. Encerrar quando terminares
POST /api/platform/tenants/{tenantId}/remote-session/{sessionId}/end
```

Monitorização e alertas platform:

- `GET /api/platform/health` - estado de todos os tenants
- `GET /api/platform/notifications` - alertas in-app (offline, latência, backlog SSE)
- `GET /api/platform/notifications/stream` - SSE em tempo real para a consola

Em produção, eventos assíncronos usam **RabbitMQ** (`PRATTEN_MESSAGING_RABBIT_ENABLED=true`)
com retry e **DLQ** (`pratten.platform.events.dlq`) para mensagens que falham após 5 tentativas.
Em dev processam-se inline (sem broker).

A consola do H2 fica em `http://localhost:8080/api/h2-console` (URL `jdbc:h2:mem:pratten`, utilizador `sa`, sem password).

## Testar com Postman

Na pasta `teste/` há uma collection Postman, um environment local e ficheiros JSON
com os corpos dos pedidos (`teste/bodies/`). Importa no Postman:

1. `teste/Pratten.local.postman_environment.json`
2. `teste/Pratten.postman_collection.json`

Selecciona o environment **Pratten Local**, arranca o servidor e corre primeiro
**Auth → Login staff (OWNER)** ou **Platform → Login superadmin** - os tokens
ficam guardados automaticamente nas variáveis do environment.

## Experimentar a API

Primeiro faz login para obteres um token:

```powershell
curl.exe -X POST http://localhost:8080/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{\"slug\":\"demo\",\"email\":\"ana@demo.pt\",\"password\":\"demo1234\"}'
```

Depois usa o token nos pedidos autenticados:

```powershell
curl.exe http://localhost:8080/api/tables -H "Authorization: Bearer <TOKEN>"
```

O tenant vem sempre dentro do token, por isso não precisas de mais nada. A única
exceção é o website público, que se identifica pelo cabeçalho `X-Tenant-ID`.

## Arrancar a app (front)

A app liga-se ao servidor, por isso arranca primeiro o `back/`. Depois, a partir
da pasta `front/`:

```powershell
cd front
flutter pub get
flutter run
```

Por omissão liga a `http://localhost:8080/api`. Para trocar o endereço (por exemplo
no emulador Android, que vê o PC em `10.0.2.2`):

```powershell
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080/api
```

Cada utilizador entra com o slug do restaurante, email e palavra-passe, e é levado
para a sua área conforme o perfil:

- **Empregado** – as suas mesas, abrir contas, apontar pedidos e faturar.
- **Cozinha** – fila de preparação, a avançar o estado de cada item.
- **Gestor** – atalhos para mesas, cozinha e relatórios de vendas.

Podes experimentar já com as credenciais de demonstração acima.

## Infraestrutura local (PostgreSQL + RabbitMQ)

Na raiz do repositório:

```powershell
docker compose up -d
```

| Serviço | URL / Porta | Credenciais |
|---------|-------------|-------------|
| PostgreSQL | `localhost:5433` | `pratten` / `pratten` (db: `pratten`) |
| RabbitMQ AMQP | `localhost:5672` | `pratten` / `pratten` |
| RabbitMQ UI | http://localhost:15672 | `pratten` / `pratten` |
| **Redis** | **6379** | password: `pratten` |

Com Redis activo (`REDIS_ENABLED=true`, perfil `prod`), login lockout e SSE sync/notifications
funcionam em **várias instâncias** da API em paralelo.

| Componente | Canal / chave Redis | Função |
|------------|---------------------|--------|
| Login lockout | `pratten:login-attempts:{slug}:{email}` | Contador partilhado entre instâncias |
| SSE tenant | Pub/Sub `pratten.sync.events` | Fan-out de eventos `/sync/stream` |
| SSE platform | Pub/Sub `pratten.platform.notifications` | Fan-out de `/platform/notifications/stream` |
| Ligações SSE | `pratten:sync-connections:{tenantId}` | Contagem global para health/degraded |

Em dev (`pratten.redis.enabled=false`, default) tudo corre em memória local - uma só instância.

Aplicar migrações Flyway (V1–V9):

```powershell
cd back
mvn flyway:migrate "-Dflyway.url=jdbc:postgresql://localhost:5433/pratten" "-Dflyway.user=pratten" "-Dflyway.password=pratten"
```

Arrancar em produção local com broker:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5433/pratten"
$env:RABBITMQ_HOST = "localhost"
$env:REDIS_ENABLED = "true"
$env:REDIS_PASSWORD = "pratten"
$env:PRATTEN_MESSAGING_RABBIT_ENABLED = "true"
.\mvnw.cmd spring-boot:run
```

## Perfil de produção

Em produção usa-se PostgreSQL e as migrações são geridas pelo Flyway (inclui a V5
do superadmin). Para aplicar só as migrações sem arrancar a app:

```powershell
cd back
mvn flyway:migrate
```

Por omissão liga a `jdbc:postgresql://localhost:5432/pratten` (utilizador `pratten`).
Ajusta `flyway.url`, `flyway.user` e `flyway.password` no `pom.xml` ou passa
`-Dflyway.url=...` se a base for outra.

Em desenvolvimento o perfil `dev` usa Hibernate `create-drop` e não corre o Flyway;
o superadmin é criado automaticamente pelo seed.

Ativa o perfil `prod` e define as variáveis de ambiente:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/pratten"
$env:DB_USERNAME = "pratten"
$env:DB_PASSWORD = "..."
$env:PRATTEN_JWT_SECRET = "um-segredo-comprido-com-pelo-menos-32-bytes"
$env:PRATTEN_PLATFORM_ADMIN_EMAIL = "admin@pratten.pt"
$env:PRATTEN_PLATFORM_ADMIN_PASSWORD = "uma-password-forte-12+"
.\mvnw.cmd spring-boot:run
```

Na primeira arrancada com a base vazia, o perfil `prod` cria automaticamente o
superadmin a partir de `PRATTEN_PLATFORM_ADMIN_EMAIL` e `PRATTEN_PLATFORM_ADMIN_PASSWORD`
(mínimo 12 caracteres). Em dev isto continua a ser feito pelo seed.

Nota: o segredo do JWT e as origens de CORS têm de ser afinados antes de ir para o ar –
por omissão estão permissivos, só para facilitar o desenvolvimento.

## Testes

```powershell
cd back
.\mvnw.cmd test
```

O CI (GitHub Actions) corre `mvn test` em cada push/PR. Testes de integração:

| Teste | Infra | O que valida |
|-------|-------|--------------|
| `FlywayMigrationIntegrationTest` | PostgreSQL (Testcontainers) | Migrações V1–V9 |
| `RedisLoginAttemptStoreIntegrationTest` | Redis | Lockout partilhado entre instâncias |
| `RedisSyncPubSubIntegrationTest` | Redis | Pub/Sub SSE sync |
| `RabbitPlatformEventIntegrationTest` | RabbitMQ | Eventos platform assíncronos |
| `PlatformStockAlertIntegrationTest` | H2 + webhook local | Alerta `LOW_STOCK` + entrega |

Requer Docker local para os testes Testcontainers (skipped automaticamente sem Docker).

## Documentação API (dev)

Com o servidor em perfil `dev`, abre `http://localhost:8080/api/swagger-ui.html`.
Usa **Authorize** com o JWT (`Bearer …`) obtido em `/auth/login` ou `/platform/auth/login`.

## O que já está implementado

- **Autenticação e papéis (JWT).** Registo de um restaurante com o seu gestor, login e
  perfil do utilizador actual. Cada papel (OWNER, MANAGER, WAITER, KITCHEN) só acede
  ao que lhe compete.
- **Superadmin de plataforma.** Login separado, gestão de tenants, diagnósticos,
  reset de passwords, **sessão remota como OWNER**, monitorização de saúde,
  notificações in-app e fila assíncrona (RabbitMQ em produção). Alertas **LOW_STOCK**
  quando ingredientes descem abaixo do mínimo (webhook/email configurável).
- **Multi-tenancy.** Os dados de cada restaurante ficam isolados. O tenant é fixado a
  partir do token, por isso ninguém consegue espreitar os dados de outro.
- **Catálogo.** Categorias, produtos (com stock), receitas por prato e dedução automática
  de stock ao enviar pedidos para cozinha.
- **Mesas.** Criação e edição de mesas, estado (livre, ocupada, reservada, a limpar) e
  atribuição de mesas a um empregado.
- **Pedidos.** Abertura de conta por mesa, adição e remoção de itens, gorjetas e
  pagamentos.
- **Cozinha.** Fila de preparação e mudança de estado de cada item.
- **Reservas.** Ciclo completo, incluindo reservas que chegam do website.
- **Website público.** Endpoints abertos para consultar o menu, ver a disponibilidade
  de mesas e criar reservas.
- **Relatórios do gestor.** Valores por empregado – vendas, gorjetas, número de mesas e
  de pedidos.
- **Sincronização em tempo real.** Um fluxo de eventos (SSE) em `/sync/stream` mantém
  mobile, POS, cozinha e website a par das alterações. Com Redis, o fan-out SSE
  funciona entre várias instâncias da API.
- **App multi-plataforma (Flutter).** Login por restaurante, encaminhamento por perfil
  e os fluxos do empregado (mesas, pedidos, pagamento), da cozinha e do gestor.

## Estrutura (back/)

- `domain/` – entidades e enums do negócio.
- `repository/` – acesso aos dados (Spring Data JPA).
- `service/` – regras de negócio.
- `web/` – controladores REST.
- `dto/` – objetos de entrada e saída da API.
- `security/` – JWT e filtro de autenticação.
- `tenant/` – suporte de multi-tenancy.
- `config/` – segurança, resolução de tenant e dados de demonstração.
- `resources/db/migration/` – migrações Flyway (produção).
