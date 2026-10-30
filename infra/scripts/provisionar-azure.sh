#!/usr/bin/env bash
set -euo pipefail
export MSYS_NO_PATHCONV=1

modo=${1:---planejar}
if (( $# > 1 )); then
    printf 'Uso: bash %s [--planejar|--executar]\n' "$0" >&2
    exit 2
fi
case "$modo" in
    --planejar|--executar) ;;
    *) printf 'Modo inválido. Use --planejar ou --executar.\n' >&2; exit 2 ;;
esac

AZURE_SUBSCRIPTION_ID=${AZURE_SUBSCRIPTION_ID:-PREENCHER}
AZURE_RESOURCE_GROUP=${AZURE_RESOURCE_GROUP:-rg-nexo-devops}
AZURE_LOCATION=${AZURE_LOCATION:-chilecentral}
AZURE_VM_NAME=${AZURE_VM_NAME:-vm-nexo}
AZURE_VM_SIZE=${AZURE_VM_SIZE:-Standard_B2als_v2}
AZURE_IMAGE=${AZURE_IMAGE:-Canonical:ubuntu-24_04-lts:server:24.04.202610210}
AZURE_ADMIN_USER=${AZURE_ADMIN_USER:-nexoadmin}
AZURE_API_PORT=${AZURE_API_PORT:-8080}
ADMIN_CIDR=${ADMIN_CIDR:-PREENCHER}
SSH_PUBLIC_KEY=${SSH_PUBLIC_KEY:-$HOME/.ssh/nexo-azure.pub}
chave_ssh=$SSH_PUBLIC_KEY

falhar() {
    printf '%s\n' "$1" >&2
    exit 1
}

azure() {
    printf '+ az '
    printf '%q ' "$@" --subscription "$AZURE_SUBSCRIPTION_ID" --only-show-errors
    printf '\n'
    if [[ "$modo" == --executar ]]; then
        az "$@" --subscription "$AZURE_SUBSCRIPTION_ID" --only-show-errors
    fi
}

