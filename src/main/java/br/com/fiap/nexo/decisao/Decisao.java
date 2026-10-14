package br.com.fiap.nexo.decisao;

import br.com.fiap.nexo.alternativa.Alternativa;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "NEXO_DECISAO")
public class Decisao {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "nexo_decisao_seq")
    @SequenceGenerator(name = "nexo_decisao_seq", sequenceName = "SEQ_NEXO_DECISAO", allocationSize = 1)
    @Column(name = "ID_DECISAO", precision = 10, scale = 0)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alternativa_id", nullable = false, foreignKey = @ForeignKey(name = "fk_decisao_alternativa"))
    private Alternativa alternativa;
    @Column(nullable = false, length = 50)
    private String apelido;
    @Generated(event = EventType.INSERT)
    @Column(name = "data_escolha", nullable = false, insertable = false, updatable = false)
    private LocalDateTime dataEscolha;

    protected Decisao() {}

    public Decisao(Alternativa alternativa, DecisaoRequest request) { atualizar(alternativa, request); }

    public void atualizar(Alternativa alternativa, DecisaoRequest request) {
        this.alternativa = alternativa;
        apelido = request.apelido();
    }

    public Long getId() { return id; }
    public Alternativa getAlternativa() { return alternativa; }
    public String getApelido() { return apelido; }
    public LocalDateTime getDataEscolha() { return dataEscolha; }
}
