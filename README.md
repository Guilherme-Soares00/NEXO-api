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

API REST em Java 25, Spring Boot 3.5.16, Spring MVC, Spring Data JPA e Oracle. Oferece CRUD de cenários, alternativas, decisões e mitos/fatos, busca parcial por título e paginação. Controllers recebem DTOs validados; serviços aplicam regras e transações; repositories fazem a persistência. Erros são retornados em JSON.

- **Modelo:** [modelagem Java e diagrama de classes](docs/java/README.md).
- **Documentação dos endpoints:** [Swagger UI](http://localhost:8080/swagger-ui/index.html) e [OpenAPI JSON](http://localhost:8080/v3/api-docs), disponíveis com a API iniciada.
- **Recursos:** `/cenarios`, `/alternativas`, `/decisoes` e `/mitos-fatos`. A busca usa `/cenarios/busca?texto=energia&page=0&size=10`; o jogo, `/cenarios/{id}/jogo`.

### Banco Oracle

A aplicação usa as tabelas `NEXO_CENARIO`, `NEXO_ALTERNATIVA`, `NEXO_DECISAO` e `NEXO_MITO_FATO`, com as quatro sequences `SEQ_NEXO_*`. Não cria objetos ou dados automaticamente e não depende de procedures/functions para o CRUD.

Para instalar em **schema próprio vazio**, usar [o script de estrutura Oracle](docs/java/sql/schema-oracle.sql). Não executar esse script sobre o schema compartilhado do grupo. IDs são gerados por sequence com `allocationSize=1`; a data da decisão vem de `DEFAULT LOCALTIMESTAMP` e é preservada nas atualizações. Exclusões respeitam as dependências entre os registros.

### Executar a API

Requisitos: JDK 25, Maven e acesso autorizado a um Oracle preparado. Configure as variáveis no ambiente do processo:

| Variável | Conteúdo |
| --- | --- |
| `ORACLE_JDBC_URL` | URL JDBC do Oracle autorizado |
| `APP_USER` | Usuário do schema |
| `APP_USER_PASSWORD` | Senha desse usuário |
| `NEXO_ADMIN_TOKEN` | Token aleatório de 64 caracteres hexadecimais |

O arquivo [.env.example](.env.example) contém apenas exemplos. Spring Boot **não carrega `.env` automaticamente**. No IntelliJ, configure o ambiente de `NexoApplication` em **Run → Edit Configurations → Environment variables**, sem compartilhar a configuração ou publicar credenciais. Pelo terminal, forneça as variáveis ao processo e execute:

```text
mvn spring-boot:run
```

A API inicia em `127.0.0.1:8080`; `SERVER_ADDRESS` e `SERVER_PORT` permitem alterar o endereço e a porta. Uma consulta pública:

```text
curl "http://localhost:8080/cenarios/busca?texto=energia&page=0&size=10"
```

### Administração protegida

O catálogo e `POST /decisoes` são públicos. Escritas de conteúdo e leitura, alteração ou exclusão de decisões exigem `Authorization: Bearer <token>`, com o mesmo valor de `NEXO_ADMIN_TOKEN`. Não há contas individuais, login ou JWT. A aplicação não inicia sem um token válido.

Para gerar o token no ambiente de uma sessão **PowerShell**, sem imprimi-lo ou gravá-lo em arquivo:

```powershell
$bytes = New-Object byte[] 32
$random = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $random.GetBytes($bytes)
    $env:NEXO_ADMIN_TOKEN = -join ($bytes | ForEach-Object { $_.ToString('x2') })
} finally {
    $random.Dispose()
    [Array]::Clear($bytes, 0, $bytes.Length)
}
```

Inicie a aplicação nessa mesma sessão após configurar as variáveis Oracle. No Swagger, use **Authorize → adminToken** e informe somente o token, sem o prefixo `Bearer`. Não publique o valor nem o envie pela internet em HTTP simples; use HTTPS ou túnel seguro para administração remota.

### Dados para teste

Importe uma destas coleções no Postman:

- [CRUD completo](requests/NEXO.postman_collection.json): percorre os quatro recursos e exclui os registros que criou ao terminar.
- [Dados de demonstração](requests/NEXO.dados-teste.postman_collection.json): cria cenário de Saúde, alternativas A/B, um Fato e uma decisão fictícia; **mantém os registros no banco**.

Defina `baseUrl` para a API autorizada e `adminToken` como variável secreta em ambiente local do Postman. Execute uma iteração na ordem, interrompendo em caso de falha. Os IDs são capturados das respostas; não precisam ser fixados manualmente. Repetir a carga de demonstração cria novos registros, portanto só a execute quando houver autorização para alterar o banco.

O exemplo aborda esterilização de produtos médicos e usa fontes da [AIEA](https://www.iaea.org/topics/medical-sterilization) e da [FDA](https://www.fda.gov/medical-devices/general-hospital-devices-and-supplies/sterilization-medical-devices). O conteúdo é educativo, com participantes e decisões fictícios.

### Testes e resultados

```text
mvn verify
```

Executa os testes locais sem exigir Oracle, Docker ou Azure e gera o pacote em `target/`. A última verificação do código aprovou **148 testes locais**, sem falhas.

A integração opcional usa `mvn verify -Poracle-it` e exige **schema de testes separado e inicialmente vazio**, configurado por `ORACLE_TEST_JDBC_URL`, `ORACLE_TEST_USER` e `ORACLE_TEST_PASSWORD`. Não executar esse perfil no banco compartilhado nem sobre os dados de apresentação.

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

O Oracle é externo: **o Compose não instala banco nem cria o schema**. É necessário acesso autorizado ao schema do grupo ou a um Oracle próprio preparado conforme o [script de estrutura Oracle](docs/java/sql/schema-oracle.sql). Esse script de estrutura só deve ser executado em um schema próprio apropriado, nunca automaticamente no banco compartilhado. Não executar `-Poracle-it` nesse schema compartilhado: a suíte exige um schema de testes separado e vazio.

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
