package org.mercosur.fondoPrevision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.mercosur.fondoPrevision.entities.SaldosUltimoMes;

@Repository
public interface SaldosUltimoMesRepository extends JpaRepository<SaldosUltimoMes, Long> {

}
