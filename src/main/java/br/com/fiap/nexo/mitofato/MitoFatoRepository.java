package br.com.fiap.nexo.mitofato;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MitoFatoRepository extends JpaRepository<MitoFato, Long> {
    Page<MitoFato> findByCenarioId(Long cenarioId, Pageable pageable);
    boolean existsByCenarioId(Long cenarioId);
}
