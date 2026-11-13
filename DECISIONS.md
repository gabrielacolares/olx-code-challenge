# Decisões técnicas

Este documento registra as decisões que moldam a implementação atual. O projeto usa Java 25 e Spring Boot 4.1.2; as escolhas abaixo descrevem o código existente e seus limites.

## Arquitetura: MVC em camadas

Foi considerada uma arquitetura hexagonal, mas a implementação adotou MVC em camadas porque o escopo atual é uma API Spring MVC pequena, com uma entrada HTTP, um banco PostgreSQL via Spring Data JPA e uma integração externa de moderação. Não há outro adaptador de entrada, outro mecanismo de persistência nem requisito de executar o domínio fora do processo Spring.

Para este escopo, criar portas e adaptadores próprios em torno de cada operação de controller e repository acrescentaria interfaces e delegações que repetiriam as abstrações já oferecidas pelo Spring MVC e Spring Data. Isso aumentaria a navegação entre arquivos sem fornecer, hoje, uma implementação alternativa ou uma fronteira independente que precisasse ser trocada. A escolha é proporcional ao desafio; hexagonal não foi considerada tecnicamente inadequada. Ela passaria a justificar o custo se surgissem múltiplos adaptadores, armazenamento alternativo, execução independente do Spring ou regras de domínio que precisassem ser isoladas como módulo próprio.

A organização MVC atual é:

- `controller`: recebe HTTP, aplica validação de entrada, chama o service e escolhe resposta/status HTTP. `ApiExceptionHandler` centraliza a tradução de erros para HTTP.
- `dto`: contém os contratos JSON de request, response, paginação e erro. Esses tipos não são usados como modelos de persistência.
- `service`: coordena os fluxos da aplicação. `AdsService` valida limites de paginação, monta consultas, chama a moderação antes de persistir e trata falha de persistência.
- `model`: contém `Ads`, estados, decisão de moderação e modelo de página. `Ads` mantém invariantes como título, preço e imagens obrigatórios.
- `repository`: `AdsRepository` estende `JpaRepository` e oferece as consultas específicas de categoria e carregamento de imagens.
- `mapper`: `AdsResponseMapper` converte modelos em DTOs de resposta.
- `integration/moderation`: concentra cliente, DTOs do serviço externo, contrato HTTP declarativo, executor com Retry, interceptor, cooldown e configuração de rate limit.
- `config` e `exception`: configuram beans transversais e representam/traduzem falhas de integração.

O contrato `ModerationApi` com `@HttpExchange` é uma interface HTTP declarativa gerada por `HttpServiceProxyFactory`; não é uma porta de caso de uso da arquitetura hexagonal. O Retry está em `ModerationRequestExecutor`, um bean separado, para que a interceptação AOP do Resilience4j ocorra na chamada entre beans.

## Modelo e persistência JPA

`Ads` é usado diretamente como modelo e entidade JPA. Essa decisão evita manter uma classe de entidade duplicada e conversões entre ela e o modelo. O custo é que o modelo depende de anotações JPA e os serviços dependem diretamente de `AdsRepository`; portanto, o projeto não pretende ser um domínio independente de framework nem uma implementação hexagonal estrita.

As imagens são uma coleção JPA ordenada (`ad_images`). Na listagem, o banco primeiro pagina anúncios por UUID crescente e uma segunda consulta carrega as imagens apenas para os IDs daquela página. O detalhe usa `EntityGraph` para buscar o anúncio e suas imagens. Isso preserva ordenação determinística e evita carregar todas as imagens de todas as páginas.

`Ads` usa Lombok `@Data`, que gera também `equals` e `hashCode` sobre os campos da entidade, incluindo a coleção de imagens. Isso reduz boilerplate, mas vincula igualdade do objeto à coleção JPA; se a igualdade de entidades se tornar parte de conjuntos, caches ou comparações entre sessões, deve ser revista com uma estratégia explícita baseada em identidade.

## Fluxos e responsabilidades

- **Criar:** `AdsController` converte o request DTO nos argumentos de `AdsService`. O service cria e valida `Ads`, consulta a moderação, aplica o status/motivo retornado e só então persiste. Uma falha na integração interrompe o fluxo antes do repository; uma reprovação válida é persistida como `REJECTED` com motivo. A API retorna `201` para ambos os resultados persistidos.
- **Detalhe:** `AdsService.findById` reutiliza `AdsRepository.findById`; o controller responde `200` com DTO ou `404` sem corpo. UUID inválido é convertido em `400` pelo tratamento MVC.
- **Listagem:** `page` é base zero, padrão `0`; `size` padrão `20` e intervalo permitido de `1` a `100`; `category` é opcional e comparada sem distinção de maiúsculas/minúsculas. A paginação é feita no banco e a ordenação é por UUID crescente.
- **Validação:** Jakarta Validation protege o contrato HTTP em `AdsRequest`; as invariantes centrais também são verificadas por `Ads.validate()`, para não depender exclusivamente da controller.
- **Erros:** dados e parâmetros inválidos retornam `400`. Falha ou resposta inválida da moderação é traduzida para `502 Bad Gateway`, sem corpo. Ausência de anúncio retorna `404`.

## PostgreSQL, Flyway e seed

PostgreSQL é o banco de execução local e do Compose. Flyway controla a evolução do schema; Hibernate usa `ddl-auto: validate`, em vez de alterar tabelas durante a inicialização. `baseline-on-migrate` com versão `0` permite adotar o histórico em bancos existentes.

