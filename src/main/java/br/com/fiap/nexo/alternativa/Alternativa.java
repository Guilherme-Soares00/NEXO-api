package br.com.fiap.nexo.alternativa;

import br.com.fiap.nexo.cenario.Cenario;
import jakarta.persistence.*;

@Entity
@Table(name = "NEXO_ALTERNATIVA", uniqueConstraints =
        @UniqueConstraint(name = "uk_alternativa_cenario_rotulo", columnNames = {"cenario_id", "rotulo"}))
public class Alternativa {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nexo_alternativa_seq")
    @SequenceGenerator(name = "nexo_alternativa_seq", sequenceName = "SEQ_NEXO_ALTERNATIVA", allocationSize = 1)
    @Column(name = "ID_ALTERNATIVA", precision = 10, scale = 0)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cenario_id", nullable = false, foreignKey = @ForeignKey(name = "fk_alternativa_cenario"))
    private Cenario cenario;
    @Column(nullable = false, length = 1)
    private String rotulo;
    @Column(nullable = false, length = 1000)
    private String descricao;
    @Column(nullable = false, length = 1000)
    private String feedback;

    protected Alternativa() {}

    public Alternativa(Cenario cenario, AlternativaRequest request) { atualizar(cenario, request); }

    public void atualizar(Cenario cenario, AlternativaRequest request) {
        this.cenario = cenario;
        rotulo = request.rotulo();
        descricao = request.descricao();
        feedback = request.feedback();
    }

    public Long getId() { return id; }
    public Cenario getCenario() { return cenario; }
    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    public String getFeedback() { return feedback; }

}
