package org.mercosur.fondoPrevision.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mercosur.fondoPrevision.dto.CapitalesForDisplay;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Gplantahist;
import org.mercosur.fondoPrevision.entities.Movimientos;
import org.mercosur.fondoPrevision.entities.MovimientosHist;
import org.mercosur.fondoPrevision.entities.ResultadoDistribucion;
import org.mercosur.fondoPrevision.entities.Saldos;
import org.mercosur.fondoPrevision.entities.SaldosHistoria;
import org.mercosur.fondoPrevision.entities.TipoMovimiento;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
import org.mercosur.fondoPrevision.repository.GplantahistRepository;
import org.mercosur.fondoPrevision.repository.MovimientosHistRepository;
import org.mercosur.fondoPrevision.repository.MovimientosRepository;
import org.mercosur.fondoPrevision.repository.PrestamoRepository;
import org.mercosur.fondoPrevision.repository.PrestamohistRepository;
import org.mercosur.fondoPrevision.repository.ResultadoDistribucionRepository;
import org.mercosur.fondoPrevision.repository.SaldosHistoriaRepository;
import org.mercosur.fondoPrevision.repository.SaldosRepository;
import org.mercosur.fondoPrevision.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsultaCapitalesServiceImpl implements ConsultaCapitalesService{
	@Autowired
	SaldosHistoriaRepository saldosHistoriaRepository;
	
	@Autowired
	MovimientosHistRepository movimientosHistRepository;
	
	@Autowired
	MovimientosRepository movimientosRepository;
	
	@Autowired
	TipoMovimientoRepository tipoMovimientoRepository;
	
	@Autowired
	LogfondoService logfondoService;
	
	@Autowired
	PrestamohistRepository prestamoHistRepository;
	
	@Autowired
	PrestamoRepository prestamoRepository;

	@Autowired
	GplantaRepository gplantaRepository;
	
	@Autowired
	GplantahistRepository gplantahistRepository;

	@Autowired
	ResultadoDistribucionRepository resultadoDistribucionRepository;
	
	@Autowired
	SaldosRepository saldosRepository;
	
	@Override
	public List<CapitalesForDisplay> getCapitalesByMesliquidacion(String mesliquidacion) throws Exception {

		List<Gplanta> lstFuncionarios = gplantaRepository.findAll();
		CapitalesForDisplay cfd;
		List<CapitalesForDisplay> lstcfd = new ArrayList<CapitalesForDisplay>();
		Boolean conDistrib = false;
		List<ResultadoDistribucion> lstDistrib = resultadoDistribucionRepository.getByMesDistrib(mesliquidacion);
		if(!lstDistrib.isEmpty()) {
			conDistrib = true;
		}
		
		for(Gplanta f : lstFuncionarios) {
			
			Boolean ingresoposterior = false;
			
			cfd = new CapitalesForDisplay();
			cfd.setNombre(f.getNombre());
			cfd.setNrofuncionario(f.getTarjeta());
			cfd.setMesliquidacion(mesliquidacion);
			cfd.setOtros(BigDecimal.ZERO);
			cfd.setRetiros(BigDecimal.ZERO);
		
			BigDecimal capdispante = BigDecimal.ZERO;
			BigDecimal capdispactual = BigDecimal.ZERO;
			BigDecimal capintegactual = BigDecimal.ZERO;
			BigDecimal importeDistrib = BigDecimal.ZERO;
			
			if(conDistrib) {
				Optional<ResultadoDistribucion> res = lstDistrib.stream().filter(rd -> f.getTarjeta().equals(rd.getTarjeta()))
						.findFirst();
				if(res.isPresent()) {
					importeDistrib = res.get().getDistribucionFuncionario();
				}
			}
			cfd.setImporteDistribucion(importeDistrib);
			
			List<SaldosHistoria> lstSaldos = saldosHistoriaRepository.getByTarjetaAndMesliquidacion(f.getTarjeta(), mesliquidacion);
			if(!lstSaldos.isEmpty()) {
				int index = lstSaldos.size() - 1;

				capdispante = lstSaldos.get(0).getCapitalDispAntes();
				capdispactual = lstSaldos.get(index).getCapitalDispActual();
				capintegactual = lstSaldos.get(index).getCapitalIntegActual();				
			}
			else {
				SaldosHistoria saldos = saldosHistoriaRepository.getUltimoByTarjeta(f.getTarjeta());
				if(Integer.valueOf(saldos.getMesliquidacion()) <= Integer.valueOf(mesliquidacion)) {
					capdispante = saldos.getCapitalDispAntes();
					capdispactual = saldos.getCapitalDispActual();
					capintegactual = saldos.getCapitalIntegActual();					
				}
				else {
					ingresoposterior = true;
				}
			}
			
			List<Movimientos> lstMovs = movimientosRepository.getByFuncAndPeriodo(f.getIdgplanta(), mesliquidacion, mesliquidacion);				
			BigDecimal cancelaciones = BigDecimal.ZERO;
			BigDecimal prstnuevos = BigDecimal.ZERO;
			BigDecimal amortiza = BigDecimal.ZERO;
			BigDecimal aportes = BigDecimal.ZERO;
			BigDecimal otros = BigDecimal.ZERO;
			for(Movimientos m : lstMovs) {
				switch (m.getCodigoMovimiento()) {
				case (short) 1:{
					aportes = aportes.add(m.getImporteMov());
					break;
				}
				case (short) 2:{
					aportes = aportes.add(m.getImporteMov());
					break;
				}
				case (short) 3:{
					prstnuevos = prstnuevos.add(m.getImporteMov());
					break;
				}
				case (short) 4:{
					amortiza = amortiza.add(m.getImporteCapSec());
					break;
				}
				case (short) 5:{
					cancelaciones = cancelaciones.add(m.getImporteMov());
					break;
				}
				case (short) 9:{
					otros = otros.add(m.getImporteMov());
					break;
				}
				}
			}	
			BigDecimal totmovprst = amortiza.add(cancelaciones).subtract(prstnuevos);
			
			if(!ingresoposterior) {
				cfd.setCapitalDispAnterior(capdispante);
				cfd.setAmortizacion(amortiza);
				cfd.setCancelaciones(cancelaciones);
				cfd.setPrstnuevos(prstnuevos);
				cfd.setTotalMovPrst(totmovprst);
				cfd.setTotalMovAportes(aportes);
				cfd.setOtros(otros);
				cfd.setCapitalDispActual(capdispactual);
				cfd.setCapitalIntegActual(capintegactual);
				
				lstcfd.add(cfd);				
			}
		}
		lstcfd = agregarEgresos(lstcfd, mesliquidacion, conDistrib);
		return lstcfd;
	}

	private List<CapitalesForDisplay> agregarEgresos(List<CapitalesForDisplay> lstcfd, String mesliquidacion, Boolean conDistrib) 
		throws Exception {
		CapitalesForDisplay cfd;
		List<Integer> tarjetasEgresos = saldosHistoriaRepository.getTarjetasEgresosByMesliquidacion(mesliquidacion);
		List<ResultadoDistribucion> lstDistrib = resultadoDistribucionRepository.getByMesDistrib(mesliquidacion);
		
		for(Integer t : tarjetasEgresos) {
			
			Gplantahist f = gplantahistRepository.getByTarjeta(t);
			cfd = new CapitalesForDisplay();
			cfd.setNombre(f.getNombre());
			cfd.setNrofuncionario(f.getTarjeta());
			cfd.setMesliquidacion(mesliquidacion);
		
			BigDecimal capdispante = BigDecimal.ZERO;
			BigDecimal importeDistrib = BigDecimal.ZERO;
			BigDecimal capdispactual = BigDecimal.ZERO;
			BigDecimal capintegrado = BigDecimal.ZERO;
			
			if(conDistrib) {
				Optional<ResultadoDistribucion> res = lstDistrib.stream().filter(rd -> f.getTarjeta().equals(rd.getTarjeta()))
						.findFirst();
				if(res.isPresent()) {
					importeDistrib = res.get().getDistribucionFuncionario();
				}
			}
			cfd.setImporteDistribucion(importeDistrib);
			
			List<SaldosHistoria> lstSaldos = saldosHistoriaRepository.getByTarjetaAndMesliquidacion(f.getTarjeta(), mesliquidacion);
			if(!lstSaldos.isEmpty()) {
				capdispante = lstSaldos.get(0).getCapitalDispAntes();
			}
			else {
				SaldosHistoria saldos = saldosHistoriaRepository.getUltimoByTarjeta(f.getTarjeta());
				capdispante = saldos.getCapitalDispAntes();
			}
			
				// agregado 11-03-2022 para obtener los capitales actuales en caso de la consulta sea
				// para un mes anterior al mes en que egresó el funcionario.
			SaldosHistoria sh = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(f.getTarjeta(), mesliquidacion);
			capdispactual = sh.getCapitalDispActual();
			capintegrado = sh.getCapitalIntegActual();
				// fin agregado.
			
			List<MovimientosHist> lstMovs = movimientosHistRepository.getByFuncAndMesliquidacion(t, mesliquidacion);				
			BigDecimal cancelaciones = BigDecimal.ZERO;
			BigDecimal prstnuevos = BigDecimal.ZERO;
			BigDecimal amortiza = BigDecimal.ZERO;
			BigDecimal aportes = BigDecimal.ZERO;
			BigDecimal otros = BigDecimal.ZERO;
			BigDecimal retiros = BigDecimal.ZERO;
			
			for(MovimientosHist m : lstMovs) {
				switch (m.getCodigoMovimiento()) {
				case (short) 2:{
					aportes = aportes.add(m.getImporteMov());
					break;
				}
				case (short) 3:{
					prstnuevos = prstnuevos.add(m.getImporteMov());
					break;
				}
				case (short) 4:{
					amortiza = amortiza.add(m.getImporteCapSec());
					break;
				}
				case (short) 5:{
					cancelaciones = cancelaciones.add(m.getImporteMov());
					break;
				}
				case (short) 6:{
					otros = otros.add(m.getImporteMov());
					break;
				}
				case (short) 7:{
					retiros = retiros.add(m.getImporteMov());
					break;
				}
				case (short) 9:{
					otros = otros.add(m.getImporteMov());
					break;
				}
				case (short) 11:{
					otros = otros.add(m.getImporteMov());
					break;
				}
				}
			}	
			BigDecimal totmovprst = amortiza.add(cancelaciones).subtract(prstnuevos);
			
			cfd.setCapitalDispAnterior(capdispante);
			cfd.setAmortizacion(amortiza);
			cfd.setCancelaciones(cancelaciones);
			cfd.setPrstnuevos(prstnuevos);
			cfd.setTotalMovPrst(totmovprst);
			cfd.setTotalMovAportes(aportes);
			cfd.setOtros(otros);
			cfd.setRetiros(retiros);
			if(retiros.compareTo(BigDecimal.ZERO) > 0) {
				// quiere decir que la consulta corresponde al mes en que se cerró la cta.
				cfd.setCapitalDispActual(BigDecimal.ZERO);
				cfd.setCapitalIntegActual(BigDecimal.ZERO);				
			}
			else {
				cfd.setCapitalDispActual(capdispactual);
				cfd.setCapitalIntegActual(capintegrado);
			}
			
			lstcfd.add(cfd);
		}
		return lstcfd;
	}
	
	@Override
	public List<CapitalesForDisplay> realizarAjuste(CapitalesForDisplay capfordisplay) throws Exception {
		
		Integer tarjeta = capfordisplay.getNrofuncionario();
		String mesliquidacion = capfordisplay.getMesliquidacion();
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		
		Gplanta funcionario = gplantaRepository.findByTarjeta(tarjeta);

		Optional<Saldos> saldos = saldosRepository.findByTarjeta(tarjeta);
		if(saldos.isPresent()) {
			if(saldos.get().getMesliquidacion().equals(mesliquidacion)) {
				Saldos saldosnew = saldos.get();
				saldosnew.setCapitalDispActual(capfordisplay.getCapitalDispActual());
				saldosnew.setCapitalIntegActual(capfordisplay.getCapitalIntegActual());
				saldosnew.setNumerales(capfordisplay.getCapitalIntegActual());
				saldosnew = saldosRepository.save(saldosnew);				
			}
			else {
				throw new Exception("El mesLiquidación ingresado no coincide con el de los saldos");
			}
		}
		
		SaldosHistoria saldoshist = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(tarjeta, mesliquidacion);
		
		SaldosHistoria newsh = new SaldosHistoria();
		newsh.setCapitalDispActual(capfordisplay.getCapitalDispActual());
		newsh.setCapitalDispAntes(saldoshist.getCapitalDispActual());
		newsh.setCapitalIntegActual(capfordisplay.getCapitalIntegActual());
		newsh.setCapitalIntegAntes(saldoshist.getCapitalIntegActual());
		newsh.setFecha(currentDate);
		newsh.setGplanta_id(funcionario.getIdgplanta());
		newsh.setMesliquidacion(mesliquidacion);
		newsh.setMotivo("Ajuste Contable");
		newsh.setNumerales(capfordisplay.getCapitalIntegActual());
		newsh.setTarjeta(tarjeta);
				
		saldosHistoriaRepository.save(newsh);
		
		Movimientos mov = movimientosRepository.getLastByFuncAndMesliquidacion(tarjeta, mesliquidacion);
		BigDecimal saldoantes = mov.getSaldoActual();
		BigDecimal diferencia = BigDecimal.ZERO;
		diferencia = capfordisplay.getCapitalDispActual().subtract(saldoantes);
		
		TipoMovimiento tipmov = tipoMovimientoRepository.findByCodigoMovimiento((short)10);
		Movimientos newmov = new Movimientos();
		newmov.setCodigoMovimiento(tipmov.getCodigoMovimiento());
		newmov.setFechaMovimiento(currentDate);
		newmov.setFuncionario(funcionario);
		newmov.setImporteCapSec(BigDecimal.ZERO);
		newmov.setImporteIntFunc(BigDecimal.ZERO);
		newmov.setImporteMov(diferencia);
		newmov.setMesliquidacion(mesliquidacion);
		newmov.setNroCuota(0);
		newmov.setNroPrestamo(0);
		newmov.setObservaciones("Ajuste Contable");
		newmov.setSaldoActual(capfordisplay.getCapitalDispActual());
		newmov.setSaldoAnterior(saldoantes);
		newmov.setTarjeta(tarjeta);
		newmov.setTipoMovimiento(tipmov);
		movimientosRepository.save(newmov);
			
		logfondoService.agregarLog("Ajustes", "Ajuste de Capitales con Distribución. Funcionario: " + tarjeta.toString());
		return getCapitalesByMesliquidacion(mesliquidacion);
	}

}
