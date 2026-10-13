package br.com.fiap.nexo.alternativa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlternativaRepository extends JpaRepository<Alternativa, Long> {
    Page<Alternativa> findByCenarioId(Long cenarioId, Pageable pageable);
}
