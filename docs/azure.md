# Azure — scripts preparados, provisionamento ainda não executado

> Os scripts e este guia foram preparados sem criar recursos Azure. A execução real continua dependendo de autorização separada, conferência de custos e disponibilidade. Não execute o bloco de criação apenas para testar os arquivos.

## Navegação

- [O que será criado](#o-que-será-criado)
- [Preparação local sem provisionar](#preparação-local-sem-provisionar)
- [Planejamento seguro](#planejamento-seguro)
- [Execução futura com autorização](#execução-futura-com-autorização)
- [Docker e API na VM](#docker-e-api-na-vm)
- [Acesso e demonstração](#acesso-e-demonstração)
- [Exclusão e comprovação](#exclusão-e-comprovação)
- [O que foi verificado](#o-que-foi-verificado)

## Arquivos

| Arquivo | Finalidade |
| --- | --- |
| `azure.env.example` | Parâmetros não secretos do provisionamento |
| `infra/scripts/provisionar-azure.sh` | Grupo, rede, NSG, IP público, NIC e VM por Azure CLI |
| `infra/scripts/instalar-docker.sh` | Docker Engine, Buildx, Compose e Git na futura VM Ubuntu |
| `infra/scripts/excluir-recursos.sh` | Exclusão do grupo dedicado com conferência posterior |
| `infra/docker-compose.azure.yml` | Trocar somente a publicação de porta ao executar na VM |
| `.gitattributes` | Preservar LF nos scripts Bash e no exemplo de ambiente Azure |

## O que será criado

```text
Computador administrativo
  → IP público IPv4 Standard
  → NSG: TCP 22 e TCP 8080, somente a partir do IPv4 administrativo /32
  → NIC na subnet-nexo / vnet-nexo
  → VM Ubuntu 24.04 LTS x86-64
  → Docker / rede nexo-network / container da API
  → Oracle FIAP externo, usando o acesso autorizado ao schema do grupo
```

Esse desenho textual explica o fluxo; não substitui o diagrama final com ícones Azure exigido na entrega.

Padrões: grupo `rg-nexo-devops`, VM `vm-nexo`, região `eastus`, candidata `Standard_B2ls_v2`, disco Standard SSD LRS de 32 GiB e usuário SSH `nexoadmin`. A VM não possui autenticação por senha. Não há Oracle em container, Azure SQL, NAT Gateway ou outros serviços pagos adicionais previstos nos scripts.

Somente SSH e a porta HTTP da API recebem regras personalizadas de entrada. Ambas são restritas ao IPv4 administrativo com máscara `/32`; o script rejeita `0.0.0.0/0`. As regras padrão do NSG continuam existindo. O listener Oracle não é exposto nessa VM.

A candidata tem 2 vCPUs e 4 GiB, pois hospeda API e Docker, não o Oracle. Quota e imagem foram consultadas; disponibilidade do SKU e alocação continuam pendentes.

## Preparação local sem provisionar

Executar scripts `.sh` no **Git Bash** do Windows, na raiz do repositório. PowerShell pode ser usado para `az login`, mas não interpreta os scripts Bash diretamente. Reabrir o terminal após instalar a Azure CLI para receber o PATH atualizado.

```bash
az version
az account show --query '{assinatura:name,estado:state}' --output table
cp -n azure.env.example .env.azure
```

Editar `.env.azure` localmente. Esse arquivo é ignorado pelo Git; não colocar senha Oracle, token administrativo ou chave privada nele.

| Variável | Preenchimento |
| --- | --- |
| `AZURE_SUBSCRIPTION_ID` | ID exato da assinatura da faculdade |
| `AZURE_RESOURCE_GROUP` | Grupo dedicado; manter `rg-nexo-devops` ou um sufixo alfanumérico |
| `AZURE_LOCATION` | `eastus`, dentro da política observada |
| `AZURE_VM_SIZE` | `Standard_B2ls_v2`, pendente de disponibilidade |
| `ADMIN_CIDR` | IPv4 público da rede administrativa seguido de `/32` |
| `SSH_PUBLIC_KEY` | Caminho do arquivo de chave pública, terminado em `.pub` |
| `AZURE_API_PORT` | `8080`, igual à porta publicada futuramente na VM |

Carregar somente o arquivo local criado a partir do exemplo confiável:

```bash
set -a
. ./.env.azure
set +a
```

Para obter o ID sem consultar credenciais:

```bash
az account show --query id --output tsv
```

O exemplo mantém o ID vazio. O script nunca escolhe outra assinatura, região ou VM mais cara automaticamente. Todos os comandos de recursos usam `--subscription` explícito.

Para a chave SSH, reutilizar uma chave própria adequada ou gerar uma nova localmente, somente se o destino ainda não existir:

```bash
ssh-keygen -t ed25519 -f "$HOME/.ssh/nexo-azure" -C nexo-azure
```

Escolher a passphrase diretamente no terminal e não compartilhar a chave privada. A geração é uma preparação local, não provisionamento Azure. Este módulo não gerou nem substituiu chaves.

## Planejamento seguro

Estes comandos não fazem chamadas Azure nem instalam pacotes:

```bash
bash infra/scripts/provisionar-azure.sh
bash infra/scripts/instalar-docker.sh
bash infra/scripts/excluir-recursos.sh
```

Todos assumem `--planejar` quando nenhum argumento é informado. O provisionamento exibe os comandos futuros. O instalador descreve o que faria. A exclusão informa o alvo, sem consultá-lo nem apagá-lo.

Não existe login automático, execução no carregamento do arquivo ou criação automática de chave SSH. O fato de um plano exibir `az ... create` não significa que o comando foi executado.

## Execução futura com autorização

**Somente quando os custos e a sessão de execução forem aprovados:**

```bash
CONFIRMAR_CRIACAO=SIM bash infra/scripts/provisionar-azure.sh --executar
```

As duas condições são necessárias. O script recusa configurações inválidas, grupo existente, assinatura selecionada diferente, origem de rede irrestrita, chave ausente e SKU sem disponibilidade informada pela API. Antes da primeira criação, revalida também a imagem.

A consulta de SKUs pode demorar. Se houver erro ou timeout, interromper e investigar sem retirar os bloqueios ou trocar a VM silenciosamente. A confirmação de quota não garante capacidade de alocação.

O script cria um grupo dedicado com tags `projeto=NEXO` e `finalidade=devops-sprint1`, uma VNet/subnet, NSG e duas regras, IP público, NIC e VM. A NIC já existente é fornecida ao comando de criação da VM, evitando uma segunda rede implícita. Não usa `az vm open-port`.

Uma falha depois da criação do grupo pode deixar recursos parciais consumindo créditos. Não há exclusão automática; conferir o estado e autorizar a limpeza pelo script apropriado.

Depois de provisionar, obter o IP e confirmar a VM:

```bash
IP_PUBLICO=$(az network public-ip show --resource-group "$AZURE_RESOURCE_GROUP" --name pip-nexo --subscription "$AZURE_SUBSCRIPTION_ID" --query ipAddress --output tsv)
IP_PUBLICO=${IP_PUBLICO%$'\r'}
az vm show --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_VM_NAME" --show-details --subscription "$AZURE_SUBSCRIPTION_ID" --query '{nome:name,estado:powerState,ip:publicIps,tamanho:hardwareProfile.vmSize}' --output table
```

Não copiar senhas ou tokens para parâmetros de provisionamento, custom-data, script, histórico ou arquivos versionados.

## Docker e API na VM

Estes passos só se aplicam depois de a VM existir. Acessar por SSH, conferindo a impressão digital do servidor pelo ambiente Azure antes de aceitar uma chave desconhecida:

```bash
scp -i "$HOME/.ssh/nexo-azure" infra/scripts/instalar-docker.sh "$AZURE_ADMIN_USER@$IP_PUBLICO:instalar-docker.sh"
ssh -i "$HOME/.ssh/nexo-azure" "$AZURE_ADMIN_USER@$IP_PUBLICO"
```

Na VM:

```bash
sudo bash instalar-docker.sh --executar
git clone https://github.com/Guilherme-Soares00/NEXO-api.git
cd NEXO-api
```

O instalador verifica Ubuntu 24.04 e x86-64, usa o repositório oficial Docker e não remove pacotes conflitantes automaticamente. Não adiciona o usuário SSH ao grupo Docker, que concede privilégios elevados; por isso os comandos usam `sudo docker`.

O clone contém apenas arquivos publicados. Se os arquivos aprovados ainda existirem somente no computador, transferir os arquivos necessários via SCP antes de executar; não presumir que um commit local já esteja disponível no GitHub. Nunca copiar `.idea`, `.env` da IDE ou a chave privada. Não houve publicação automática neste módulo.

Na VM, preparar `.env.docker` com as credenciais autorizadas, seguindo [o guia Docker](docker.md). Gerar o token nesse arquivo e informar usuário e senha diretamente por um editor local da VM; não enviá-los em comandos SSH ou no vídeo. Aplicar permissão restrita:

```bash
umask 077
cp -n docker.env.example .env.docker
chmod 600 .env.docker
nano .env.docker
```

O editor não deve ser mostrado enquanto contém os segredos. O arquivo precisa conter URL Oracle, usuário, senha e token válido. Se alterar `AZURE_API_PORT`, adicionar o mesmo valor ao arquivo da VM; o padrão é `8080`. O `API_PORT=8081` do exemplo local não altera o override Azure.

Verificar a versão do Compose. O override utiliza `!override`, recurso oficial disponível a partir do Compose 2.24.4, para substituir a porta local, sem publicar duas portas. Um validador YAML genérico pode não reconhecer essa tag; o modelo foi validado pelo próprio Docker Compose.

```bash
sudo docker compose version
sudo docker compose --env-file .env.docker -f docker-compose.yml -f infra/docker-compose.azure.yml config --quiet
sudo docker compose --env-file .env.docker -f docker-compose.yml -f infra/docker-compose.azure.yml up -d --build
sudo docker compose --env-file .env.docker -f docker-compose.yml -f infra/docker-compose.azure.yml ps
sudo docker compose --env-file .env.docker -f docker-compose.yml -f infra/docker-compose.azure.yml logs --tail=100 api
```

Não imprimir `docker compose config` sem `--quiet`, nem o ambiente completo de `docker inspect`: isso pode revelar credenciais.

A API escuta na porta 8080 do container. O override publica a porta em `0.0.0.0` somente na VM; a rede Azure limita o acesso à origem administrativa. A execução local padrão permanece em `127.0.0.1:8081`.

É necessário verificar se o Oracle FIAP permite conexões originadas da Azure. Falha de rede, DNS, autenticação ou schema não deve ser corrigida com DDL, troca automática de banco ou execução dos testes de integração no schema compartilhado.

## Acesso e demonstração

No computador administrativo, usar o IP da VM para mostrar a execução remota. Não enviar o token pelo HTTP público:

```bash
curl --fail "http://$IP_PUBLICO:$AZURE_API_PORT/cenarios?page=0&size=1"
```

Mostrar também o terminal SSH, a VM no Azure e `docker compose ps` na VM. O teste no computador não substitui essa prova.

Para operações administrativas e Swagger autorizado, abrir um túnel SSH em outro terminal:

```bash
ssh -i "$HOME/.ssh/nexo-azure" -N -L 18081:127.0.0.1:8080 "$AZURE_ADMIN_USER@$IP_PUBLICO"
```

Usar `http://127.0.0.1:18081/swagger-ui/index.html` pelo túnel protegido. A porta local é apenas o encaminhamento para o container da VM, não uma execução local da API. Deixar essa diferença explícita no vídeo e mostrar também a consulta ao IP público sem token.

Se `AZURE_API_PORT` mudar, o destino remoto do túnel deve usar a porta publicada na VM. Esse acesso protegido não configura HTTPS público. Não liberar a porta para toda a internet nem enviar o token por HTTP apenas para facilitar a gravação.

Mudança do IPv4 público administrativo exige revisão das regras NSG. Não há script que abra acesso irrestrito automaticamente.

## Exclusão e comprovação

Gravar as demonstrações e coletar evidências antes de excluir. Coordenar com as outras disciplinas caso dependam da API na VM. A exclusão do grupo não apaga o Oracle externo da FIAP, mas remove os recursos e arquivos que estiverem na VM.

Primeiro, visualizar o alvo:

```bash
bash infra/scripts/excluir-recursos.sh
```

Somente após autorizar a exclusão de todo o grupo dedicado:

```bash
CONFIRMAR_EXCLUSAO="$AZURE_RESOURCE_GROUP" bash infra/scripts/excluir-recursos.sh --executar
```

O script exige o nome exato do grupo, prefixo esperado, assinatura selecionada correta e tags do projeto. Lista os recursos, espera o comando de exclusão concluir e consulta novamente a existência do grupo. Não anuncia sucesso se o grupo continuar existindo. Esses controles não substituem conferir se alguém adicionou outro recurso importante ao grupo.

Comprovação adicional, somente leitura:

```bash
az group exists --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --output tsv
az resource list --subscription "$AZURE_SUBSCRIPTION_ID" --query "[?resourceGroup=='$AZURE_RESOURCE_GROUP'].{nome:name,tipo:type}" --output table
```

O primeiro comando deve retornar `false`; o segundo não deve listar recursos do grupo. Guardar a captura/trecho de vídeo com o alvo e os resultados. Não afirmar exclusão apenas porque o comando de delete retornou sem erro.

## O que foi verificado

- Os três arquivos `.sh` passaram em `bash -n`.
- O modo padrão dos três scripts foi executado localmente e não realizou chamadas Azure nem instalação.
- Caminhos de criação e exclusão foram exercitados com uma função `az` simulada, sem acesso ao serviço real.
- Testes cobriram confirmação ausente, ID inválido, origem irrestrita, assinatura diferente, grupo existente, SKU restrito, tags incorretas e ausência de confirmação real de exclusão.
- O fluxo do instalador foi exercitado em container Ubuntu 24.04 descartável, com apt, download, systemctl e Docker simulados; nenhum pacote foi instalado nesse teste.
- O Docker Compose validou o merge e confirmou que somente a porta da API muda, sem alterar as variáveis de conexão ou acrescentar banco.
- Consultas reais de leitura confirmaram assinatura ativa, política de regiões, quota e imagem. A consulta de SKUs expirou, portanto sua disponibilidade permanece pendente.
- Nenhuma VM, rede, grupo ou outro recurso Azure foi criado, alterado ou excluído neste módulo.
- A instalação real de Docker na VM, a execução da API na Azure, a conectividade Azure → Oracle, a gravação e a exclusão real ainda não foram feitas.

## Fontes oficiais

- [Instalar Docker no Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
- [Merge e override do Compose](https://docs.docker.com/reference/compose-file/merge/)
- [Criar VM por Azure CLI](https://learn.microsoft.com/en-us/cli/azure/vm?view=azure-cli-latest#az-vm-create)
- [NSG e regras](https://learn.microsoft.com/en-us/cli/azure/network/nsg/rule?view=azure-cli-latest)
- [IP público](https://learn.microsoft.com/en-us/cli/azure/network/public-ip?view=azure-cli-latest)
- [Grupos de recursos e exclusão](https://learn.microsoft.com/en-us/cli/azure/group?view=azure-cli-latest)
