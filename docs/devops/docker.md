# Docker — API NEXO com Oracle externo

## Escopo

Esta etapa reutiliza a API Java existente na prova de conceito de DevOps da Sprint 1 V2, páginas 24–28. Somente a API roda em container. O Oracle permanece no servidor da FIAP e no schema compartilhado acordado pelo grupo.

O container não cria tabelas, sequences ou dados. Não depende de procedures ou functions. O perfil Oracle mantém `ddl-auto=validate` e inicialização SQL desabilitada. A estrutura deve existir antes de iniciar a API.

Docker padroniza o ambiente Java e o empacotamento da API. Cloud Computing permite executar essa mesma imagem em uma VM Azure, sem depender do IntelliJ ou do computador do desenvolvedor. Esta etapa valida a execução local; a demonstração avaliada ainda precisa ocorrer na VM criada por Azure CLI.

## Arquivos

| Arquivo | Responsabilidade |
| --- | --- |
| `Dockerfile` | Compilar com Maven/JDK 25 e executar somente o JAR com JRE 25 Alpine e usuário sem privilégios administrativos |
| `.dockerignore` | Limitar o contexto de build ao POM e a `src/main`, excluindo também arquivos `.env` e logs |
| `docker-compose.yml` | Executar somente a API, fornecer variáveis de ambiente e verificar a consulta pública de cenários |
| `docker.env.example` | Exemplo versionado sem usuário, senha ou token |
| `.env.docker` | Configuração local ignorada pelo Git e excluída do contexto de build |

O Compose é uma conveniência de execução, não um requisito expresso da V2. Não existe serviço Oracle ou volume de banco neste Compose: a persistência pertence ao Oracle externo.

## Ferramentas

| Ferramenta | Versão usada nesta validação | Comando de conferência |
| --- | --- | --- |
| Docker Desktop | 4.95.0 | Consultar About no Docker Desktop |
| Docker Engine | 29.8.2 | `docker version` |
| Docker Compose | 5.6.0 | `docker compose version` |
| Maven da imagem de build | 3.9.16 | `docker run --rm maven:3.9.16-eclipse-temurin-25 mvn -version` |
| Java da imagem de execução | Temurin 25.0.4.1 | `docker run --rm --entrypoint java nexo-api:local -version` |
| Spring Boot | 3.5.16 | Consultar `pom.xml` |

As tags usadas são `maven:3.9.16-eclipse-temurin-25` e `eclipse-temurin:25-jre-alpine`. Tags podem receber novas imagens; confira a versão e o ID efetivos ao reproduzir.

## 1. Preparar o ambiente

No Windows, iniciar o Docker Desktop com containers Linux e executar os comandos na raiz do repositório. Não é necessário instalar Java ou Maven no host para construir a imagem.

```powershell
docker version
docker compose version
```

Criar o arquivo local somente se ele ainda não existir:

```powershell
if (-not (Test-Path -LiteralPath .env.docker)) {
    Copy-Item -LiteralPath docker.env.example -Destination .env.docker
}
notepad .env.docker
```

Preencher `APP_USER` e `APP_USER_PASSWORD` com o acesso autorizado ao schema do grupo. Conferir `ORACLE_JDBC_URL`: o exemplo usa o host, a porta e o SID usados na conexão FIAP validada neste projeto. O nome da conexão salva no SQL Developer não é utilizado.

Senhas com `$`, `#` ou espaços devem ser delimitadas por aspas simples no arquivo de ambiente para preservar o valor literal. Não colocar os valores no histórico do terminal, no README, no vídeo ou em capturas.

O Compose não importa configurações de execução do IntelliJ. `.env.docker` é separado do `.env` usado pela IDE. Variáveis já definidas no terminal têm precedência na interpolação do Compose; remova valores antigos dessa sessão se apontarem para outro destino.

`NEXO_ADMIN_TOKEN` deve ser um segredo aleatório de 64 caracteres hexadecimais. Se o arquivo local já tiver um token válido, preserve-o. Para gerar ou rotacionar um token sem imprimi-lo, depois de criar `.env.docker`, usar PowerShell:

```powershell
$bytes = New-Object byte[] 32
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
    $rng.GetBytes($bytes)
    $token = [System.BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
    $path = (Resolve-Path -LiteralPath .env.docker).Path
    $lines = (Get-Content -LiteralPath $path) -replace '^NEXO_ADMIN_TOKEN=.*$', "NEXO_ADMIN_TOKEN=$token"
    [System.IO.File]::WriteAllLines($path, $lines, (New-Object System.Text.UTF8Encoding($false)))
} finally {
    $rng.Dispose()
    [System.Array]::Clear($bytes, 0, $bytes.Length)
    Remove-Variable token, bytes, lines -ErrorAction SilentlyContinue
}
```

O token do container pode ser diferente do token da execução no IntelliJ. O cliente administrativo precisa usar o token da instância que está acessando. Reiniciar/recriar o container após alterar a configuração.

Ignorar um arquivo no Git não o criptografa. Os segredos continuam disponíveis localmente e no ambiente do container para quem administra o Docker. Não compartilhar a saída de comandos que exibam o ambiente resolvido.

## 2. Testar e construir

Se Java 25 e Maven estiverem disponíveis no host, executar a suíte local antes de empacotar:

