package br.com.fiap.nexo.cenario;

import br.com.fiap.nexo.alternativa.AlternativaRepository;
import br.com.fiap.nexo.alternativa.AlternativaResponse;
import br.com.fiap.nexo.shared.*;
import br.com.fiap.nexo.mitofato.MitoFatoRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CenarioService {
    private final CenarioRepository repository;
    private final AlternativaRepository alternativas;
    private final MitoFatoRepository mitosFatos;

    public CenarioService(CenarioRepository repository, AlternativaRepository alternativas,
                          MitoFatoRepository mitosFatos) {
        this.repository = repository;
        this.alternativas = alternativas;
        this.mitosFatos = mitosFatos;
    }

    public ApiPage<CenarioResponse> listar(int page, int size) {
        return ApiPage.from(repository.findAll(Pagination.of(page, size)).map(CenarioResponse::from));
    }

    public ApiPage<CenarioResponse> buscar(String texto, int page, int size) {
        texto = Text.trim(texto);
        if (texto == null || texto.isBlank() || texto.length() > 150) {
            throw new InvalidInputException("Texto de busca deve ter de 1 a 150 caracteres após trim.");
        }
        return ApiPage.from(repository.findByTituloContainingIgnoreCase(texto, Pagination.of(page, size)).map(CenarioResponse::from));
    }

    public CenarioResponse obter(Long id) { return CenarioResponse.from(encontrar(id)); }

    public CenarioJogoResponse obterParaJogo(Long id) {
        CenarioResponse cenario = obter(id);
        var pagina = alternativas.findByCenarioId(id, Pagination.of(0, 3, "rotulo"));
        var opcoes = pagina.map(AlternativaResponse::from).getContent();
        if (pagina.getTotalElements() != 2
                || !opcoes.stream().map(AlternativaResponse::rotulo).toList().equals(List.of("A", "B"))
                || !mitosFatos.existsByCenarioId(id)) {
            throw new ConflictException("O cenário precisa das alternativas A e B e de pelo menos um Mito ou Fato para exibição no jogo.");
        }
        return new CenarioJogoResponse(cenario, opcoes);
    }

    public ApiPage<AlternativaResponse> listarAlternativas(Long id, int page, int size) {
        encontrar(id);
        return ApiPage.from(alternativas.findByCenarioId(id, Pagination.of(page, size, "rotulo")).map(AlternativaResponse::from));
    }

    @Transactional
    public CenarioResponse criar(CenarioRequest request) {
        return CenarioResponse.from(repository.saveAndFlush(new Cenario(request)));
    }

    @Transactional
    public CenarioResponse atualizar(Long id, CenarioRequest request) {
        Cenario cenario = encontrar(id);
        cenario.atualizar(request);
        return CenarioResponse.from(repository.saveAndFlush(cenario));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrar(id));
        repository.flush();
    }

    private Cenario encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Cenário não encontrado."));
    }
}
