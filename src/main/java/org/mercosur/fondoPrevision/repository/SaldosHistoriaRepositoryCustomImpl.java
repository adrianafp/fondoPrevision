package org.mercosur.fondoPrevision.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.mercosur.fondoPrevision.entities.SaldosHistoria;

@Repository
@Transactional
public class SaldosHistoriaRepositoryCustomImpl implements SaldosHistoriaRepositoryCustom {

	@PersistenceContext
	EntityManager entityManager;


	@Override
	public Optional<String> getMaxMesLiquidacion() {
		Optional<String> mesLiq = Optional.ofNullable((String)entityManager.createQuery("select max(sh.mesliquidacion) from " +
				"SaldosHistoria sh").getSingleResult());
		return mesLiq;
	}


	@SuppressWarnings("unchecked")
	@Override
	public Iterable<SaldosHistoria> getSaldosHisByTarjeta(Integer tarjeta) {
		
		return entityManager.createQuery("select sh from SaldosHistoria sh where sh.tarjeta =:tarjeta")
				.setParameter("tarjeta", tarjeta).getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<SaldosHistoria> getByTarjetaAndMesliquidacion(Integer tarjeta, String mesliquidacion) {
		List<SaldosHistoria> lstsaldosh =  entityManager.createQuery("select sh from SaldosHistoria sh " + 
					"where sh.tarjeta =:t and sh.mesliquidacion =:mes order by sh.idfsaldoshistoria")
					.setParameter("t", tarjeta)
					.setParameter("mes", mesliquidacion)
					.getResultList();
		return lstsaldosh;
	}


	@Override
	public SaldosHistoria getByTarjetaAndMesDistribucion(Integer tarjeta, String mesliquidacion) {
		SaldosHistoria saldosh =  (SaldosHistoria) entityManager.createQuery("select sh from SaldosHistoria sh " + 
				"where sh.tarjeta =:t and sh.mesliquidacion =:mes and sh.motivo like '%Distribución de utilidades%'")
				.setParameter("t", tarjeta)
				.setParameter("mes", mesliquidacion)
				.getSingleResult();

		return saldosh;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SaldosHistoria getUltimoByTarjeta(Integer tarjeta) throws Exception {
		List<SaldosHistoria> lstsaldosh =  entityManager.createQuery("select sh from SaldosHistoria sh " + 
				"where sh.tarjeta =:t order by sh.idfsaldoshistoria desc")
				.setParameter("t", tarjeta)
				.getResultList();
	
	return lstsaldosh.get(0);
	}

	@Override
	public void deleteByMesliquidacion(String mesliquidacion) throws Exception {
		entityManager.createQuery("delete from SaldosHistoria sh where sh.mesliquidacion =:mes")
			.setParameter("mes", mesliquidacion).executeUpdate();
	}
	
	@Override
	public BigDecimal numeralesFuncionario(String anioMes2,
			Integer tarjeta) throws Exception {
					
		BigDecimal numerales = (BigDecimal) entityManager.createNativeQuery("select sh.numerales from fsaldoshistoria sh where " + 
				"sh.idfsaldoshistoria IN (select max(fsh.idfsaldoshistoria) from fsaldoshistoria fsh where " + 
				"fsh.mesliquidacion =:ml and fsh.tarjeta =:tarjeta group by fsh.tarjeta)")
				.setParameter("ml", anioMes2)
				.setParameter("tarjeta", tarjeta)
				.getSingleResult();

		return numerales;
	}

	@SuppressWarnings("unchecked")
	public BigDecimal totalNumeralesConDistribucion(String aniomes) throws Exception {
		List<BigDecimal> valores = entityManager.createQuery("select sh.numerales from " +
				"SaldosHistoria sh where sh.mesliquidacion = :aniomes and sh.motivo like '%Distribución de utilidades%'")
				.setParameter("aniomes", aniomes)
				.getResultList();

		return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	}


	@SuppressWarnings("unchecked")
	@Override
	public BigDecimal totalNumeralesPorTarjetasConDistribucion(String aniomes, String tarjetas) throws Exception {
		List<BigDecimal> valores = entityManager.createQuery("select sh.numerales " +
				"from SaldosHistoria sh where sh.mesliquidacion = :aniomes and " +
				"sh.tarjeta NOT IN (" + tarjetas +") and sh.motivo like '%Distribución de utilidades%'")
				.setParameter("aniomes", aniomes)
				.getResultList();
		return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	}


	@Override
	public BigDecimal totalNumeralesPorTajetasSinDistribucion(String aniomes, String tarjetas) throws Exception {
/*		List<BigDecimal> valores = entityManager.createNativeQuery("select MAX(x.numerales) from (select * from fsaldoshistoria sh " + 
				"where sh.mesliquidacion =:ml and sh.tarjeta NOT IN (" + tarjetas + ") order by sh.idFSaldosHistoria desc) as x " + 
				"group by tarjeta")
				.setParameter("ml", aniomes)
				.getResultList();  
		return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);*/

		BigDecimal sumaNumerales = (BigDecimal) entityManager.createNativeQuery("select SUM(x.numerales) from " + 
				"(select sh.idfsaldoshistoria, sh.tarjeta, sh.numerales from fsaldoshistoria sh where " + 
				"sh.idfsaldoshistoria IN (select max(fsh.idfsaldoshistoria) from fsaldoshistoria fsh where " + 
				"fsh.mesliquidacion =:ml and fsh.tarjeta NOT IN (" + tarjetas + ") group by fsh.tarjeta)) as x")
				.setParameter("ml", aniomes)
				.getSingleResult();
		
		return sumaNumerales;
	}


	@Override
	public BigDecimal totalNumeralesSinDistribucion(String aniomes) throws Exception {
		/*List<BigDecimal> valores = entityManager.createNativeQuery("select MAX(x.numerales) from (select * from fsaldoshistoria sh " + 
				"where sh.mesliquidacion =:ml order by sh.idFSaldosHistoria desc) as x group by tarjeta")
				.setParameter("ml", aniomes)
				.getResultList();
		return valores.stream().reduce(BigDecimal.ZERO, BigDecimal::add);*/
		
		BigDecimal sumaNumerales = (BigDecimal) entityManager.createNativeQuery("select SUM(x.numerales) from " + 
				"(select sh.idfsaldoshistoria, sh.tarjeta, sh.numerales from fsaldoshistoria sh where " + 
				"sh.idfsaldoshistoria IN (select max(fsh.idfsaldoshistoria) from fsaldoshistoria fsh where " + 
				"fsh.mesliquidacion =:ml  and fsh.motivo not like '%cierre de Cuenta' group by fsh.tarjeta)) as x")
				.setParameter("ml", aniomes)
				.getSingleResult();
		
		return sumaNumerales;

	}


	@SuppressWarnings("unchecked")
	@Override
	public SaldosHistoria getLastByTarjetaAndMesliquidacion(Integer tarjeta, String mesliquidacion) {
		List<SaldosHistoria> lstsaldosh =  entityManager.createQuery("select sh from SaldosHistoria sh " + 
				"where sh.tarjeta =:t and sh.mesliquidacion =:mes order by sh.idfsaldoshistoria DESC")
				.setParameter("t", tarjeta)
				.setParameter("mes", mesliquidacion)
				.getResultList();
		return lstsaldosh.get(0);
	}


	@SuppressWarnings("unchecked")
	@Override
	public List<Integer> getTarjetasEgresosByMesliquidacion(String mesliquidacion) throws Exception {
		
		return entityManager.createQuery("select distinct sh.tarjeta from SaldosHistoria sh where sh.mesliquidacion =:mes " +
				"and sh.tarjeta IN (select distinct gph.tarjeta from org.mercosur.fondoPrevision.entities.Gplantahist gph)")
				.setParameter("mes", mesliquidacion)
				.getResultList();
	}


	@Override
	public void deleteByTarjeta(Integer tarjeta) throws Exception {
		entityManager.createQuery("delete from SaldosHistoria sh where sh.tarjeta =:tarjeta").setParameter("tarjeta", tarjeta)
		.executeUpdate();
		
	}

}
