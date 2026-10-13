package br.com.fiap.nexo.cenario;

import jakarta.persistence.*;

@Entity
@Table(name = "NEXO_CENARIO")
public class Cenario {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nexo_cenario_seq")
    @SequenceGenerator(name = "nexo_cenario_seq", sequenceName = "SEQ_NEXO_CENARIO", allocationSize = 1)
    @Column(name = "ID_CENARIO", precision = 10, scale = 0)
    private Long id;
    @Column(nullable = false, length = 150)
    private String titulo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;
    @Column(nullable = false, length = 1000)
    private String situacao;
    @Column(name = "objetivo_educacional", nullable = false, length = 500)
    private String objetivoEducacional;
    @Column(name = "fonte_url", nullable = false, length = 1000)
    private String fonteUrl;

    protected Cenario() {}

    public Cenario(CenarioRequest request) { atualizar(request); }

    public void atualizar(CenarioRequest request) {
        titulo = request.titulo();
        categoria = request.categoria();
        situacao = request.situacao();
        objetivoEducacional = request.objetivoEducacional();
        fonteUrl = request.fonteUrl();
    }

    public Long getId() { return id; }
    public String getTitulo() { return titulo; }
    public Categoria getCategoria() { return categoria; }
    public String getSituacao() { return situacao; }
    public String getObjetivoEducacional() { return objetivoEducacional; }
    public String getFonteUrl() { return fonteUrl; }
}
