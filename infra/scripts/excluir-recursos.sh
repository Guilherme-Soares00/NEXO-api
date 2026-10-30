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

falhar() {
    printf '%s\n' "$1" >&2
    exit 1
}

if [[ "$modo" == --planejar ]]; then
    printf 'PLANEJAMENTO: nenhuma chamada Azure será executada.\n'
    printf 'A execução futura excluirá TODO o grupo %s na assinatura %s.\n' "$AZURE_RESOURCE_GROUP" "$AZURE_SUBSCRIPTION_ID"
    printf 'Exige --executar e CONFIRMAR_EXCLUSAO com o nome exato do grupo.\n'
    exit 0
fi

[[ ${CONFIRMAR_EXCLUSAO:-} == "$AZURE_RESOURCE_GROUP" ]] || falhar 'Bloqueado: CONFIRMAR_EXCLUSAO deve conter o nome exato do grupo.'
[[ "$AZURE_SUBSCRIPTION_ID" =~ ^[[:xdigit:]]{8}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{4}-[[:xdigit:]]{12}$ ]] || falhar 'AZURE_SUBSCRIPTION_ID deve conter o ID exato da assinatura.'
[[ "$AZURE_RESOURCE_GROUP" =~ ^rg-nexo-devops(-[a-z0-9]+)*$ ]] || falhar 'Recusa de exclusão: nome de grupo fora do prefixo rg-nexo-devops.'
command -v az >/dev/null || falhar 'Azure CLI não encontrada.'
assinatura=$(az account show --query id --output tsv --only-show-errors)
assinatura=${assinatura%$'\r'}
[[ "$assinatura" == "$AZURE_SUBSCRIPTION_ID" ]] || falhar 'A assinatura selecionada difere do destino configurado.'
existe=$(az group exists --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --output tsv --only-show-errors)
existe=${existe%$'\r'}
if [[ "$existe" == false ]]; then
    printf 'Grupo já ausente: %s. Nenhuma exclusão foi executada.\n' "$AZURE_RESOURCE_GROUP"
    exit 0
fi
[[ "$existe" == true ]] || falhar 'Não foi possível confirmar a existência do grupo.'
projeto=$(az group show --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --query tags.projeto --output tsv --only-show-errors)
projeto=${projeto%$'\r'}
finalidade=$(az group show --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --query tags.finalidade --output tsv --only-show-errors)
finalidade=${finalidade%$'\r'}
[[ "$projeto" == NEXO && "$finalidade" == devops-sprint1 ]] || falhar 'Recusa de exclusão: o grupo não possui as tags esperadas do projeto.'
az resource list --resource-group "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --query '[].{nome:name,tipo:type}' --output table --only-show-errors
az group delete --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --yes --only-show-errors
existe=$(az group exists --name "$AZURE_RESOURCE_GROUP" --subscription "$AZURE_SUBSCRIPTION_ID" --output tsv --only-show-errors)
existe=${existe%$'\r'}
[[ "$existe" == false ]] || falhar 'A exclusão ainda não foi confirmada. Consulte novamente antes de encerrar a demonstração.'
restantes=$(az resource list --subscription "$AZURE_SUBSCRIPTION_ID" --query "[?resourceGroup=='$AZURE_RESOURCE_GROUP'].{nome:name,tipo:type}" --output json --only-show-errors)
[[ "$restantes" =~ ^[[:space:]]*\[[[:space:]]*\][[:space:]]*$ ]] || falhar 'Ainda há recursos listados no grupo. A limpeza total não foi confirmada; confira antes de encerrar.'
printf 'Exclusão confirmada: grupo %s ausente e nenhum recurso restante nesse grupo.\n' "$AZURE_RESOURCE_GROUP"
printf '%s\n' "$restantes"