```powershell
mvn -B -ntp verify
```

Esse comando não executa a suíte Oracle. Não usar `-Poracle-it` contra o schema compartilhado: os testes de integração exigem outro schema exclusivo e vazio.

Construir a imagem:

```powershell
docker build -t nexo-api:local .
```

O Dockerfile copia somente o POM e o código de produção. O build compila e empacota sem testes com `-Dmaven.test.skip=true`; ele não substitui a execução separada de `mvn verify`. A imagem final recebe apenas o JAR da aplicação, sem Maven, código-fonte ou arquivos locais de credenciais.

Validar a configuração sem imprimir seus segredos:

```powershell
docker compose --env-file .env.docker config --quiet
```

Ausência de URL, usuário, senha ou token impede a resolução do Compose. O formato do token também é validado pela aplicação ao iniciar.

## 3. Executar em background

```powershell
docker compose --env-file .env.docker up -d --build
docker compose --env-file .env.docker ps
docker compose --env-file .env.docker logs --tail=100 api
```

A porta padrão é `127.0.0.1:8081` no computador e `8080` dentro do container, permitindo manter a execução do IntelliJ em `8080`. `API_PORT` permite trocar somente a porta local publicada.

O container utiliza o perfil `oracle`, escuta em `0.0.0.0:8080` internamente e conecta ao Oracle pela URL fornecida. `localhost` dentro do container identifica o próprio container, não o servidor Oracle da FIAP.

Configurações de execução:

- Usuário `nexo`, UID/GID 10001.
- Rede bridge nomeada `nexo-network`.
- Sistema de arquivos somente leitura e diretório temporário `/tmp` em memória.
- Capacidades Linux removidas e elevação de privilégios desabilitada.
- Limite de memória de 768 MiB, com heap Java ajustado proporcionalmente.
- Sem reinício automático após falhas, evitando tentativas contínuas com credenciais incorretas.
- Healthcheck por GET público em `/cenarios?page=0&size=1`, que verifica HTTP e leitura no banco, sem gravar registros.

## 4. Conferir somente leitura

```powershell
curl.exe -i "http://127.0.0.1:8081/cenarios?page=0&size=1"
curl.exe -i "http://127.0.0.1:8081/v3/api-docs"
curl.exe -i "http://127.0.0.1:8081/decisoes"
```

As consultas de cenários e OpenAPI devem retornar `200`. Consultar decisões sem token deve retornar `401`. Uma página de cenários vazia continua sendo uma resposta válida.

Swagger: `http://127.0.0.1:8081/swagger-ui/index.html`.

O estado deve passar a `healthy` quando o healthcheck conseguir consultar cenários. `unhealthy` indica falhas nas consultas, mas não reinicia o container automaticamente. Consultar logs e verificar conectividade, credenciais, estrutura e permissões; não executar DDL ou apagar dados para tentar corrigir a inicialização.

POST, PUT e DELETE alteram o schema compartilhado e só devem ser executados com autorização e registros de demonstração acordados. Esta validação de container não executa as coleções Postman nem a carga de dados.

## 5. Parar ou atualizar

```powershell
docker compose --env-file .env.docker down
```

Esse comando remove o container e a rede do Compose. Não exclui dados no Oracle externo.

Após mudar código ou variáveis:

```powershell
docker compose --env-file .env.docker up -d --build --force-recreate
```

Alterações de dados feitas pela API continuam no Oracle independentemente da remoção do container.

## Validação executada neste módulo

- `mvn -B -ntp verify`: BUILD SUCCESS, 147 testes locais, sem falhas, erros ou ignorados.
- Build real de `nexo-api:local` concluído com sucesso.
- Verificações automatizadas do Dockerfile, isolamento do contexto de build e resolução do Compose aprovadas; ausência de senha rejeitada antes de iniciar.
- Execução de shell e Java dentro da imagem confirmou UID/GID 10001 e Temurin 25.0.4.1.
- Container executado com sistema de arquivos somente leitura, sem capacidades Linux e com `no-new-privileges`.
- API no container retornou `200` em cenários, Swagger e OpenAPI, usando o Oracle externo configurado pelo responsável.
- GET de decisões sem token retornou `401`; com o token local do container, retornou `200`.
- Healthcheck atingiu `healthy` e a porta foi publicada somente em `127.0.0.1:8081`.
- Nenhuma escrita, instalação de schema ou carga de dados foi feita no Oracle neste módulo.
- Nenhum recurso Azure foi criado; testes locais não comprovam conectividade a partir da nuvem.

## Próxima etapa: Azure

O Compose atual mantém a publicação em loopback para testes locais e acesso administrativo protegido. Não basta copiá-lo para a VM e esperar acesso público direto.

Os scripts de provisionamento, instalação e exclusão e o guia de acesso remoto estão preparados em [Azure](azure.md). A execução real, a configuração na VM e a conectividade com o Oracle ainda dependem da sessão autorizada. Não enviar o token administrativo por HTTP público. A aplicação precisa ser demonstrada em execução na VM, não apenas localmente.

O [diagrama da arquitetura](arquitetura-devops.md) está preparado. A gravação, o PDF final e a comprovação da exclusão dependem da demonstração real na VM.
