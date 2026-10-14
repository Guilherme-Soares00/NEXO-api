package br.com.fiap.nexo.mitofato;

import br.com.fiap.nexo.cenario.Cenario;
import jakarta.persistence.*;

@Entity
@Table(name = "NEXO_MITO_FATO")
public class MitoFato {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nexo_mito_fato_seq")
    @SequenceGenerator(name = "nexo_mito_fato_seq", sequenceName = "SEQ_NEXO_MITO_FATO", allocationSize = 1)
    @Column(name = "ID_MITO_FATO", precision = 10, scale = 0)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CENARIO_ID", nullable = false)
    private Cenario cenario;
    @Column(nullable = false, length = 500)
    private String afirmacao;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private Classificacao classificacao;
    @Column(nullable = false, length = 1000)
    private String explicacao;
    @Column(name = "FONTE_URL", nullable = false, length = 1000)
    private String fonteUrl;

    protected MitoFato() {}

    public MitoFato(Cenario cenario, MitoFatoRequest request) { atualizar(cenario, request); }

    public void atualizar(Cenario cenario, MitoFatoRequest request) {
        this.cenario = cenario;
        afirmacao = request.afirmacao();
        classificacao = request.classificacao();
        explicacao = request.explicacao();
        fonteUrl = request.fonteUrl();
    }

    public Long getId() { return id; }
    public Cenario getCenario() { return cenario; }
    public String getAfirmacao() { return afirmacao; }
    public Classificacao getClassificacao() { return classificacao; }
    public String getExplicacao() { return explicacao; }
    public String getFonteUrl() { return fonteUrl; }
}
