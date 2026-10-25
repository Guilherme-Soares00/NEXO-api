#!/usr/bin/env bash
set -euo pipefail

modo=${1:---planejar}
if (( $# > 1 )); then
    printf 'Uso: bash %s [--planejar|--executar]\n' "$0" >&2
    exit 2
fi
case "$modo" in
    --planejar)
        printf 'PLANEJAMENTO: nenhum pacote será instalado e nenhum arquivo de sistema será alterado.\n'
        printf 'Destino da execução futura: VM Ubuntu 24.04 LTS x86-64.\n'
        printf 'Instalar Docker Engine, CLI, Buildx, Compose e Git pelo repositório oficial Docker.\n'
        printf 'Para executar na VM: sudo bash infra/scripts/instalar-docker.sh --executar\n'
        exit 0
        ;;
    --executar) ;;
    *) printf 'Modo inválido. Use --planejar ou --executar.\n' >&2; exit 2 ;;
esac

(( EUID == 0 )) || { printf 'Execute somente na VM: sudo bash %s --executar\n' "$0" >&2; exit 1; }
[[ -f /etc/os-release ]] || { printf 'Sistema operacional não identificado.\n' >&2; exit 1; }
. /etc/os-release
[[ ${ID:-} == ubuntu && ${VERSION_ID:-} == 24.04 ]] || { printf 'Este script é destinado ao Ubuntu 24.04 LTS.\n' >&2; exit 1; }
[[ $(uname -m) == x86_64 ]] || { printf 'Arquitetura esperada: x86-64.\n' >&2; exit 1; }

for pacote in docker.io docker-compose docker-compose-v2 docker-doc docker-buildx podman-docker containerd runc; do
    estado=$(dpkg-query -W -f='${Status}' "$pacote" 2>/dev/null || true)
    if [[ "$estado" == 'install ok installed' ]]; then
        printf 'Pacote conflitante encontrado: %s. Revise manualmente; nada será removido automaticamente.\n' "$pacote" >&2
        exit 1
    fi
done

apt-get update
apt-get install -y ca-certificates curl
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
printf 'Types: deb\nURIs: https://download.docker.com/linux/ubuntu\nSuites: %s\nComponents: stable\nArchitectures: %s\nSigned-By: /etc/apt/keyrings/docker.asc\n' "$VERSION_CODENAME" "$(dpkg --print-architecture)" > /etc/apt/sources.list.d/docker.sources
apt-get update
apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin git
systemctl enable --now docker
docker version
docker compose version
printf 'Instalação concluída. Use sudo docker; o usuário SSH não foi adicionado ao grupo docker.\n'
