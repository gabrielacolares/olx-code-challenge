# AI_USAGE

Este arquivo registra o uso de ChatGPT/Codex neste projeto. Os textos em **Prompt (paráfrase)** resumem as solicitações; não são transcrições literais. As sugestões foram revisadas em relação ao código, ao README do desafio e ao contrato local do mock.

## Busca de anúncio por UUID

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Implemente `GET /api/ads/{id}` para buscar um anúncio por UUID. Siga o contrato do desafio: `200` quando encontrado, `404` quando ausente e `400` para UUID inválido. Reutilize repository, service e mapper existentes; evite camadas desnecessárias e cubra os resultados nos testes da controller.
- **Resultado revisado:** O fluxo reutiliza `AdsRepository.findById`, `AdsService`, `AdsResponseMapper` e a resposta já existente. Os testes cobrem `200`, `404` e UUID inválido.
- **Validação registrada:** `mvn test`.

## Listagem paginada

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Implemente a listagem paginada de anúncios com filtro opcional por categoria, usando paginação no banco e ordenação estável. Preserve os contratos existentes, valide os parâmetros e inclua testes unitários do service e testes HTTP da controller.
- **Resultado revisado:** Spring Data executa a paginação; a API usa `page=0`, `size=20` por padrão, limita `size` a 1–100, filtra categoria sem diferenciar maiúsculas/minúsculas e ordena por UUID. As imagens da página são carregadas em uma consulta limitada aos IDs retornados.
- **Decisões de revisão:** `AdsPage` representa os dados paginados; `AdsPageResponse` representa a resposta HTTP. Os testes da controller usam MockMvc.
- **Validação registrada:** `mvn test`.

## Documentação OpenAPI

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Documente os endpoints, DTOs, validações e respostas de erro que já existem usando Springdoc OpenAPI compatível com a versão do Spring Boot do projeto. Disponibilize Swagger UI sem alterar o comportamento de negócio.
- **Resultado revisado:** Foi usada a linha Springdoc compatível com Spring Boot 4; os contratos documentados correspondem aos endpoints implementados.
- **Validação registrada:** `mvn test` e consulta de `/v3/api-docs` quando disponível.

## Retry, `Retry-After` e cooldown

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Trate o mock como serviço externo. Use Resilience4j para repetir somente respostas `429`, com até três tentativas totais. Respeite `Retry-After`, use fallback de um segundo quando o header estiver ausente ou inválido e não repita erros permanentes nem timeouts. Cubra o comportamento sem esperas longas nos testes.
- **Resultado revisado:** `@Retry(name = "moderation")` permite até três tentativas totais e só repete `429`. O cooldown compartilhado impede novos envios durante o período informado; o fallback é de um segundo. O retry e o cooldown são componentes separados.
- **Validação registrada:** testes de resposta aprovada, `429`, esgotamento, `400` e cooldown com relógio/sleeper falsos; `mvn test`.

## Rate limiter da integração

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Considere o limite documentado pelo mock, de cinco requisições por segundo. Configure um rate limiter com margem, limite cada tentativa HTTP — inclusive retries — e defina um tempo máximo de espera por permissão. Analise a janela móvel e cubra concorrência nos testes.
- **Resultado revisado:** A configuração permite uma chamada a cada 251 ms, com espera máxima de 15 segundos por permissão. O limite é local a cada instância da aplicação; não constitui coordenação global entre réplicas.
- **Revisão da cadência:** Uma verificação concorrente anterior encontrou respostas `429`; a análise considerou a janela móvel do mock e o período fixo do RateLimiter. A cadência foi ajustada para 251 ms. Na validação registrada à época, oito criações concorrentes retornaram `201` sem `429`; os anúncios temporários daquela validação foram removidos.
- **Validação registrada:** testes dos valores de configuração e `mvn test`.

## Timeouts HTTP

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Configure timeouts explícitos de conexão e leitura para o cliente HTTP da moderação. Mantenha Retry restrito a `429`; não use `@TimeLimiter` em uma chamada síncrona. Cubra timeout e falha de comunicação nos testes.
- **Resultado revisado:** O cliente JDK usa timeout de conexão de dois segundos e de resposta/leitura de dez segundos, configuráveis por `MODERATION_CONNECT_TIMEOUT` e `MODERATION_RESPONSE_TIMEOUT`. Timeout não é repetido.
- **Validação registrada:** teste de timeout simulado e `mvn test`.

## Cliente HTTP declarativo da moderação

