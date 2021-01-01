package org.mercosur.fondoPrevision.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mercosur.fondoPrevision.dto.CapitalesForDisplay;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Movimientos;
import org.mercosur.fondoPrevision.entities.ResultadoDistribucion;
import org.mercosur.fondoPrevision.entities.Saldos;
import org.mercosur.fondoPrevision.entities.SaldosHistoria;
import org.mercosur.fondoPrevision.entities.TipoMovimiento;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
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
			
			cfd = new CapitalesForDisplay();
			cfd.setNombre(f.getNombre());
			cfd.setNrofuncionario(f.getTarjeta());
			cfd.setMesliquidacion(mesliquidacion);
		
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
				capdispante = saldos.getCapitalDispAntes();
				capdispactual = saldos.getCapitalDispActual();
				capintegactual = saldos.getCapitalIntegActual();
			}
			
			List<Movimientos> lstMovs = movimientosRepository.getByFuncAndPeriodo(f.getIdgplanta(), mesliquidacion, mesliquidacion);				
			BigDecimal cancelaciones = BigDecimal.ZERO;
			BigDecimal prstnuevos = BigDecimal.ZERO;
			BigDecimal amortiza = BigDecimal.ZERO;
			BigDecimal aportes = BigDecimal.ZERO;
			for(Movimientos m : lstMovs) {
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
				case (short) 9:{
					aportes = aportes.add(m.getImporteMov());
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
			cfd.setCapitalDispActual(capdispactual);
			cfd.setCapitalIntegActual(capintegactual);
			
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
