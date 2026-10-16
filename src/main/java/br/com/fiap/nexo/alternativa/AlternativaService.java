package br.com.fiap.nexo.alternativa;

import br.com.fiap.nexo.shared.*;
import br.com.fiap.nexo.cenario.Cenario;
import br.com.fiap.nexo.cenario.CenarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AlternativaService {
    private final AlternativaRepository repository;
    private final CenarioRepository cenarios;

    public AlternativaService(AlternativaRepository repository, CenarioRepository cenarios) {
        this.repository = repository;
        this.cenarios = cenarios;
    }

    public ApiPage<AlternativaResponse> listar(int page, int size) {
        return ApiPage.from(repository.findAll(Pagination.of(page, size)).map(AlternativaResponse::from));
    }

    public AlternativaResponse obter(Long id) { return AlternativaResponse.from(encontrar(id)); }

    @Transactional
    public AlternativaResponse criar(AlternativaRequest request) {
        Cenario cenario = encontrarCenario(request.cenarioId());
        return AlternativaResponse.from(repository.saveAndFlush(new Alternativa(cenario, request)));
    }

    @Transactional
    public AlternativaResponse atualizar(Long id, AlternativaRequest request) {
        Alternativa alternativa = encontrar(id);
        alternativa.atualizar(encontrarCenario(request.cenarioId()), request);
        return AlternativaResponse.from(repository.saveAndFlush(alternativa));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrar(id));
        repository.flush();
    }

    private Cenario encontrarCenario(Long id) {
        return cenarios.findById(id).orElseThrow(() -> new NotFoundException("Cenário não encontrado."));
    }

    private Alternativa encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Alternativa não encontrada."));
    }

}
