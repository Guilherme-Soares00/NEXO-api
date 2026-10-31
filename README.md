# NEXO | Laboratório de decisões

API educativa para explorar aplicações civis da tecnologia nuclear em saúde, agricultura, indústria, pesquisa e energia. O estudante consulta um cenário, compara alternativas, registra uma decisão e acessa explicações e itens de mito ou fato.

Projeto acadêmico do Challenge FIAP ADS 2026, com entregas de Java Advanced e DevOps & Cloud. O contexto do desafio envolve a AMAZUL e a Marinha; o NEXO não é um produto oficial dessas instituições.

## Acesso rápido por disciplina

| Disciplina | Conteúdo | Por onde começar |
| --- | --- | --- |
| **Java Advanced** | API REST, JPA, Oracle, validação, busca, Swagger e testes | **[Seção Java](#java-advanced)** · [Executar a API](#executar-a-api) · [Testes](#testes-e-resultados) |
| **DevOps & Cloud** | Docker, VM Azure, rede, custos, arquitetura e exclusão dos recursos | **[Seção DevOps](#devops--cloud)** · **[Guia Docker](docs/devops/docker.md)** · [Guia Azure](docs/devops/azure.md) |

> **Configuração do ambiente**
>
> Antes de executar em outra máquina, consulte **[os pré-requisitos e os arquivos de configuração](#configuração-do-ambiente)**. A execução Java não exige Docker ou Azure; a execução em Docker não exige uma assinatura Azure.

## Grupo

Turma: 2TDSPB

| Integrante | RM |
| --- | --- |
| Luis Fillipe Rodrigues Seripieri | 567918 |
| Gabriel Rocha de Souza | 567726 |
| Luiz Kichimoto Valdevino | 567023 |
| Guilherme Soares | 568227 |
| Gabriel Stuani | 566682 |

Repositório: [Guilherme-Soares00/NEXO-api](https://github.com/Guilherme-Soares00/NEXO-api)


## Configuração do ambiente

O projeto não exige a assinatura Azure, o IP ou a chave SSH do desenvolvedor. Cada pessoa configura seu próprio ambiente a partir dos arquivos de exemplo. **Clonar o repositório não configura automaticamente o banco nem cria uma VM.** Executar os comandos na raiz da cópia do projeto, sem usar caminhos particulares de outro computador.

| Forma de execução | Disciplina | Pré-requisitos | Configuração e instruções |
| --- | --- | --- | --- |
| Testes locais | Java Advanced | JDK 25 e Maven compatível | `mvn verify` não exige Azure, Docker nem conexão Oracle; ver [Testes e resultados](#testes-e-resultados) |
| API pelo IntelliJ/Maven | Java Advanced | JDK 25, Maven e acesso a um Oracle preparado | Variáveis de `.env.example` no ambiente do processo; ver [Executar a API](#executar-a-api) |
| API em Docker local | DevOps & Cloud | Docker Engine com containers Linux, Docker Compose e acesso ao Oracle | `.env.docker`, a partir de `docker.env.example`; seguir **[o Guia Docker](docs/devops/docker.md)** |
| Infraestrutura Azure | DevOps & Cloud | Git, Bash, Azure CLI, OpenSSH e assinatura autorizada com permissão, quota e orçamento | `.env.azure` no computador e `.env.docker` na futura VM; ver [Configuração Azure](#azure-o-que-alterar-no-próprio-ambiente) e [Guia Azure](docs/devops/azure.md) |

Java e Maven no computador não são necessários para o build Docker, pois o Dockerfile inclui o estágio de compilação. No Windows, iniciar o Docker Desktop com containers Linux para a execução local e usar Git Bash para os scripts `.sh`.

Para conferir as ferramentas, conforme a opção escolhida: `java -version`, `mvn -version`, `git --version`, `bash --version`, `az version`, `ssh -V`, `docker version` e `docker compose version`.

**Arquivos particulares não acompanham o repositório:** `.env`, `.env.azure`, `.env.docker` e chaves privadas. Não preencher os exemplos públicos com senhas, tokens ou dados de uma conta real. Configurar o próprio ambiente não exige receber arquivos privados do desenvolvedor.

---

## Java Advanced

Esta seção apresenta a implementação e a execução da API Java: modelo, persistência, endpoints, validações, segurança e testes. **Docker, assinatura Azure e chave SSH não são requisitos para executar a API pelo IntelliJ/Maven.** O acesso a um Oracle preparado é necessário para iniciar a aplicação, mas não para executar os testes locais.

### Tecnologias e funcionamento

Java 25, Spring Boot 3.5.16, Spring MVC, Spring Data JPA, Oracle JDBC e springdoc 2.9.1. Controllers recebem DTOs validados; serviços controlam transações; repositories Spring Data JPA fazem leituras e escritas nas quatro entidades.

A API oferece CRUD de cenários, alternativas, decisões e mitos/fatos, busca parcial pelo título e paginação. Também permite consultar e cadastrar alternativas de um cenário, valida os rótulos A/B e devolve erros JSON padronizados. As entidades JPA não são expostas diretamente nas respostas.

Os recursos estão organizados em pacotes de domínio, com DTOs, controllers, serviços e repositories. As configurações compartilhadas ficam em `configuration/`; validações e tratamento de erros comuns, em `shared/`.

Enunciado vigente: Challenge de tecnologia nuclear, Sprint 1, páginas 33–36 para Java. Na página 38, Database adia procedures, functions com cursor e sua integração para a Sprint 2. O CRUD Java atual não depende dessas rotinas.

Oracle é o perfil padrão. A aplicação não cria tabelas, sequences ou dados. Para instalação manual em schema próprio sem objetos NEXO, usar [docs/java/sql/schema-oracle.sql](docs/java/sql/schema-oracle.sql). O schema deve atender a [README.md#banco-oracle](README.md#banco-oracle). IDs usam as sequences existentes com allocationSize=1; DATA_ESCOLHA continua sendo gerada no banco e preservada no PUT.

### Alinhamento com o Oracle 19c

Tabelas NEXO_CENARIO, NEXO_ALTERNATIVA, NEXO_DECISAO e NEXO_MITO_FATO. Mito ou Fato pertence ao cenário e é compartilhado por A/B. Não há campo de ordenação no modelo. Situação e feedback aceitam até 1000 caracteres; apelidos, de 3 a 50. DATA_ESCOLHA é data/hora local sem fuso, gerada no Oracle, sem conversão para UTC.

CRUD de mitos/fatos em /mitos-fatos; consulta por cenário em /cenarios/{id}/mitos-fatos. GET /cenarios/{id}/jogo exige A, B e ao menos um item de descoberta. O CRUD comum permite montar o conteúdo gradualmente. Estrutura e requisitos do schema em [README.md#banco-oracle](README.md#banco-oracle).

### Testes e resultados

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

Na conferência final, `mvn -B -ntp clean verify` aprovou **148 testes locais**, após a correção do cabeçalho `Allow` nas respostas 405. A imagem Docker foi reconstruída e passou por 16 verificações HTTP de leitura contra o Oracle externo. Não houve nova execução de `oracle-it` nem escrita no schema compartilhado; detalhes em [README.md#testes-e-resultados](README.md#testes-e-resultados).

### Executar a API

Na conferência complementar, o build limpo da versão local atual aprovou **148 testes**, sem falhas. O CRUD das quatro entidades e as duas coleções Postman também passaram no Oracle do grupo, com os dados existentes integralmente preservados após a limpeza dos registros de teste. As sequences puderam avançar, conforme autorização; não foram reiniciadas. Resultados e distinção entre versão local e publicada em [README.md#testes-e-resultados](README.md#testes-e-resultados).

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

### Administração protegida

Catálogo e POST /decisoes são públicos. Alterações de conteúdo e consulta, alteração ou exclusão de decisões exigem o cabeçalho `Authorization: Bearer <token>`, correspondente a `NEXO_ADMIN_TOKEN` no ambiente. A aplicação exige um token válido para iniciar. Não há tabela de usuários, login ou JWT.

Geração local do token, matriz de acesso, Swagger, Postman, rotação e limites: [README.md#administração-protegida](README.md#administração-protegida). Nunca enviar o token por HTTP público nem embuti-lo em aplicativos distribuídos. HTTPS/túnel seguro e proteção contra abuso devem ser definidos antes da exposição remota.

### Documentação e dados para teste

- [Produto](README.md)
- [Contrato HTTP](README.md#java-advanced)
- [Contrato Oracle](README.md#banco-oracle)
- [Arquitetura MVC](README.md#java-advanced)
- [Requisitos](README.md#java-advanced)
- [Checklist Java Sprint 1](README.md#java-advanced)
- [Qualidade](README.md#testes-e-resultados)
- [Segurança e simplicidade](README.md#administração-protegida)
- [Autenticação administrativa](README.md#administração-protegida)
- [Histórico de evidências](README.md#testes-e-resultados)
- [Fontes institucionais](README.md#dados-para-teste)

A coleção [requests/NEXO.postman_collection.json](requests/NEXO.postman_collection.json) acompanha o CRUD, captura IDs e exclui os registros de exemplo ao terminar. Para manter os dados de teste, usar [requests/NEXO.dados-teste.postman_collection.json](requests/NEXO.dados-teste.postman_collection.json): cria um cenário, alternativas A/B, um Mito ou Fato e uma decisão fictícia, sem limpeza automática. O exemplo de Saúde contém um cenário educativo específico, alternativas fundamentadas em fontes AIEA/FDA e um Fato sobre esterilização por radiação. As duas coleções foram executadas via Newman com Oracle real: carga de demonstração com 8 requisições e 18 asserções; CRUD com 29 requisições e 45 asserções, sem falhas. Os dados de demonstração foram mantidos. Esse exemplo não substitui a carga DML de Database nem um catálogo de todas as categorias. Consulte [README.md#dados-para-teste](README.md#dados-para-teste).

---

## DevOps & Cloud

Esta seção trata do empacotamento e da infraestrutura da mesma API Java. **Docker local e implantação na Azure são etapas diferentes:** o teste local permite conferir o container; a demonstração de nuvem exige a aplicação executando na VM criada por Azure CLI.

**[Assistir à demonstração no YouTube](https://www.youtube.com/watch?v=KHZwnoqLOJQ)** · **[PDF da entrega de DevOps](docs/devops/entrega-devops.pdf)**

A demonstração foi concluída: VM criada por Azure CLI, Docker instalado no Ubuntu, imagem construída na VM, container saudável e API acessível pelo IP público. Ao terminar, o grupo de recursos foi excluído, com confirmação de ausência do grupo e lista vazia de recursos restantes. O ambiente Azure não permanece ativo; o vídeo registra a execução e a limpeza.

### Docker: execução local e configuração

> **[Guia Docker — preparação, build e execução](docs/devops/docker.md)**
>
> Siga o guia para criar `.env.docker`, gerar o token, construir a imagem e verificar o container. Não é necessário configurar Azure, IP administrativo ou chave SSH para essa etapa local.

1. [Preparar o ambiente e as variáveis](docs/devops/docker.md#1-preparar-o-ambiente).
2. [Testar e construir a imagem](docs/devops/docker.md#2-testar-e-construir).
3. [Executar o container em background](docs/devops/docker.md#3-executar-em-background).
4. [Conferir a API e o healthcheck](docs/devops/docker.md#4-conferir-somente-leitura).

A API pode ser compilada e executada em container com Java 25, mantendo o Oracle externo do grupo. O Dockerfile usa múltiplos estágios e usuário sem privilégios administrativos; o Compose publica a API somente em `127.0.0.1:8081`, sem interferir na porta padrão do IntelliJ.

Docker padroniza o empacotamento da API e reúne a compilação e a execução em uma imagem reproduzível. Cloud Computing fornece a VM e a rede para executar essa imagem fora do computador de desenvolvimento. No NEXO, a VM Azure hospedou Docker e a API Java, enquanto o Oracle autorizado permaneceu externo na FIAP. Isso permitiu demonstrar a solução e remover a infraestrutura de nuvem sem incluir o banco nessa exclusão.

#### Oracle e variáveis do container

O Oracle é externo: **o Compose não instala banco nem cria o schema**. É necessário acesso autorizado ao schema do grupo ou a um Oracle próprio preparado conforme o [contrato do banco](README.md#banco-oracle). O script de estrutura indicado nesse contrato só deve ser executado em um schema próprio apropriado, nunca automaticamente no banco compartilhado. Não executar `-Poracle-it` nesse schema compartilhado: a suíte exige um schema de testes separado e vazio.

Para Docker, copiar `docker.env.example` para `.env.docker` e preencher:

| Variável | Configuração necessária |
| --- | --- |
| `ORACLE_JDBC_URL` | Informar o endereço JDBC do Oracle autorizado; o exemplo FIAP só serve se houver acesso a esse servidor. `localhost` dentro do container não aponta para o computador hospedeiro |
| `APP_USER` | Informar o usuário do schema preparado |
| `APP_USER_PASSWORD` | Informar a senha desse usuário apenas no arquivo local |
| `NEXO_ADMIN_TOKEN` | Gerar um token próprio de 64 caracteres hexadecimais pelo [guia Docker](docs/devops/docker.md#1-preparar-o-ambiente); o cliente administrativo deve usar o token dessa instância |
| `API_PORT` | Manter `8081` para Docker local ou escolher outra porta livre; essa variável não controla a publicação na Azure |

O arquivo [docker.env.example](docker.env.example) não contém credenciais; `.env.docker` é ignorado pelo Git e excluído do contexto de build. Os exemplos não importam a configuração do IntelliJ. Na VM, usar o mesmo nome `.env.docker`, mas preencher o arquivo naquele ambiente; não copiar indiscriminadamente arquivos particulares do computador. Os comandos completos estão no **[Guia Docker](docs/devops/docker.md)**.

### Azure: o que alterar no próprio ambiente

A criação na Azure consome o orçamento ou os créditos da **assinatura selecionada e configurada em `.env.azure`**, não de uma conta embutida no projeto. Autenticar e selecionar a própria assinatura autorizada. Estes comandos preparam a sessão e o arquivo local, sem criar recursos:

```bash
az login
az account show --query '{assinatura:name,id:id,estado:state}' --output table
cp -n azure.env.example .env.azure
```

Se houver mais de uma assinatura disponível, listar com `az account list --query '[].{nome:name,id:id}' --output table` e selecionar com `az account set --subscription 'ID_DA_ASSINATURA_ESCOLHIDA'`, substituindo o identificador pelo ID real desejado. Conferir novamente `az account show` e usar esse mesmo ID no arquivo.

Editar o `.env.azure` criado. O `cp -n` não sobrescreve uma configuração já existente.

| Variável | Precisa alterar? | Como preencher |
| --- | --- | --- |
| `AZURE_SUBSCRIPTION_ID` | Sim | Copiar o ID da própria assinatura selecionada, conferido pelo comando acima |
| `ADMIN_CIDR` | Sim | Informar o IPv4 público da conexão administrativa seguido de `/32`; não é o IP da futura VM |
| `SSH_PUBLIC_KEY` | Preparar o arquivo | Gerar ou reutilizar uma chave própria. O padrão `$HOME/.ssh/nexo-azure.pub` pode ser mantido se o arquivo existir nesse caminho |
| `AZURE_LOCATION` | Conferir | `chilecentral` foi a região da demonstração, não uma garantia para outras contas. Usar região aceita pelo script e permitida na própria assinatura |
| `AZURE_VM_SIZE` | Conferir | `Standard_B2als_v2` foi o modelo configurado, com 2 vCPUs e 4 GiB. Uma nova execução depende de disponibilidade, quota e preço na região escolhida. Não existe substituição automática por modelo mais caro |
| `AZURE_IMAGE` | Conferir | Manter a imagem Ubuntu 24.04 LTS x86-64 indicada no exemplo se estiver disponível. O instalador Docker exige essa distribuição e arquitetura; não trocar por outro sistema sem ajustar a instalação |
| `AZURE_RESOURCE_GROUP` | Conferir | Manter `rg-nexo-devops` somente se o grupo não existir na assinatura. Para outro grupo dedicado, usar um sufixo, como `rg-nexo-devops-demo`; o script exige esse prefixo e não reutiliza grupos existentes |
| `AZURE_VM_NAME` | Pode manter | `vm-nexo`, dentro do novo grupo dedicado |
| `AZURE_ADMIN_USER` | Pode manter | `nexoadmin`, usuário Linux que será criado na VM; não é o login da conta Azure |
| `AZURE_API_PORT` | Pode manter | `8080`. Se alterar, configurar o mesmo valor na `.env.docker` da VM e ajustar o acesso e o túnel conforme o guia Azure |

Para consultar o IPv4 público da conexão atual:

```bash
curl -4 --fail --silent --show-error https://api.ipify.org
printf '\n'
```

Acrescentar `/32` ao resultado ao preencher `ADMIN_CIDR`. Uma mudança de rede ou de IP pode exigir atualizar as regras de acesso. O IP da VM é outro endereço, criado pelo provisionamento e consultado depois pelo guia Azure.

Para gerar uma chave própria, somente se nenhum dos arquivos de destino existir:

```bash
mkdir -p "$HOME/.ssh"
if [[ ! -e "$HOME/.ssh/nexo-azure" && ! -e "$HOME/.ssh/nexo-azure.pub" ]]; then
    ssh-keygen -t ed25519 -f "$HOME/.ssh/nexo-azure" -C nexo-azure
fi
```

Se o par já existir, reutilizar e conferir o caminho configurado, sem sobrescrever. Se houver apenas um dos arquivos, regularizar o par antes de continuar. A chave pública termina em `.pub`; a privada permanece no computador de quem acessará a VM. Se optar por uma passphrase, digitá-la diretamente no terminal. Nunca compartilhar a chave privada ou a passphrase.

**Limitação atual de regiões:** o provisionador aceita `chilecentral`, `mexicocentral`, `eastus`, `canadacentral` e `centralus`, conforme a política observada na assinatura usada na preparação. Essa lista não descreve todas as regiões da Azure nem as permissões de outra conta. Se a assinatura utilizada só permitir uma região fora dessa lista, alterar apenas `.env.azure` não será suficiente: a validação de região do provisionador precisará ser adaptada. Antes disso, conferir a política da assinatura, a compatibilidade e os custos; não remover as proteções de assinatura, acesso ou exclusão.

Depois de preencher o arquivo, carregar os valores na sessão Git Bash e conferir o planejamento:

```bash
set -a
. ./.env.azure
set +a
bash infra/scripts/provisionar-azure.sh --planejar
```

O planejamento não chama a Azure nem comprova disponibilidade; apenas mostra os comandos com os parâmetros escolhidos. A criação efetiva e a instalação na VM estão separadas no [guia Azure](docs/devops/azure.md#provisionamento-com-autorização) e exigem autorização para o gasto. A execução demonstrada não garante quota, políticas, capacidade de VM/imagem ou acesso ao Oracle em outra assinatura ou em uma nova sessão.

Após concluir a demonstração, usar [o procedimento de exclusão do grupo](docs/devops/azure.md#exclusão-e-comprovação). Ele remove os recursos desse grupo e verifica sua ausência; desligar ou excluir somente a VM não substitui essa limpeza. O disco e a NIC são configurados para exclusão junto à VM, mas o IP público depende da exclusão do grupo inteiro. Não colocar recursos alheios ao projeto no grupo dedicado e conferir resíduos também quando uma criação falhar.

### Azure: provisionamento e exclusão

Os scripts `.sh` de criação da VM, instalação do Docker e exclusão do grupo estão preparados. **Por padrão, eles apenas planejam: não criam recursos, não excluem recursos e não instalam pacotes.** A execução real exige `--executar`; o provisionamento e a exclusão exigem também confirmações explícitas diferentes.

- [Guia Azure, acesso à VM e comprovação da exclusão](docs/devops/azure.md)
- [Parâmetros de exemplo, sem segredos](azure.env.example)

A preparação foi verificada com testes locais de controle dos scripts e validação do Compose. A execução real na Azure e a exclusão do grupo estão registradas no [vídeo da demonstração](https://www.youtube.com/watch?v=KHZwnoqLOJQ). Os testes simulados verificam o controle dos scripts e não substituem essa evidência real.

### Arquitetura e demonstração

![Arquitetura demonstrada do NEXO no Azure, com API em Docker e Oracle FIAP externo; recursos Azure excluídos após a demonstração.](docs/devops/diagrama-arquitetura.png)

O [guia de arquitetura](docs/devops/arquitetura-devops.md) explica os componentes e apresenta o diagrama em PNG.

---

## Escopo e observações

Contas individuais, login, ranking, painel e interfaces de outras disciplinas não integram a API atual; a administração é protegida pelo token compartilhado autorizado.

Situações, apelidos e decisões de demonstração são fictícios. Conteúdo de saúde é educativo.