- **Ferramenta:** ChatGPT/Codex.
- **Prompt:** Substitua a chamada manual ao `RestClient` por uma interface Spring HTTP Service Client com `@HttpExchange`. Preserve o Retry no bean executor, o rate limit e cooldown no interceptor, e propague o `adId` via MDC para logs de retry.
- **Resultado revisado:** `ModerationApi` é criada por `HttpServiceProxyFactory`; `ModerationRequestExecutor` mantém `@Retry`; o interceptor aplica rate limit e cooldown e trata `Retry-After`; `ModerationClient` define e restaura o MDC. A assinatura do contrato HTTP não recebe o identificador do anúncio.
- **Validação registrada:** o código e os testes foram compilados com `mvn -DskipTests test`; os testes não foram executados nessa refatoração.

## Simulações de resiliência em runtime 

- **Ferramenta:** ChatGPT/Codex.
- **Prompt** Simule chamadas ao mock e à API para verificar rate limit, retry após `429`, retries esgotados, timeout, concorrência e mock inacessível. Registre status HTTP, duração e logs; diferencie respostas HTTP de falhas de conexão.
- **Resultados revisados:** Chamadas concorrentes diretas ao mock produziram `429`; a API recebeu `429`, registrou retry e conseguiu uma aprovação. Sob carga sustentada, a API recebeu três `429`, fez duas tentativas adicionais e retornou `502` após esgotar o retry. Oito chamadas concorrentes pela API não produziram `429` observado; seis retornaram `201` e duas `502` por `500` aleatório do mock. Com timeout de leitura reduzido para 400 ms em uma instância auxiliar, foram registrados timeouts como `ResourceAccessException`. Com o mock inacessível, a API retornou `502` e registrou falha de comunicação.
- **Observação sobre os dados:** As chamadas aprovadas dessas simulações criaram anúncios no PostgreSQL compartilhado; esses registros não foram removidos.

## Padronização dos testes da controller e README

- **Ferramenta:** ChatGPT/Codex.
- **Prompt** Padronize em inglês nomes de testes, helpers e variáveis dos testes da controller. Use fixtures em inglês, mas preserve mensagens e valores de domínio que fazem parte do contrato da API. Atualize o README com os comandos de teste e as informações atuais de Retry/timeout.
- **Resultado revisado:** Os nomes e fixtures dos testes da controller estão em inglês; mensagens de erro da API continuam em português. O README documenta `mvn test`, o comando focado da controller, Retry e timeouts.
- **Validação registrada:** oito testes da controller passaram, sem falhas ou erros; `git diff --check` no README.
## Compatibilidade com a resposta de reprovação do mock

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Investigue por que anúncios com preço fora da faixa retornavam `502`. Compare o contrato documentado da moderação com a resposta do mock em execução, sem alterar o mock. Faça o cliente aceitar o formato realmente retornado e cubra o caso com teste. Documente as regras de negócio, a divergência observada e diferencie o `200` da moderação do status do endpoint de criação da nossa API.
- **Resultado revisado:** A documentação do mock exemplifica a resposta de reprovação com `reason`; uma chamada manual ao mock retornou HTTP `200` com `rejection_reason`. O DTO foi simplificado para mapear ambos os nomes para a propriedade `reason`, e foi adicionado um teste de desserialização da resposta `rejection_reason`. O mock permaneceu inalterado. O README passou a descrever validações de entrada, categorias e faixas de preço da moderação, respostas de criação, resiliência e a diferença entre os status HTTP dos dois serviços. Os nomes dos testes de moderação foram padronizados em inglês.
- **Decisão revisada:** As faixas específicas de preço são avaliadas pelo serviço de moderação. Um preço positivo fora da faixa é uma reprovação de negócio, não uma falha da request. O `200` do mock confirma que a avaliação foi concluída; `201` é o contrato escolhido para `POST /api/ads`, que persiste o anúncio com status aprovado ou reprovado.
- **Validação registrada:** A resposta real do mock foi conferida manualmente no cliente HTTP. `git diff --check` passou para as alterações documentais. Após o ajuste do DTO e do teste de desserialização, a suíte Maven não foi executada nesta sessão; o teste precisa ser validado antes da entrega.


## Migrações de schema e carga inicial com Flyway

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Gerencie a evolução do schema PostgreSQL e a carga inicial de anúncios com migrações Flyway, mantendo compatibilidade com bancos existentes, evitando que o Hibernate altere o schema e garantindo que os dados de seed não sejam duplicados em reinicializações.
- **Resultado revisado:** O `pom.xml` inclui `spring-boot-starter-flyway` e o módulo de banco PostgreSQL do Flyway. A configuração usa baseline na versão 0 para bancos existentes e `ddl-auto: validate`. V1 cria `ads` e `ad_images`; V2 adiciona e preenche `created_at`; V3 importa `src/main/resources/seed/ads.json`, gera UUIDs determinísticos a partir dos IDs de origem e insere os anúncios com status aprovado. O histórico do Flyway impede reaplicar a carga nas inicializações seguintes.
- **Cobertura existente:** `AdsSeedMigrationTest` confere os 57 anúncios, valores preservados, UUID determinístico, imagens vazias e ausência de duplicação após uma nova chamada a `migrate()`.
- **Validação registrada nesta atualização:** o teste de migração foi conferido no código; `mvn test` não foi executado ao editar este registro documental.

