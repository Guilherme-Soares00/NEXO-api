package br.com.fiap.nexo.cenario;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Área de aplicação do cenário")
public enum Categoria {
    SAUDE, AGRICULTURA, INDUSTRIA, PESQUISA, ENERGIA
}
