package org.mercosur.fondoPrevision.repository;

public interface GplantahistRepositoryCustom {

	public void insertByTarjeta(Integer tarjeta) throws Exception;
	
	public Integer getLastTarjeta() throws Exception;

}