## Criação dos logs de moderação e persistência

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Adicione logs operacionais estruturados aos fluxos de moderação e persistência, com contexto útil e níveis adequados, sem registrar conteúdo do anúncio, payloads HTTP, credenciais ou tokens; evite repetir a mesma exceção em camadas diferentes.
- **Resultado revisado:** Os logs usam SLF4J disponibilizado pelos starters Spring Boot, sem dependência de logging adicional. O interceptor registra `429`, `Retry-After` e duração; o listener do `RetryRegistry` registra o número da tentativa e o status; `ModerationClient` registra falha de comunicação, erro HTTP, resposta inválida, esgotamento do retry e falha inesperada; `AdsService` registra falha de persistência após a moderação. O `adId` é correlacionado pelo MDC durante a chamada síncrona. Títulos, descrições, imagens e corpos HTTP não são registrados. Aprovação e reprovação não geram logs `INFO`.
- **Cobertura existente:** `ModerationRequestInterceptorTest` cobre início do cooldown em `429`, ausência de cooldown em outros status e limite da cadência do RateLimiter. Os testes focam no comportamento da integração, sem assertions frágeis sobre texto de log.
- **Validação registrada nesta atualização:** os testes e pontos de logging foram conferidos no código; `mvn test` não foi executado ao editar este registro documental.


## Lombok

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Reduza código repetitivo de acesso e construção em DTOs sem usar `@Data` indiscriminadamente nem adicionar setters onde não são necessários. Configure o processamento de anotações compatível com o Java do projeto e com MapStruct.
- **Resultado revisado:** Lombok `1.18.42` é usado em DTOs e tipos simples de modelo para gerar getters, setters apenas nos objetos mutáveis e construtores sem argumentos onde a desserialização exige. As entidades JPA e os serviços não usam anotações Lombok. O processador é configurado no Maven junto de `lombok-mapstruct-binding`.
- **Validação registrada:** o projeto compilou com processamento de anotações nas compilações Maven registradas acima.

## MapStruct

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Mapeie modelos de anúncio para DTOs de resposta usando MapStruct e preserve o contrato JSON e a separação entre modelo e camada web.
- **Resultado revisado:** MapStruct `1.6.3` gera `AdsResponseMapper` como bean Spring (`componentModel = "spring"`), convertendo `Ads` em `AdsResponse` e `AdsPage` em `AdsPageResponse`. O processador é configurado no Maven com o binding de Lombok; não há mapeamento manual duplicado nos DTOs.
- **Validação registrada:** compilação Maven com os processadores de anotação habilitados; testes de controller usam o mapper gerado.

## Formatação com Spotless

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Configure uma verificação automática de estilo Java consistente com as convenções adotadas no projeto.
- **Resultado revisado:** O Maven usa `spotless-maven-plugin` `2.43.0` com Palantir Java Format `2.71.0`, ordem/remoção de imports não usados, remoção de espaços finais e newline no fim dos arquivos. O objetivo `check` está configurado no build; `spotless:apply` pode ser executado para formatar arquivos.
- **Validação registrada:** a configuração está presente no `pom.xml`; esta atualização do AI_USAGE não executou `spotless:check`.

## Tecnologias de teste

- **Ferramenta:** ChatGPT/Codex.
- **Prompt (paráfrase):** Use as dependências de teste existentes para cobrir regras de serviço, contrato HTTP, integração de moderação, migrações e persistência sem substituir a cobertura anterior.
- **Resultado revisado:** A suíte usa JUnit 5 e `spring-boot-starter-test`, com Mockito para doubles, `@SpringBootTest` para testes com contexto, MockMvc para testes HTTP focados, REST Assured em servidor aleatório para o POST, AssertJ/Hamcrest/JUnit assertions para verificações, H2 para persistência isolada e testes Flyway para a carga de seed. `@ParameterizedTest` cobre conjuntos de categorias, motivos e entradas inválidas.
- **Validação registrada:** as tecnologias e classes de teste foram conferidas no `pom.xml` e em `src/test/java`. Nenhum teste foi executado ao atualizar este registro.
