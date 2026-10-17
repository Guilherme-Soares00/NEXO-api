package br.com.fiap.nexo.mitofato;

import br.com.fiap.nexo.cenario.CenarioRepository;
import br.com.fiap.nexo.cenario.Cenario;
import br.com.fiap.nexo.shared.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MitoFatoService {
    private final MitoFatoRepository repository;
    private final CenarioRepository cenarios;

    public MitoFatoService(MitoFatoRepository repository, CenarioRepository cenarios) {
        this.repository = repository;
        this.cenarios = cenarios;
    }

    public ApiPage<MitoFatoResponse> listar(int page, int size) {
        return ApiPage.from(repository.findAll(Pagination.of(page, size)).map(MitoFatoResponse::from));
    }

    public ApiPage<MitoFatoResponse> listarPorCenario(Long id, int page, int size) {
        if (!cenarios.existsById(id)) throw new NotFoundException("Cenário não encontrado.");
        return ApiPage.from(repository.findByCenarioId(id, Pagination.of(page, size)).map(MitoFatoResponse::from));
    }

    public MitoFatoResponse obter(Long id) {
        return MitoFatoResponse.from(encontrar(id));
    }

    @Transactional
    public MitoFatoResponse criar(MitoFatoRequest request) {
        Cenario cenario = encontrarCenario(request.cenarioId());
        return MitoFatoResponse.from(repository.saveAndFlush(new MitoFato(cenario, request)));
    }

    @Transactional
    public MitoFatoResponse atualizar(Long id, MitoFatoRequest request) {
        MitoFato mitoFato = encontrar(id);
        mitoFato.atualizar(encontrarCenario(request.cenarioId()), request);
        return MitoFatoResponse.from(repository.saveAndFlush(mitoFato));
    }

    @Transactional
    public void excluir(Long id) {
        repository.delete(encontrar(id));
        repository.flush();
    }

    private MitoFato encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Mito ou Fato não encontrado."));
    }

    private Cenario encontrarCenario(Long id) {
        return cenarios.findById(id).orElseThrow(() -> new NotFoundException("Cenário não encontrado."));
    }
}