- V1 cria `ads` e `ad_images`.
- V2 adiciona `created_at`, preenche registros existentes e torna a coluna obrigatória.
- V3 importa os 57 anúncios de `src/main/resources/seed/ads.json`, preserva as datas e gera UUIDs determinísticos a partir dos IDs de origem. O histórico do Flyway impede repetir a importação em inicializações subsequentes.
- H2 é usado nos testes para isolamento; não substitui PostgreSQL na execução local do Compose.

`AdsSeedMigrationTest` verifica quantidade, campos preservados, UUID determinístico e idempotência da migração.

## MapStruct, Lombok e formatação

MapStruct gera `AdsResponseMapper` em tempo de compilação como bean Spring. O mapeamento fica fora dos DTOs e não carrega regras de negócio. `lombok-mapstruct-binding` permite que os processadores de Lombok e MapStruct trabalhem juntos.

Lombok é usado para reduzir getters, setters e construtores em DTOs e tipos selecionados; `AdsRequest` e `ModerationResponse` têm construtor sem argumentos para desserialização, enquanto os DTOs de resposta expõem getters sem setters. A entidade `Ads` é a exceção documentada acima, com `@Data`.

Spotless aplica Palantir Java Format, organiza/remove imports não utilizados, remove espaços finais e garante newline final. A configuração está no Maven pelo `spotless-maven-plugin`; `spotless:check` verifica e `spotless:apply` corrige a formatação.

## OpenAPI

Springdoc OpenAPI `3.2.0` documenta os endpoints e seus DTOs. A versão foi escolhida para a linha Spring Boot 4. A documentação usa os caminhos padrão: `/swagger-ui/index.html` e `/v3/api-docs`; os metadados ficam em `OpenApiConfiguration` e não alteram o comportamento dos endpoints.

## Execução local com Docker Compose

O Compose inicia API, PostgreSQL e mock de moderação. Dentro da rede Compose, a API usa o nome de serviço `postgres` e `moderation-api`, nunca `localhost`, para alcançar esses containers. Health checks controlam a dependência de inicialização. Em execução local fora do Compose, as URLs padrão apontam para portas publicadas no host.

## Testes

A suíte usa JUnit 5 e `spring-boot-starter-test`; Mockito para doubles; Spring Boot Test para contexto; MockMvc para testes HTTP focados; REST Assured com porta aleatória para o fluxo POST; AssertJ, Hamcrest e assertions JUnit; H2 para banco isolado e testes de Flyway. Testes parametrizados cobrem categorias, motivos de reprovação e entradas inválidas. `ModerationCooldownTest` usa relógio e sleeper substitutos para testar esperas sem pausa real; o teste do RateLimiter verifica a cadência.

Essa combinação exercita os contratos e os limites externos sem adicionar uma abstração de domínio só para facilitar mocking.

## Integração de moderação e resiliência

- `ModerationApi` declara o POST HTTP para o mock; `ModerationHttpConfiguration` cria o proxy com `HttpServiceProxyFactory` sobre `RestClient`.
- `ModerationRequestInterceptor` aguarda o cooldown compartilhado, obtém permissão do RateLimiter antes do envio e, em `429`, interpreta `Retry-After` e atualiza o cooldown para chamadas concorrentes.
- O Resilience4j Retry permite três tentativas totais e só repete `HttpClientErrorException.TooManyRequests`. `wait-duration` é zero porque a pausa de retry é orientada pelo header, não por intervalo fixo.
- `Retry-After` é dado em segundos. Se estiver ausente ou inválido, `ModerationCooldown` usa fallback de um segundo. Esperas válidas são respeitadas integralmente; a requisição original da API permanece síncrona e aberta durante a espera.
- O RateLimiter permite uma chamada a cada 251 ms e espera até 15 segundos por permissão. É local ao processo: múltiplas réplicas não compartilham a mesma cota. Não há fila nem Circuit Breaker.
- O cliente JDK usado pelo `RestClient` tem timeout de conexão de dois segundos e resposta/leitura de dez segundos, ajustáveis por variáveis de ambiente. `@TimeLimiter` não é usado porque a chamada é síncrona.
- O listener de eventos do `RetryRegistry` registra a tentativa. `ModerationClient` define e restaura o `adId` no MDC para a chamada síncrona.

## Logs

A aplicação usa SLF4J fornecido pelos starters Spring Boot, sem biblioteca adicional de logging. Mensagens parametrizadas usam campos `chave=valor` com anúncio, operação, resultado, status HTTP, tentativa ou duração quando aplicável. Os logs registram `429`/`Retry-After`, eventos de retry, falhas HTTP e de comunicação, respostas inválidas e falha de persistência após moderação. Título, descrição, imagens e corpo HTTP não são registrados. O fluxo normal de aprovação/reprovação não produz log `INFO`; o status e o motivo ficam persistidos para consulta.

O MDC é suficiente para correlacionar o `adId` durante a chamada síncrona. O projeto não configura tracing distribuído nem propagação de trace ID.

## Formato de resposta do mock

A documentação do mock exemplifica o motivo como `reason`; uma chamada manual observou `rejection_reason` para uma resposta `200` com anúncio reprovado. `ModerationResponse` aceita ambos os nomes via `@JsonAlias`, sem modificar o mock. O `200` do mock indica que a moderação foi concluída; a API persiste a decisão e responde `201` conforme seu contrato de criação.
