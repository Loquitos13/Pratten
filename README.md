# Pratten

SaaS B2B multi-tenant para restauração. Cada restaurante (tenant) tem o seu gestor,
que cria os perfis da equipa e organiza mesas, pedidos, cozinha, faturação e reservas.
O website público de reservas e a app mobile dos empregados falam com a mesma API,
sempre em sincronia.

Este repositório tem duas partes:

- `back/` – API REST em Spring Boot (é o servidor).
- `front/` – app em Flutter (em fase inicial).

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

A consola do H2 fica em `http://localhost:8080/api/h2-console` (URL `jdbc:h2:mem:pratten`, utilizador `sa`, sem password).

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

## Perfil de produção

Em produção usa-se PostgreSQL e as migrações são geridas pelo Flyway. Ativa o perfil
`prod` e define as variáveis de ambiente:

```powershell
$env:SPRING_PROFILES_ACTIVE = "prod"
$env:DB_URL = "jdbc:postgresql://localhost:5432/pratten"
$env:DB_USERNAME = "pratten"
$env:DB_PASSWORD = "..."
$env:PRATTEN_JWT_SECRET = "um-segredo-comprido-com-pelo-menos-32-bytes"
.\mvnw.cmd spring-boot:run
```

Nota: o segredo do JWT e as origens de CORS têm de ser afinados antes de ir para o ar –
por omissão estão permissivos, só para facilitar o desenvolvimento.

## Testes

```powershell
cd back
.\mvnw.cmd test
```

## O que já está implementado

- **Autenticação e papéis (JWT).** Registo de um restaurante com o seu gestor, login e
  perfil do utilizador atual. Cada papel (OWNER, MANAGER, WAITER, KITCHEN, CASHIER) só
  acede ao que lhe compete.
- **Multi-tenancy.** Os dados de cada restaurante ficam isolados. O tenant é fixado a
  partir do token, por isso ninguém consegue espreitar os dados de outro.
- **Catálogo.** Categorias, produtos (com stock) e itens de menu.
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
  mobile, POS, cozinha e website a par das alterações.

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
