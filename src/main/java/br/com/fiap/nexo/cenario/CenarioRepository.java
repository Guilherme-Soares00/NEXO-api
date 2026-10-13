package br.com.fiap.nexo.cenario;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CenarioRepository extends JpaRepository<Cenario, Long> {
    Page<Cenario> findByTituloContainingIgnoreCase(String texto, Pageable pageable);
}
