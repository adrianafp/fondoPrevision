package org.mercosur.fondoPrevision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.mercosur.fondoPrevision.entities.DatosDistribucion;

@Repository
public interface DatosDistribucionRepository extends JpaRepository<DatosDistribucion, Long>, DatosDistribucionRepositoryCustom {

}
