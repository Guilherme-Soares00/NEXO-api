# Arquitetura DevOps do NEXO

A API NEXO foi executada em um container Docker dentro de uma VM Linux no Azure. O Oracle permaneceu no servidor da FIAP, com o schema compartilhado do grupo. Essa separação permitiu remover os recursos da demonstração sem incluir o banco na exclusão.

O desenho representa a arquitetura demonstrada em `chilecentral`. O vídeo registra a VM em execução, o container saudável e uma consulta de cenários com retorno de dados pelo IP público. Depois da demonstração, o grupo Azure foi excluído. O diagrama documenta esse ambiente, não um serviço que continua disponível.

## Diagrama

![Arquitetura demonstrada do NEXO: acesso restrito ao Azure Public IP, NSG, interface de rede e VM Ubuntu com Docker e API, conectada ao Oracle FIAP externo. Recursos Azure excluídos ao término.](diagrama-arquitetura.png)

- [Abrir a imagem PNG](diagrama-arquitetura.png)

O diagrama em PNG inclui os ícones e os nomes dos serviços Azure e é utilizado no README, no vídeo e no PDF.

## Componentes

| Componente | Papel na solução |
| --- | --- |
| Navegador ou Postman | Consulta a API a partir do IPv4 administrativo autorizado |
| Azure Public IP, `pip-nexo` | Fornece o endereço público da VM para a demonstração |
| Azure Network Security Group, `nsg-nexo` | Permite entrada TCP 22 e 8080 somente de `ADMIN_CIDR`, com máscara `/32` |
| Azure Virtual Network, `vnet-nexo` | Organiza a rede da VM; a interface utiliza `subnet-nexo` |
| Azure Network Interface, `nic-nexo` | Conecta a VM à subnet, ao IP público e ao NSG |
| Azure Virtual Machines, `vm-nexo` | Hospeda Ubuntu 24.04 LTS x86-64 e Docker |
| Docker Engine e `nexo-network` | Executam o container da API em uma rede bridge nomeada |
| API NEXO | Usa Java 25, Spring Boot e JPA, com porta interna 8080 e usuário sem privilégios administrativos |
| Oracle FIAP | Mantém as tabelas e os dados do grupo fora da VM Azure |

O modelo configurado foi `Standard_B2als_v2`, com 2 vCPUs e 4 GiB de RAM, mais barato que a candidata Intel equivalente entre os preços comparados na região. O disco de sistema foi configurado com 32 GiB e Standard HDD LRS. A disponibilidade para uma nova execução precisa ser consultada novamente; não há troca automática por uma VM mais cara.

## Como os componentes se comunicam

As consultas públicas chegam ao IP da VM pela porta 8080. O NSG limita a origem dessas conexões ao endereço administrativo configurado. No desenho, ele aparece no caminho para destacar esse controle, mas não é um servidor intermediário: suas regras são aplicadas à interface de rede.

O Docker encaminha a porta publicada para a porta 8080 do container. A API usa a URL JDBC para acessar `oracle.fiap.com.br:1521`, com SID `ORCL` e as credenciais autorizadas. Nenhum serviço de banco é criado no Azure ou no Compose.

A administração usa SSH na porta 22. Para enviar o token da API, o acesso passa por um túnel SSH: a porta local 18081 encaminha as requisições à porta 8080 da VM. Essa porta local não é uma segunda implantação. A consulta ao IP público e a visualização do container pelo terminal SSH comprovam onde a aplicação está executando.

O projeto não configura HTTPS público nesta etapa. Por isso, o token não deve ser enviado pelo endereço HTTP público. A porta 1521 do Oracle não é liberada como entrada na VM; ela é o destino externo da conexão JDBC.

## Persistência e exclusão

O disco mostrado pertence ao sistema operacional da VM, não ao Oracle. O Compose não cria um volume de banco. Os dados continuam no schema FIAP mesmo depois que o container, a VM e o grupo de recursos forem excluídos.

Isso não substitui uma política de backup do banco. A exclusão remove os arquivos que estiverem na VM, então as evidências da entrega precisam ser copiadas antes da limpeza.

O script configura a exclusão do disco de sistema e da NIC junto à VM. O IP público não tem exclusão automática configurada; sua limpeza é feita pela exclusão de todo o grupo de recursos, que também remove VNet e NSG. A limpeza só confirma sucesso após verificar que o grupo está ausente e a consulta de recursos restantes está vazia. Excluir somente a VM não substitui esse procedimento.

## Evidências e reprodução

A [demonstração em vídeo](https://www.youtube.com/watch?v=KHZwnoqLOJQ) registra provisionamento, instalação, build, consultas HTTP e exclusão do grupo. Não foi demonstrada uma restauração do banco nem a exclusão em cascata do IP ao apagar somente a VM.

Os parâmetros estão em [azure.env.example](../../azure.env.example), e os comandos de implantação estão no [guia Azure](azure.md).

## Ícones e formatos

Os ícones Azure vieram da [coleção oficial do Azure Architecture Center](https://learn.microsoft.com/en-us/azure/architecture/icons/), pacote `Azure_Public_Service_Icons_V24.zip`. Eles são usados com os nomes dos serviços, sem recorte, rotação ou mudança de proporção.

Os símbolos Docker e Spring Boot vieram do projeto [Simple Icons](https://github.com/simple-icons/simple-icons). As marcas identificam as tecnologias utilizadas; o NEXO não é um serviço oficial dessas empresas.

O PNG incorpora os ícones, pode ser aberto sem acesso à internet e está em alta resolução para uso em gravações e documentos.
