package org.mercosur.fondoPrevision.repository;

import org.mercosur.fondoPrevision.entities.Gcargo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GcargoRepository extends JpaRepository<Gcargo, Integer>, GcargoRepositoryCustom {

}
