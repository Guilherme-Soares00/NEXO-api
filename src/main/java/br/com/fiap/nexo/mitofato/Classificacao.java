package br.com.fiap.nexo.mitofato;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Classificação da afirmação educativa")
public enum Classificacao {
    MITO, FATO
}
