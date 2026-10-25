# NEXO | Laboratório de decisões

API educativa para explorar aplicações civis da tecnologia nuclear em saúde, agricultura, indústria, pesquisa e energia. O estudante consulta um cenário, compara alternativas, registra uma decisão e acessa explicações e itens de mito ou fato.

Projeto acadêmico do Challenge FIAP ADS 2026, com entregas de Java Advanced e DevOps & Cloud. O contexto do desafio envolve a AMAZUL e a Marinha; o NEXO não é um produto oficial dessas instituições.

## Acesso rápido por disciplina

| Quero fazer | Guia |
| --- | --- |
| Executar a API no IntelliJ/Maven | [Execução Java](#executar-a-api) |
| Entender o contrato HTTP e o Oracle | [API](README.md#java-advanced) · [Oracle](README.md#banco-oracle) |
| Executar em container no computador | [Docker local](docs/docker.md) |
| Planejar a VM sem criar recursos | [Azure e scripts](docs/azure.md#planejamento-seguro) |
| Excluir e comprovar a limpeza após a demonstração | [Exclusão dos recursos](docs/azure.md#exclusão-e-comprovação) |

Turma: 2TDSPB

| Integrante | RM |
| --- | --- |
| Luis Fillipe Rodrigues Seripieri | 567918 |
| Gabriel Rocha de Souza | 567726 |
| Luiz Kichimoto Valdevino | 567023 |
| Guilherme Soares | 568227 |
| Gabriel Stuani | 566682 |

Repositório: [Guilherme-Soares00/NEXO-api](https://github.com/Guilherme-Soares00/NEXO-api)
Entrega interna:.

## Tecnologias e funcionamento

Java 25, Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Oracle JDBC e springdoc 2.9.1. Controllers recebem DTOs validados; serviços controlam transações; repositories Spring Data JPA fazem leituras e escritas nas quatro entidades.

A API oferece CRUD de cenários, alternativas, decisões e mitos/fatos, busca parcial pelo título e paginação. Também permite consultar e cadastrar alternativas de um cenário, valida os rótulos A/B e devolve erros JSON padronizados. As entidades JPA não são expostas diretamente nas respostas.

Os recursos estão organizados em pacotes de domínio, com DTOs, controllers, serviços e repositories. As configurações compartilhadas ficam em `configuration/`; validações e tratamento de erros comuns, em `shared/`.

Enunciado vigente: `2TDS Agosto - Marinha 2026 - 1º Semestre V2.pdf`, páginas 33–36 para Java. Na página 38, Database adia procedures, functions com cursor e sua integração para a Sprint 2. O CRUD Java atual não depende dessas rotinas.

Oracle é o perfil padrão. A aplicação não cria tabelas, sequences ou dados. Para instalação manual em schema próprio sem objetos NEXO, usar [docs/sql/schema-oracle.sql](docs/sql/schema-oracle.sql). O schema deve atender a [README.md#banco-oracle](README.md#banco-oracle). IDs usam as sequences existentes com allocationSize=1; DATA_ESCOLHA continua sendo gerada no banco e preservada no PUT.

## Alinhamento com o Oracle 19c

Tabelas NEXO_CENARIO, NEXO_ALTERNATIVA, NEXO_DECISAO e NEXO_MITO_FATO. Mito ou Fato pertence ao cenário e é compartilhado por A/B. Não há campo de ordenação no modelo. Situação e feedback aceitam até 1000 caracteres; apelidos, de 3 a 50. DATA_ESCOLHA é data/hora local sem fuso, gerada no Oracle, sem conversão para UTC.

CRUD de mitos/fatos em /mitos-fatos; consulta por cenário em /cenarios/{id}/mitos-fatos. GET /cenarios/{id}/jogo exige A, B e ao menos um item de descoberta. O CRUD comum permite montar o conteúdo gradualmente. Estrutura e requisitos do schema em [README.md#banco-oracle](README.md#banco-oracle).

## Testes e resultados

Requisitos: JDK 25 e Maven compatível. Conferir com `java -version` e `mvn -version`.

```text
mvn test
mvn verify
```

Esses comandos executam os testes locais sem abrir conexão com Oracle e empacotam o projeto quando aplicável. O perfil de integração é explícito:

```text
mvn verify -Poracle-it
```

A integração exige schema Oracle de testes separado e vazio. Na execução registrada, `mvn -B -ntp clean verify -Poracle-it` passou com 145 testes locais e 144 testes Oracle, sem falhas, erros ou ignorados. A API empacotada também passou por 46 verificações HTTP reais, incluindo persistência confirmada por outra conexão JDBC. Os testes removem somente os IDs que criam; as quatro tabelas ficaram vazias ao final. Evidências e limites em [README.md#testes-e-resultados](README.md#testes-e-resultados).

Na atualização dos dados educativos, `mvn verify` foi executado novamente: **146 testes locais aprovados**, sem falhas. As duas coleções Postman também passaram no Newman contra Oracle real, conforme [README.md#dados-para-teste](README.md#dados-para-teste). Os 144 testes de integração Oracle citados acima pertencem à execução anterior.

## Executar a API

Configure as variáveis abaixo no ambiente do processo:

| Variável | Conteúdo |
| --- | --- |
| `ORACLE_JDBC_URL` | URL JDBC da conexão autorizada |
| `APP_USER` | Usuário do schema |
| `APP_USER_PASSWORD` | Senha desse usuário |
| `NEXO_ADMIN_TOKEN` | Token aleatório de 64 caracteres hexadecimais |

O arquivo [.env.example](.env.example) contém somente exemplos. Spring Boot não carrega o `.env` automaticamente. No IntelliJ, selecione o arquivo local em **Run → Edit Configurations → NexoApplication → Environment variables** e configure a senha localmente. Mantenha **Store as project file** desmarcado. Fora da IDE, forneça as variáveis ao terminal/processo. O `.env` e a pasta `.idea/` são ignorados pelo Git; não publique credenciais.

Depois de preparar o banco e as variáveis, execute `NexoApplication` pelo IntelliJ ou use:

```text
mvn spring-boot:run
```

O endereço padrão é 127.0.0.1:8080. SERVER_ADDRESS e SERVER_PORT permitem configuração explícita.

- [Swagger UI](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI JSON](http://localhost:8080/v3/api-docs)

Uma consulta pública, sem token:

```text
curl "http://localhost:8080/cenarios/busca?texto=energia&page=0&size=10"
```

Para executar o pacote gerado por `mvn package`, no ambiente configurado:

```text
java -jar target/nexo-0.0.1-SNAPSHOT.jar
```

Configuração Windows e testes Oracle: [README.md#executar-a-api](README.md#executar-a-api).

## Administração protegida

Catálogo e POST /decisoes são públicos. Alterações de conteúdo e consulta, alteração ou exclusão de decisões exigem o cabeçalho `Authorization: Bearer <token>`, correspondente a `NEXO_ADMIN_TOKEN` no ambiente. A aplicação exige um token válido para iniciar. Não há tabela de usuários, login ou JWT.

Geração local do token, matriz de acesso, Swagger, Postman, rotação e limites: [README.md#administração-protegida](README.md#administração-protegida). Nunca enviar o token por HTTP público nem embuti-lo em aplicativos distribuídos. HTTPS/túnel seguro e proteção contra abuso devem ser definidos antes da exposição remota.

## Documentação e dados para teste

- [Produto](README.md)
- [Contrato HTTP](README.md#java-advanced)
- [Contrato Oracle](README.md#banco-oracle)
- [Arquitetura MVC](README.md#java-advanced)
- [Requisitos](README.md#java-advanced)
- [Checklist Java Sprint 1 V2](README.md#java-advanced)
- [Qualidade](README.md#testes-e-resultados)
- [Segurança e simplicidade](README.md#administração-protegida)
- [Autenticação administrativa](README.md#administração-protegida)
- [Histórico de evidências](README.md#testes-e-resultados)
- [Fontes institucionais](README.md#dados-para-teste)

A coleção [requests/NEXO.postman_collection.json](requests/NEXO.postman_collection.json) acompanha o CRUD, captura IDs e exclui os registros de exemplo ao terminar. Para manter os dados de teste, usar [requests/NEXO.dados-teste.postman_collection.json](requests/NEXO.dados-teste.postman_collection.json): cria um cenário, alternativas A/B, um Mito ou Fato e uma decisão fictícia, sem limpeza automática. O exemplo de Saúde contém um cenário educativo específico, alternativas fundamentadas em fontes AIEA/FDA e um Fato sobre esterilização por radiação. As duas coleções foram executadas via Newman com Oracle real: carga de demonstração com 8 requisições e 18 asserções; CRUD com 29 requisições e 45 asserções, sem falhas. Os dados de demonstração foram mantidos. Esse exemplo não substitui a carga DML de Database nem um catálogo de todas as categorias. Consulte [README.md#dados-para-teste](README.md#dados-para-teste).

## Docker e Oracle externo

A API pode ser compilada e executada em container com Java 25, mantendo o Oracle externo do grupo. O Dockerfile usa múltiplos estágios e usuário sem privilégios administrativos; o Compose publica a API somente em `127.0.0.1:8081`, sem interferir na porta padrão do IntelliJ.

Preparação do `.env.docker`, build, execução, verificação e limites da validação: [Docker](docs/docker.md). O arquivo [docker.env.example](docker.env.example) não contém credenciais; o arquivo local `.env.docker` é ignorado pelo Git e excluído do contexto de build.

Docker padroniza o empacotamento e a execução da API. Cloud Computing permitirá hospedar essa imagem em uma VM Azure, acessando o mesmo Oracle autorizado. Nenhum recurso de nuvem foi provisionado nesta etapa; a entrega DevOps ainda exige execução na VM criada por Azure CLI e seus materiais de comprovação.

## Azure: planejamento antes de gastar créditos

Os scripts `.sh` de criação da VM, instalação do Docker e exclusão do grupo estão preparados. **Por padrão, eles apenas planejam: não criam recursos, não excluem recursos e não instalam pacotes.** A execução real exige `--executar`; o provisionamento e a exclusão exigem também confirmações explícitas diferentes.

- [Guia Azure, acesso à VM e comprovação da exclusão](docs/azure.md)
- [Parâmetros de exemplo, sem segredos](azure.env.example)

A preparação foi validada localmente, com comandos Azure simulados e consultas reais somente de leitura. **Nenhum recurso Azure foi provisionado.** O deploy na VM, o acesso Azure → Oracle e a exclusão real ainda precisam ser demonstrados.

Contas individuais, login, ranking, painel e interfaces de outras disciplinas não integram a API atual; a administração é protegida pelo token compartilhado autorizado.

Situações, apelidos e decisões de demonstração são fictícios. Conteúdo de saúde é educativo.