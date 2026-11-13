# Desafio Backend — Anúncios

API REST de anúncios construída com Java 25 e Spring Boot 4. O serviço persiste anúncios em PostgreSQL e consulta um serviço externo de moderação antes de salvar cada anúncio.

## Requisitos

- JDK 25 e Maven 3.6.3 ou superior para executar a API fora do Docker
- Docker com Docker Compose para subir a aplicação completa

## Iniciar a aplicação com Docker Compose

Na raiz do repositório, execute:

```bash
docker compose up --build
```

O Compose aguarda o PostgreSQL e o serviço mock de moderação ficarem saudáveis antes de iniciar a API.

- API: http://localhost:8080
- Health da API: http://localhost:8080/actuator/health
- Serviço mock de moderação: http://localhost:8787
- Health do mock: http://localhost:8787/health
- PostgreSQL: localhost:5432 (banco `olx_ads`)

As credenciais padrão para desenvolvimento local são `olx_user` / `olx_password`. É possível substituí-las pelas variáveis `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `POSTGRES_PORT`.

Para parar os serviços e manter os dados do PostgreSQL:

```bash
docker compose down
```

Para remover também o volume do banco e todos os dados persistidos:

```bash
docker compose down -v
```

## Executar a API fora do Docker

Suba o PostgreSQL e o serviço mock:

```bash
docker compose up -d postgres moderation-api
```

Em outro terminal, na raiz do projeto, inicie a API:

```bash
mvn spring-boot:run
```

A configuração local usa PostgreSQL em `localhost:5432` e o mock em `localhost:8787`. O perfil de testes usa H2 em memória e não precisa desses serviços.

## Banco de dados e carga inicial

O Flyway cria e valida o schema durante a inicialização. Na primeira execução em um banco, importa os 57 anúncios de `src/main/resources/seed/ads.json`; as migrações aplicadas não executam novamente nas inicializações seguintes.

Os IDs numéricos do arquivo são convertidos em UUIDs determinísticos e o campo `created_at` é preservado. Os anúncios importados recebem status `APPROVED`. Como o arquivo de seed não contém imagens, a lista de imagens desses anúncios fica vazia. Em bancos existentes sem histórico do Flyway, a configuração faz o baseline do schema e as migrações preservam as tabelas e os registros já existentes.

## Endpoints

### Criar anúncio

`POST /api/ads`

O anúncio é enviado para moderação antes de ser persistido. Esta API retorna `201 Created` quando grava o anúncio, tanto se ele for aprovado quanto reprovado; em caso de reprovação, o corpo inclui o status e o motivo. O `200 OK` descrito para `POST /v1/moderation/check` pertence ao mock externo e indica que a avaliação foi concluída; ele não define o status de `POST /api/ads`.

```bash
curl -i -X POST http://localhost:8080/api/ads \
  -H 'Content-Type: application/json' \
  -d '{
    "title": "Bicicleta urbana aro 29",
    "description": "Bicicleta em ótimo estado",
    "price": 850.00,
    "category": "esportes",
    "images": ["https://example.com/bicicleta.jpg"]
  }'
```


#### Validações e resultado da moderação

A API valida antes de chamar a moderação:

- título obrigatório, entre 10 e 100 caracteres;
- descrição obrigatória e não vazia;
- preço maior que zero;
- categoria obrigatória, escrita em letras minúsculas e pertencente à lista abaixo;
- ao menos uma imagem, com URL `http` ou `https`.

A faixa de preço por categoria é uma regra da moderação externa. Um preço positivo pode passar pela validação da API e ainda ser reprovado por estar fora da faixa. Nesse caso, o anúncio é persistido com status `REJECTED`, o motivo fica em `rejectionReason` e a API retorna `201 Created`, como nos anúncios aprovados.

| Categoria | Preço mínimo (R$) | Preço máximo (R$) |
|---|---:|---:|
| `eletronicos` | 10 | 50.000 |
| `moveis` | 20 | 30.000 |
| `veiculos` | 1.000 | 500.000 |
| `imoveis` | 10.000 | 5.000.000 |
| `esportes` | 5 | 20.000 |
| `colecionaveis` | 1 | 100.000 |

A moderação pode reprovar anúncios pelos motivos `contact_info_not_allowed`, `prohibited_item` ou `price_out_of_range`. A aprovação é retornada como `status: "APPROVED"` e sem motivo; reprovações usam `status: "REJECTED"` e o motivo em `rejectionReason`.

O contrato documentado pelo mock usa o campo `reason` na resposta de reprovação, mas a implementação atual do mock retorna `rejection_reason`. O cliente da integração aceita os dois nomes sem alterar o mock.

Dados inválidos retornam `400 Bad Request`. Falhas na comunicação ou respostas inválidas do serviço de moderação retornam `502 Bad Gateway`, sem corpo de resposta.

### Consultar anúncio por ID

`GET /api/ads/{id}`

Informe o UUID do anúncio no caminho. A API retorna `200 OK` quando encontra o anúncio, `404 Not Found` quando ele não existe e `400 Bad Request` quando o UUID é inválido.

```bash
curl -i http://localhost:8080/api/ads/00000000-0000-0000-0000-000000000001
```

### Listar anúncios

`GET /api/ads`

Parâmetros disponíveis:

| Parâmetro | Padrão | Regras |
|---|---:|---|
| `page` | `0` | Índice da página, começando em zero |
| `size` | `20` | Quantidade de anúncios por página, de 1 a 100 |
| `category` | — | Filtro opcional, sem diferenciar maiúsculas de minúsculas |

A listagem é ordenada por UUID crescente. A resposta contém `items`, `page`, `size`, `totalItems` e `totalPages`. Parâmetros inválidos retornam `400 Bad Request`.

```bash
curl -i 'http://localhost:8080/api/ads?page=0&size=20&category=esportes'
```

## Documentação interativa

Com a aplicação em execução, acesse:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Especificação OpenAPI em JSON: http://localhost:8080/v3/api-docs

## Resiliência na integração de moderação

O serviço externo aceita até 5 requisições por segundo. Para uma instância da API, o `RateLimiter` autoriza uma chamada a cada 251 ms e aguarda até 15 segundos por uma permissão. Chamadas concorrentes aguardam a mesma cota; se a permissão não for obtida nesse prazo, a criação falha sem persistir o anúncio. Em uma implantação com várias instâncias da API, cada instância tem seu próprio limitador; para compartilhar uma cota global seria necessário um limitador distribuído.

Ao receber `429 Too Many Requests`, a aplicação respeita o header `Retry-After` e pausa novas chamadas à moderação durante esse período. Se o header estiver ausente ou inválido, usa 1 segundo. O Resilience4j faz até 3 tentativas no total, repetindo somente respostas `429`. Se a integração não conseguir completar a moderação, a API retorna `502 Bad Gateway`, sem corpo.

O cliente também usa timeout de conexão de 2 segundos e timeout de resposta de 10 segundos. Os valores podem ser configurados por `MODERATION_CONNECT_TIMEOUT` e `MODERATION_RESPONSE_TIMEOUT`, com valores como `2s` ou `500ms`. A URL do serviço mock pode ser alterada por `MODERATION_BASE_URL`.

## Testes

Os testes automatizados usam H2 em memória e não exigem que PostgreSQL ou o serviço mock estejam ativos.

Para executar todos os testes:

```bash
mvn test
```

Para executar somente os testes da controller:

```bash
mvn -Dtest=AdsControllerTest,AdsControllerDetailTest,AdsControllerPaginationTest test
```
