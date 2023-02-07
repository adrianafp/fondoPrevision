package org.mercosur.fondoPrevision.repository;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class GplantahistRepositoryCustomImpl implements GplantahistRepositoryCustom{

	@PersistenceContext
	EntityManager entityManager;
	
	@Override
	public void insertByTarjeta(Integer tarjeta) throws Exception {
		entityManager.createQuery("insert into Gplantahist (documento, egreso, email, gcargos_id, ingreso, nacionalidad, " +
				"nombre, sector, sexo, tarjeta, ultimoIngreso, unidad) " +
				"select gp.documento, gp.egreso, gp.email, gp.gcargo.idgcargo, gp.ingreso, gp.nacionalidad, gp.nombre, " +
				"gp.sector, gp.sexo, gp.tarjeta, gp.ultimoIngreso, gp.unidad from Gplanta gp where gp.tarjeta =:tarjeta")
		.setParameter("tarjeta", tarjeta)
		.executeUpdate();
	}

	@Override
	public Integer getLastTarjeta() throws Exception {
		return (Integer) entityManager.createQuery("select Max(f.tarjeta) from Gplantahist f").getSingleResult();
	}

}
