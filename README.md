# Desafio Backend — Anúncios

API de anúncios construída com Java 25 e Spring Boot.

## Requisitos

- JDK 25 e Maven 3.6.3 ou superior para executar a API localmente
- Docker com Docker Compose

## Iniciar a API, PostgreSQL e mock

Na raiz do repositório, execute:

    docker compose up --build

O Compose aguarda o PostgreSQL e o mock ficarem saudáveis antes de iniciar a API.

- API: http://localhost:8080
- Mock de moderação: http://localhost:8787
- Health do mock: http://localhost:8787/health
- PostgreSQL: localhost:5432 (banco `olx_ads`)

As credenciais padrão para desenvolvimento local são `olx_user` / `olx_password`. Elas podem ser substituídas pelas variáveis `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `POSTGRES_PORT`.

Para encerrar os serviços sem apagar os dados do PostgreSQL:

    docker compose down

Os dados ficam no volume `postgres_data`. Para apagar também o banco e seus dados:

    docker compose down -v

## Executar a API localmente fora do Docker

Suba o PostgreSQL e o mock pelo Compose:

    docker compose up -d postgres moderation-api

Depois, em outro terminal na raiz do projeto:

    mvn spring-boot:run

A configuração local conecta ao PostgreSQL em `localhost:5432` e ao mock em `localhost:8787`. O perfil de testes utiliza H2 em memória.

Para encerrar o PostgreSQL e o mock:

    docker compose down

## Criar anúncio

POST http://localhost:8080/api/ads

    curl -i -X POST http://localhost:8080/api/ads \
      -H 'Content-Type: application/json' \
      -d '{
        "title": "Bicicleta urbana aro 29",
        "description": "Bicicleta em ótimo estado",
        "price": 850.00,
        "category": "esportes",
        "images": ["https://example.com/bicicleta.jpg"]
      }'

O anúncio é enviado ao serviço de moderação antes de ser salvo. A resposta é 201 Created e contém o status e, quando reprovado, o motivo. Dados inválidos retornam 400; falhas do serviço de moderação retornam 502.
## Consultar anúncio

GET http://localhost:8080/api/ads/{id}

Informe o UUID do anúncio no caminho. A API retorna `200 OK` com os dados do anúncio, `404 Not Found` quando não existe e `400 Bad Request` quando o UUID é inválido.

    curl -i http://localhost:8080/api/ads/00000000-0000-0000-0000-000000000001

## Listagem de anuncios

GET http://localhost:8080/api/ads

A listagem aceita page (base zero, padrão 0), size (padrão 20, de 1 a 100) e category (filtro opcional, sem diferenciar maiúsculas de minúsculas). A ordenação e por UUID crescente. A resposta inclui ‘items’, page, size, totalItems e totalPages. Parâmetros inválidos retornam 400.

    curl -i 'http://localhost:8080/api/ads?page=0&size=20&category=esportes'


## Documentacao interativa da API

Com a aplicação iniciada (por exemplo, com docker compose up --build), acesse:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Especificação OpenAPI em JSON: http://localhost:8080/v3/api-docs

A documentação descreve os endpoints de criação, consulta por UUID e listagem paginada, incluindo validações e respostas de erro.