if [[ "$modo" == --executar ]]; then
    [[ ${CONFIRMAR_CRIACAO:-} == SIM ]] || falhar 'Bloqueado: defina CONFIRMAR_CRIACAO=SIM somente após aprovar custos e execução.'
    [[ "$AZURE_SUBSCRIPTION_ID" =~ ^[[:xdigit:]]{8}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{12}$ ]] || falhar 'AZURE_SUBSCRIPTION_ID deve conter o ID exato da assinatura.'
    [[ "$AZURE_RESOURCE_GROUP" =~ ^rg-nexo-devops(-[a-z0-9]+)*$ ]] || falhar 'O grupo deve começar com rg-nexo-devops e usar somente sufixos alfanuméricos.'
    [[ "$AZURE_VM_NAME" =~ ^[a-z][a-z0-9-]{0,50}$ ]] || falhar 'Nome de VM inválido.'
    [[ "$AZURE_ADMIN_USER" =~ ^[a-z][a-z0-9-]{0,30}$ && "$AZURE_ADMIN_USER" != root ]] || falhar 'Usuário Linux inválido.'
    [[ "$AZURE_VM_SIZE" =~ ^Standard_[A-Za-z0-9_]+$ ]] || falhar 'Tamanho de VM inválido.'
    case "$AZURE_LOCATION" in
        chilecentral|mexicocentral|eastus|canadacentral|centralus) ;;
        *) falhar 'Região fora da lista permitida observada na assinatura. Confira a política antes de alterar o script.' ;;
    esac
    [[ "$AZURE_API_PORT" =~ ^[0-9]{4,5}$ ]] || falhar 'AZURE_API_PORT deve ser uma porta entre 1024 e 65535.'
    (( 10#$AZURE_API_PORT >= 1024 && 10#$AZURE_API_PORT <= 65535 )) || falhar 'AZURE_API_PORT fora do intervalo permitido.'
    [[ "$ADMIN_CIDR" =~ ^([0-9]{1,3}\.){3}[0-9]{1,3}/32$ ]] || falhar 'ADMIN_CIDR deve ser um IPv4 administrativo com máscara /32; acesso irrestrito não é aceito.'
    IFS=. read -r -a octetos <<< "${ADMIN_CIDR%/32}"
    for octeto in "${octetos[@]}"; do
        (( 10#$octeto <= 255 )) || falhar 'ADMIN_CIDR contém um octeto inválido.'
    done
    [[ ${ADMIN_CIDR%/32} != 0.0.0.0 ]] || falhar 'Informe um endereço administrativo válido.'
    command -v az >/dev/null || falhar 'Azure CLI não encontrada. Reabra o terminal após instalar.'
    [[ -f "$SSH_PUBLIC_KEY" ]] || falhar 'Chave pública SSH não encontrada. Nenhuma chave será gerada automaticamente.'
    chave_ssh=$(<"$SSH_PUBLIC_KEY")
    chave_ssh=${chave_ssh%$'\r'}
    [[ "$chave_ssh" == ssh-ed25519\ * || "$chave_ssh" == ssh-rsa\ * ]] || falhar 'Informe um arquivo de chave pública SSH, nunca a chave privada.'
    assinatura=$(az account show --query id --output tsv --only-show-errors)
    assinatura=${assinatura%$'\r'}
    [[ "$assinatura" == "$AZURE_SUBSCRIPTION_ID" ]] || falhar 'A assinatura selecionada difere de AZURE_SUBSCRIPTION_ID. Confira o destino antes de continuar.'
    existe=$(az group exists --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --output tsv --only-show-errors)
    existe=${existe%$'\r'}
    [[ "$existe" == false ]] || falhar 'O grupo já existe ou sua ausência não foi confirmada. Nenhum recurso existente será reutilizado.'
    disponivel=$(az vm list-skus --location "$AZURE_LOCATION" --size "$AZURE_VM_SIZE" --resource-type virtualMachines --all --query "length([?name=='$AZURE_VM_SIZE' && !restrictions])" --subscription "$AZURE_SUBSCRIPTION_ID" --output tsv --only-show-errors)
    disponivel=${disponivel%$'\r'}
    [[ "$disponivel" =~ ^[0-9]+$ && "$disponivel" -gt 0 ]] || falhar 'SKU indisponível ou restrito. O script não troca região ou tamanho automaticamente.'
    az vm image show --location "$AZURE_LOCATION" --urn "$AZURE_IMAGE" --subscription "$AZURE_SUBSCRIPTION_ID" --output none --only-show-errors
    printf 'EXECUÇÃO AUTORIZADA: os próximos comandos criam recursos que podem consumir créditos.\n'
    trap 'printf "Falha no provisionamento. Recursos parciais podem permanecer no grupo %s; confira antes de usar o script de exclusão.\n" "$AZURE_RESOURCE_GROUP" >&2' ERR
else
    printf 'PLANEJAMENTO: nenhuma chamada Azure será executada.\n'
fi

azure group create --name "$AZURE_RESOURCE_GROUP" --location "$AZURE_LOCATION" --tags projeto=NEXO finalidade=devops-sprint1 --output none
azure network vnet create --resource-group "$AZURE_RESOURCE_GROUP" --name vnet-nexo --location "$AZURE_LOCATION" --address-prefixes 10.20.0.0/16 --subnet-name subnet-nexo --subnet-prefixes 10.20.1.0/24 --output none
azure network nsg create --resource-group "$AZURE_RESOURCE_GROUP" --name nsg-nexo --location "$AZURE_LOCATION" --output none
azure network nsg rule create --resource-group "$AZURE_RESOURCE_GROUP" --nsg-name nsg-nexo --name permitir-ssh --priority 100 --direction Inbound --access Allow --protocol Tcp --source-address-prefixes "$ADMIN_CIDR" --source-port-ranges '*' --destination-address-prefixes '*' --destination-port-ranges 22 --output none
azure network nsg rule create --resource-group "$AZURE_RESOURCE_GROUP" --nsg-name nsg-nexo --name permitir-api --priority 110 --direction Inbound --access Allow --protocol Tcp --source-address-prefixes "$ADMIN_CIDR" --source-port-ranges '*' --destination-address-prefixes '*' --destination-port-ranges "$AZURE_API_PORT" --output none
azure network public-ip create --resource-group "$AZURE_RESOURCE_GROUP" --name pip-nexo --location "$AZURE_LOCATION" --sku Standard --allocation-method Static --version IPv4 --output none
azure network nic create --resource-group "$AZURE_RESOURCE_GROUP" --name nic-nexo --location "$AZURE_LOCATION" --vnet-name vnet-nexo --subnet subnet-nexo --network-security-group nsg-nexo --public-ip-address pip-nexo --output none
azure network nic update --resource-group "$AZURE_RESOURCE_GROUP" --name nic-nexo --set 'ipConfigurations[0].publicIPAddress.deleteOption=Delete' --output none
if [[ "$modo" == --executar ]]; then
    exclusao_ip=$(az network nic show --resource-group "$AZURE_RESOURCE_GROUP" --name nic-nexo --subscription "$AZURE_SUBSCRIPTION_ID" --query 'ipConfigurations[0].publicIPAddress.deleteOption' --output tsv --only-show-errors)
    exclusao_ip=${exclusao_ip%$'\r'}
    [[ "$exclusao_ip" == Delete ]] || falhar 'Exclusão automática do IP não confirmada. A VM não foi criada; confira e autorize a limpeza do grupo parcial.'
fi
azure vm create --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_VM_NAME" --location "$AZURE_LOCATION" --size "$AZURE_VM_SIZE" --image "$AZURE_IMAGE" --admin-username "$AZURE_ADMIN_USER" --authentication-type ssh --ssh-key-values "$chave_ssh" --nics nic-nexo --nic-delete-option Delete --os-disk-name os-nexo --os-disk-size-gb 32 --os-disk-delete-option Delete --storage-sku Standard_LRS --security-type Standard --tags projeto=NEXO finalidade=devops-sprint1 --output none
if [[ "$modo" == --executar ]]; then
    exclusao_vm=$(az vm show --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_VM_NAME" --subscription "$AZURE_SUBSCRIPTION_ID" --query "storageProfile.osDisk.deleteOption=='Delete' && networkProfile.networkInterfaces[0].deleteOption=='Delete'" --output tsv --only-show-errors)
    exclusao_vm=${exclusao_vm%$'\r'}
    [[ "$exclusao_vm" == true ]] || falhar 'Exclusão automática do disco e da NIC não confirmada. Confira os recursos e autorize a limpeza do grupo.'
    printf 'Exclusão automática confirmada: disco do sistema, NIC e IP público configurados com Delete.\n'
fi
azure vm show --resource-group "$AZURE_RESOURCE_GROUP" --name "$AZURE_VM_NAME" --show-details --query '{nome:name,estado:powerState,ipPublico:publicIps,tamanho:hardwareProfile.vmSize}' --output table
azure network nsg rule list --resource-group "$AZURE_RESOURCE_GROUP" --nsg-name nsg-nexo --query '[].{regra:name,origem:sourceAddressPrefix,porta:destinationPortRange,acesso:access}' --output table
