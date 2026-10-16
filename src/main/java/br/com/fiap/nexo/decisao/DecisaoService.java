package br.com.fiap.nexo.decisao;

import br.com.fiap.nexo.shared.*;
import br.com.fiap.nexo.alternativa.Alternativa;
import br.com.fiap.nexo.alternativa.AlternativaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class DecisaoService {
    private final DecisaoRepository repository;
    private final AlternativaRepository alternativas;

    public DecisaoService(DecisaoRepository repository, AlternativaRepository alternativas) {
        this.repository = repository;
        this.alternativas = alternativas;
    }

    public ApiPage<DecisaoResponse> listar(int page, int size) {
        return ApiPage.from(repository.findAll(Pagination.of(page, size)).map(DecisaoResponse::from));
    }

    public DecisaoResponse obter(Long id) { return DecisaoResponse.from(encontrar(id)); }

    @Transactional
    public DecisaoResponse criar(DecisaoRequest request) {
        Alternativa alternativa = encontrarAlternativa(request.alternativaId());
        return DecisaoResponse.from(repository.saveAndFlush(new Decisao(alternativa, request)));
    }

    @Transactional
    public DecisaoResponse atualizar(Long id, DecisaoRequest request) {
        Decisao decisao = encontrar(id);
        decisao.atualizar(encontrarAlternativa(request.alternativaId()), request);
        return DecisaoResponse.from(repository.saveAndFlush(decisao));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrar(id));
        repository.flush();
    }

    private Alternativa encontrarAlternativa(Long id) {
        return alternativas.findById(id).orElseThrow(() -> new NotFoundException("Alternativa não encontrada."));
    }

    private Decisao encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Decisão não encontrada."));
    }

}
