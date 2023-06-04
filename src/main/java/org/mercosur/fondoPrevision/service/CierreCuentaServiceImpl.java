package org.mercosur.fondoPrevision.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.mercosur.fondoPrevision.dto.CierreCtaForm;
import org.mercosur.fondoPrevision.dto.EstadoDeCtaCierre;
import org.mercosur.fondoPrevision.entities.DatosCierreCta;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Movimientos;
import org.mercosur.fondoPrevision.entities.Parametro;
import org.mercosur.fondoPrevision.entities.Prestamo;
import org.mercosur.fondoPrevision.entities.SaldoPrestamosAcum;
import org.mercosur.fondoPrevision.entities.Saldos;
import org.mercosur.fondoPrevision.entities.SaldosHistoria;
import org.mercosur.fondoPrevision.entities.SolicitudPrestamo;
import org.mercosur.fondoPrevision.entities.SueldoMes;
import org.mercosur.fondoPrevision.entities.TipoMovimiento;
import org.mercosur.fondoPrevision.entities.User;
import org.mercosur.fondoPrevision.repository.DatosCierreCtaRepository;
import org.mercosur.fondoPrevision.repository.DatosDistribucionRepository;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
import org.mercosur.fondoPrevision.repository.GplantahistRepository;
import org.mercosur.fondoPrevision.repository.GvinculoFuncionarioCargoRepository;
import org.mercosur.fondoPrevision.repository.MovimientosHistRepository;
import org.mercosur.fondoPrevision.repository.MovimientosRepository;
import org.mercosur.fondoPrevision.repository.ParametroRepository;
import org.mercosur.fondoPrevision.repository.PrestamoRepository;
import org.mercosur.fondoPrevision.repository.PrestamohistRepository;
import org.mercosur.fondoPrevision.repository.ResultDistribHistRepository;
import org.mercosur.fondoPrevision.repository.ResultadoDistribucionRepository;
import org.mercosur.fondoPrevision.repository.SaldoPrestamosAcumRepository;
import org.mercosur.fondoPrevision.repository.SaldosHistoriaRepository;
import org.mercosur.fondoPrevision.repository.SaldosRepository;
import org.mercosur.fondoPrevision.repository.SolicitudPrestamoRepository;
import org.mercosur.fondoPrevision.repository.SueldoMesHistRepository;
import org.mercosur.fondoPrevision.repository.SueldoMesRepository;
import org.mercosur.fondoPrevision.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CierreCuentaServiceImpl implements CierreCuentaService{

	@Autowired
	GplantaRepository gplantaRepository;
	
	@Autowired
	GvinculoFuncionarioCargoRepository gvinculoRepository;
	
	@Autowired
	SaldosHistoriaRepository saldosHistoriaRepository;
	
	@Autowired
	SaldosRepository saldosRepository;
	
	@Autowired
	SueldoMesRepository sueldoMesRepository;
	
	@Autowired
	SueldoMesHistRepository sueldoMesHistRepository;
	
	@Autowired
	DatosDistribucionRepository datosDistribRepository;
	
	@Autowired
	ResultadoDistribucionRepository resultadoDistribRepository;
	
	@Autowired
	ResultDistribHistRepository resultDistribHistRepository;
	
	@Autowired
	ParametroRepository parametroRepository;
	
	@Autowired
	PrestamoRepository prestamoRepository;
	
	@Autowired
	PrestamohistRepository prestamoshistRepository;
	
	@Autowired
	SaldoPrestamosAcumRepository saldoPrestamosRepository;
	
	@Autowired
	SolicitudPrestamoRepository solicitudPrstRepository;
	
	@Autowired
	TipoMovimientoRepository tipomovRepository;
	
	@Autowired
	MovimientosRepository movimientosRepository;
	
	@Autowired
	MovimientosHistRepository movhistRepository;
	
	@Autowired
	GplantahistRepository gplantahistRepository;
	
	@Autowired
	UserService userService;
	
	@Autowired
	DatosCierreCtaRepository datoscierreRepository;
	
	@Override
	public EstadoDeCtaCierre cerrarCuenta(CierreCtaForm form) throws Exception {
		Gplanta funcionario = gplantaRepository.getOne(form.getIdfuncionario());
		EstadoDeCtaCierre estadoResult = new EstadoDeCtaCierre();

		Optional<Saldos> saldos = saldosRepository.findByTarjeta(funcionario.getTarjeta());
		BigDecimal capitalIntegActual = BigDecimal.ZERO;
		BigDecimal capitalIntegFinal = BigDecimal.ZERO;
		if(saldos.isPresent()) {
			capitalIntegActual = saldos.get().getCapitalIntegActual();
			capitalIntegFinal = saldos.get().getCapitalIntegActual();
		}

		LocalDate fegreso = LocalDate.now();
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		fegreso = LocalDate.parse(form.getFechaEgreso(), dtf);
		String mesActual = fegreso.format(dtf);
		String mesLiqActual = mesActual.substring(0, 4).concat(mesActual.substring(5,7));
		
		Movimientos mov = movimientosRepository.getLastByFuncAndTipo(funcionario.getTarjeta(), 2);
		
		LocalDate mesAnterior = fegreso.minusMonths(1);
		String mesAntes = mesAnterior.format(dtf);
		String mesliquidacion = mesAntes.substring(0, 4).concat(mesAntes.substring(5, 7));		
		
		estadoResult.setFechaEgreso(fegreso);
		estadoResult.setFuncionario(funcionario);
		estadoResult.setCapitalIntegrado(capitalIntegActual);
		estadoResult.setSueldoUltimoMes(BigDecimal.ZERO);
		estadoResult.setImporteAguinaldo(BigDecimal.ZERO);
		estadoResult.setImporteLicencia(BigDecimal.ZERO);
		
		BigDecimal aporteTot = BigDecimal.ZERO;
		BigDecimal aporteAguinaldo = BigDecimal.ZERO;
		BigDecimal aporteLicencia = BigDecimal.ZERO;

		Iterable<Parametro> lstPar = parametroRepository.getSomeByDesc("Aporte");
		
		if(Integer.valueOf(mesLiqActual) > Integer.valueOf(mov.getMesliquidacion())) {
			aporteTot = aporteHaberes(fegreso, funcionario, lstPar, estadoResult);			
		}
		
		Movimientos movagui = movimientosRepository.getLastByFuncAndTipo(funcionario.getTarjeta(), 9);
		
		if(Integer.valueOf(mesLiqActual) > Integer.valueOf(movagui.getMesliquidacion())) {
			aporteAguinaldo = aporteAguinaldo(fegreso, funcionario, lstPar, estadoResult);			
		}
	
		if(form.getLicenciaCompleta() != null) {
			aporteLicencia = aporteLicencia(form.getLicenciaSinComp(), form.getLicenciaCompleta(), lstPar, estadoResult);
			estadoResult.setImporteLicencia(form.getLicenciaSinComp());
		}
		
		capitalIntegFinal = capitalIntegFinal.add(aporteTot);
		estadoResult.setAporteTotal(aporteTot);		

		capitalIntegFinal = capitalIntegFinal.add(aporteAguinaldo);
		estadoResult.setAporteTotalSobreAguinaldo(aporteAguinaldo);

		capitalIntegFinal = capitalIntegFinal.add(aporteLicencia);
		estadoResult.setAporteTotLicencia(aporteLicencia);

			// cálculo de intereses por distribución
		BigDecimal resultEjerc = form.getResultEjercicio();
		BigDecimal bdSuma = saldosHistoriaRepository.totalNumeralesSinDistribucion(mesliquidacion);
		BigDecimal bdNumerales = saldosHistoriaRepository.numeralesFuncionario(mesliquidacion, funcionario.getTarjeta());
		BigDecimal factor = bdNumerales.divide(bdSuma, 6, RoundingMode.HALF_DOWN);
		BigDecimal bdLeToca = factor.multiply(resultEjerc).setScale(2, RoundingMode.HALF_DOWN); 
				
		capitalIntegFinal = capitalIntegFinal.add(bdLeToca);
		estadoResult.setInteresesporcolocaciones(bdLeToca);
		
		List<Prestamo> lstPrst = prestamoRepository.findByFuncionario(form.getIdfuncionario());
		if(!lstPrst.isEmpty()) {
			Optional<SaldoPrestamosAcum> saldoAcum = saldoPrestamosRepository.findByTarjeta(funcionario.getTarjeta());
			if(saldoAcum.isPresent()) {
				capitalIntegFinal = capitalIntegFinal.subtract(saldoAcum.get().getSaldoPrestAcumulado());
				estadoResult.setSaldoPrestamos(saldoAcum.get().getSaldoPrestAcumulado());
			}
			else {
				estadoResult.setSaldoPrestamos(BigDecimal.ZERO);
			}
		}
		else {
			estadoResult.setSaldoPrestamos(BigDecimal.ZERO);
		}
		estadoResult.setLstPrst(lstPrst);
		estadoResult.setSaldoCuenta(capitalIntegFinal);
		return estadoResult;
	}

	private BigDecimal aporteLicencia(BigDecimal licenciaSinComp, BigDecimal licenciaCompleta, Iterable<Parametro> lstPar, EstadoDeCtaCierre estadoResult) {
		BigDecimal aportePatronal = BigDecimal.ZERO;
		BigDecimal aporteFun = BigDecimal.ZERO;
		BigDecimal aporteTot = BigDecimal.ZERO;
		BigDecimal divisor = BigDecimal.valueOf((long) 100);
		
		for(Parametro p:lstPar) {
			if(p.getDescripcion().contains("funcionario") || p.getDescripcion().contains("Funcionario")) {
				aporteFun = licenciaSinComp.multiply(p.getValor()).divide(divisor).setScale(2, RoundingMode.HALF_UP);
				estadoResult.setAporteFunLicencia(aporteFun);
			}
			else if(p.getDescripcion().contains("patronal")) {
				aportePatronal = (licenciaCompleta.multiply(p.getValor()).divide(divisor)).setScale(2, RoundingMode.HALF_UP);
				estadoResult.setAportePatLicencia(aportePatronal);
			}
		}

		aporteTot = aportePatronal.add(aporteFun);
		return aporteTot;
	}
	
	private BigDecimal aporteHaberes(LocalDate fegreso, Gplanta func, Iterable<Parametro> lstPar, EstadoDeCtaCierre estadoResult) {
	
		BigDecimal aportePatronal = BigDecimal.ZERO;
		BigDecimal aporteFun = BigDecimal.ZERO;
		BigDecimal aporteTot = BigDecimal.ZERO;
		BigDecimal divisor = BigDecimal.valueOf((long) 100);
		BigDecimal basico = func.getGcargo().getBasico();
		BigDecimal complemento = func.getGcargo().getComplemento();
		
		int days = fegreso.getDayOfMonth();
		BigDecimal bddays = new BigDecimal(String.valueOf(days));
		
		//Corrección de marzo/2023 - Para calcular el salario diario se divide el 
		// nominal sobre 30 siempre. */

/*		
		int lastday = fegreso.lengthOfMonth();
		BigDecimal bdlastday = new BigDecimal(String.valueOf(lastday));
*/
		BigDecimal bdtreintaDias = new BigDecimal(String.valueOf(30));
		
		BigDecimal sueldomes = basico.add(complemento);
		sueldomes = sueldomes.divide(bdtreintaDias, 2, RoundingMode.HALF_DOWN);
		sueldomes = sueldomes.multiply(bddays);
		
		
//		****** Corrección marzo 2023 ******
//		sueldomes = sueldomes.divide(bdlastday, 2, RoundingMode.HALF_DOWN);
	
		
		estadoResult.setSueldoUltimoMes(sueldomes);
		estadoResult.setBasico(basico.divide(bdtreintaDias, 2, RoundingMode.HALF_DOWN));
		
		
		// ***** Corrección marxo 2023 *****
//		BigDecimal basicomes = basico.divide(bdlastday, 2, RoundingMode.HALF_DOWN);
//		basicomes = basicomes.multiply(bddays);
	
		BigDecimal basicomes = basico.divide(bdtreintaDias, 2, RoundingMode.HALF_DOWN);
		basicomes = basicomes.multiply(bddays);
		
		for(Parametro p:lstPar) {
			if(p.getDescripcion().contains("funcionario") || p.getDescripcion().contains("Funcionario")) {
				aporteFun = basicomes.multiply(p.getValor()).divide(divisor).setScale(2, RoundingMode.HALF_UP);
				estadoResult.setAporteFun(aporteFun);
			}
			else if(p.getDescripcion().contains("patronal")) {
				aportePatronal = (sueldomes.multiply(p.getValor()).divide(divisor)).setScale(2, RoundingMode.HALF_UP);
				estadoResult.setAportePat(aportePatronal);
			}
		}

		aporteTot = aportePatronal.add(aporteFun);
		return aporteTot;
	}
	
	private BigDecimal aporteAguinaldo(LocalDate fegreso, Gplanta func, Iterable<Parametro> lstPar, EstadoDeCtaCierre estadoResult) throws Exception {
		
		LocalDate mesAnterior = fegreso.minusMonths(1);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String mesAntes = mesAnterior.format(dtf);
		String mesliquidacion = mesAntes.substring(0, 4).concat(mesAntes.substring(5, 7));
		String mesliqinicial;
		if(fegreso.getMonth().compareTo(Month.JUNE) <= 0) {
			mesliqinicial = mesAntes.substring(0, 4).concat("01");
		}
		else {
			mesliqinicial = mesAntes.substring(0, 4).concat("07");
		}
		
		BigDecimal divisor = BigDecimal.valueOf((long) 100);
		BigDecimal aporteFun = BigDecimal.ZERO;
		BigDecimal aportePat = BigDecimal.ZERO;
		BigDecimal aportePatComp = BigDecimal.ZERO;
		BigDecimal aporteAguinaldo = BigDecimal.ZERO;
		BigDecimal divDoce = BigDecimal.valueOf((long)12);
		BigDecimal bdSumabasicos = sumaBasicos(mesliqinicial, mesliquidacion, func.getTarjeta());
		BigDecimal bdSumacomp = sumaComplementos(mesliqinicial, mesliquidacion, func.getTarjeta());
		
		// **** Correción Marzo 2023 ****
		
		BigDecimal bdtreintaDias = new BigDecimal(String.valueOf(30));
		int days = fegreso.getDayOfMonth();
		BigDecimal bddays = new BigDecimal(String.valueOf(days));
		
		BigDecimal basicoactual = func.getGcargo().getBasico();
		basicoactual = basicoactual.divide(bdtreintaDias, 2, RoundingMode.HALF_DOWN);
		basicoactual = basicoactual.multiply(bddays);
		BigDecimal complactual = func.getGcargo().getComplemento();
		complactual = complactual.divide(bdtreintaDias, 2, RoundingMode.HALF_DOWN);
		complactual = complactual.multiply(bddays);
		
		bdSumabasicos = bdSumabasicos.add(basicoactual);
		bdSumabasicos = bdSumabasicos.divide(divDoce, 2, RoundingMode.HALF_DOWN);
		bdSumacomp = bdSumacomp.add(complactual);
		bdSumacomp = bdSumacomp.divide(divDoce, 2, RoundingMode.HALF_DOWN);
		
		estadoResult.setImporteAguinaldo(bdSumabasicos.add(bdSumacomp));
		estadoResult.setSumaBasicos(bdSumabasicos);
		
		for(Parametro p : lstPar) {
			if(p.getDescripcion().contains("funcionario") || p.getDescripcion().contains("Funcionario")) {
				aporteFun = bdSumabasicos.multiply(p.getValor()).divide(divisor, 2, RoundingMode.HALF_DOWN);
			}
			else if(p.getDescripcion().contains("patronal")) {
				aportePat = bdSumabasicos.multiply(p.getValor()).divide(divisor, 2, RoundingMode.HALF_DOWN);
				if(bdSumacomp.compareTo(BigDecimal.ZERO) >= 0) {
					aportePatComp = bdSumacomp.multiply(p.getValor()).divide(divisor, 2, RoundingMode.HALF_DOWN);
					aportePat = aportePat.add(aportePatComp);
				}
			}
		}
		
		estadoResult.setAporteFunAgui(aporteFun);
		estadoResult.setAportePatAgui(aportePat);
		
		aporteAguinaldo = aportePat.add(aporteFun);
		return aporteAguinaldo;
	}
	
	private BigDecimal sumaBasicos(String aniomes1, String aniomes2, Integer tarjeta) throws Exception {
		BigDecimal totbasico = BigDecimal.ZERO;

		List<SueldoMes> lstSueldoMes = sueldoMesRepository.getByPeriodoAndTarjeta(aniomes1, aniomes2, tarjeta);
		for(SueldoMes sm : lstSueldoMes) {
			totbasico = totbasico.add(sm.getSueldomes());
		}
		return totbasico;
	}

	private BigDecimal sumaComplementos(String aniomes1, String aniomes2, Integer tarjeta) throws Exception{
		BigDecimal totcomplemento = BigDecimal.ZERO;

		List<SueldoMes> lstSueldoMes = sueldoMesRepository.getByPeriodoAndTarjeta(aniomes1, aniomes2, tarjeta);
		for(SueldoMes sm : lstSueldoMes) {
			totcomplemento = totcomplemento.add(sm.getComplemento());
		}
		return totcomplemento;		
	}

	@Override
	public Boolean consolidarCierredeCuenta(EstadoDeCtaCierre estadoCta) throws Exception {

		Gplanta funcionario = estadoCta.getFuncionario();
		LocalDate fechaEgreso = estadoCta.getFechaEgreso();
		
		java.sql.Date data = java.sql.Date.valueOf(fechaEgreso);		
		funcionario.setEgreso(data);
		funcionario = gplantaRepository.save(funcionario);
		
		int mes = fechaEgreso.getMonthValue();
		int year = fechaEgreso.getYear();
		String mesliquidacion = String.valueOf(year) + (mes < 10? "0"+String.valueOf(mes):String.valueOf(mes));
		
			// creación de registros de Movimientos de cierre 
		
		Movimientos lastMov = movimientosRepository.getLastByFunc(funcionario.getTarjeta());
			// movimiento tipo 2 _ aportes sobre haberes
		TipoMovimiento tipomov = tipomovRepository.findByCodigoMovimiento((short)2);
		Movimientos mov = new Movimientos();
		mov.setCodigoMovimiento(tipomov.getCodigoMovimiento());
		mov.setFechaMovimiento(new Date());
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(estadoCta.getAportePat());
		mov.setImporteIntFunc(estadoCta.getAporteFun());
		mov.setImporteMov(estadoCta.getAporteTotal());
		mov.setMesliquidacion(mesliquidacion);
		mov.setObservaciones("Cierre de Cuenta");
		mov.setSaldoAnterior(lastMov.getSaldoActual());
		mov.setSaldoActual(lastMov.getSaldoActual().add(estadoCta.getAporteTotal()));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setTipoMovimiento(tipomov);
		
		mov = movimientosRepository.save(mov);
		BigDecimal saldoActual = mov.getSaldoActual();
		
		
			//movimiento tipo 9 - aportes sobre aguinaldo
		
		tipomov = tipomovRepository.findByCodigoMovimiento((short) 9);
		mov = new Movimientos();
		mov.setCodigoMovimiento(tipomov.getCodigoMovimiento());
		mov.setFechaMovimiento(new Date());
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(estadoCta.getAportePatAgui());
		mov.setImporteIntFunc(estadoCta.getAporteFunAgui());
		mov.setImporteMov(estadoCta.getAporteTotalSobreAguinaldo());
		mov.setMesliquidacion(mesliquidacion);
		mov.setObservaciones("Cierre de Cuenta");
		mov.setSaldoAnterior(saldoActual);
		mov.setSaldoActual(saldoActual.add(estadoCta.getAporteTotalSobreAguinaldo()));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setTipoMovimiento(tipomov);
		
		mov = movimientosRepository.save(mov);
		saldoActual = mov.getSaldoActual();

			//movimiento tipo 11 - aportes sobre licencia no gozada
			
		tipomov = tipomovRepository.findByCodigoMovimiento((short) 11);
		mov = new Movimientos();
		mov.setCodigoMovimiento(tipomov.getCodigoMovimiento());
		mov.setFechaMovimiento(new Date());
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(estadoCta.getAportePatLicencia());
		mov.setImporteIntFunc(estadoCta.getAporteFunLicencia());
		mov.setImporteMov(estadoCta.getAporteTotLicencia());
		mov.setMesliquidacion(mesliquidacion);
		mov.setObservaciones("Cierre de Cuenta");
		mov.setSaldoAnterior(saldoActual);
		mov.setSaldoActual(saldoActual.add(estadoCta.getAporteTotLicencia()));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setTipoMovimiento(tipomov);
		
		mov = movimientosRepository.save(mov);
		saldoActual = mov.getSaldoActual();

			// movimiento tipo 6 - distribución de intereses
		
		tipomov = tipomovRepository.findByCodigoMovimiento((short) 6);
		mov = new Movimientos();
		mov.setCodigoMovimiento(tipomov.getCodigoMovimiento());
		mov.setFechaMovimiento(new Date());
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(BigDecimal.ZERO);
		mov.setImporteIntFunc(BigDecimal.ZERO);
		mov.setImporteMov(estadoCta.getInteresesporcolocaciones());
		mov.setMesliquidacion(mesliquidacion);
		mov.setObservaciones("Cierre de Cuenta");
		mov.setSaldoAnterior(saldoActual);
		mov.setSaldoActual(saldoActual.add(estadoCta.getInteresesporcolocaciones()));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setTipoMovimiento(tipomov);
		
		mov = movimientosRepository.save(mov);
		saldoActual = mov.getSaldoActual();
		
			// movimiento tipo 7 - Retiro por cierre de cuenta
		tipomov = tipomovRepository.findByCodigoMovimiento((short) 7);
		mov = new Movimientos();
		mov.setCodigoMovimiento(tipomov.getCodigoMovimiento());
		mov.setFechaMovimiento(new Date());
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(BigDecimal.ZERO);
		mov.setImporteIntFunc(BigDecimal.ZERO);
		mov.setImporteMov(estadoCta.getSaldoCuenta());
		mov.setMesliquidacion(mesliquidacion);
		mov.setObservaciones("Cierre de Cuenta");
		mov.setSaldoAnterior(saldoActual);
		mov.setSaldoActual(saldoActual.subtract(estadoCta.getSaldoCuenta()));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setTipoMovimiento(tipomov);
		
		mov = movimientosRepository.save(mov);
		
		// Creación de registros de saldos historia y por el cierre
		
		SaldosHistoria saldosh = new SaldosHistoria();
//		SaldosHistoria saldoshult = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(funcionario.getTarjeta(), mesliquidacion);
		SaldosHistoria saldoshult = saldosHistoriaRepository.getUltimoByTarjeta(funcionario.getTarjeta());
		BigDecimal cdant = saldoshult.getCapitalDispActual();
		BigDecimal ciant = saldoshult.getCapitalIntegActual();

		saldosh.setFecha(new Date());
		saldosh.setGplanta_id(funcionario.getIdgplanta());
		saldosh.setMesliquidacion(mesliquidacion);
		saldosh.setTarjeta(funcionario.getTarjeta());

		if(estadoCta.getAporteTotal().compareTo(BigDecimal.ZERO) > 0) {
			saldosh.setCapitalDispActual(cdant.add(estadoCta.getAporteTotal()));
			saldosh.setCapitalIntegActual(ciant.add(estadoCta.getAporteTotal()));
			saldosh.setCapitalDispAntes(cdant);
			saldosh.setCapitalIntegAntes(ciant);
			saldosh.setNumerales(ciant.add(estadoCta.getAporteTotal()));
			saldosh.setMotivo("Aportes - Cierre de Cuenta");
			saldosHistoriaRepository.save(saldosh);
			
			saldosh = new SaldosHistoria();
			saldoshult = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(funcionario.getTarjeta(), mesliquidacion);
			
			cdant = saldoshult.getCapitalDispActual();
			ciant = saldoshult.getCapitalIntegActual();

			saldosh.setFecha(new Date());
			saldosh.setGplanta_id(funcionario.getIdgplanta());
			saldosh.setMesliquidacion(mesliquidacion);
			saldosh.setTarjeta(funcionario.getTarjeta());			
		}
		
		if(estadoCta.getAporteTotalSobreAguinaldo().compareTo(BigDecimal.ZERO) > 0) {
			saldosh.setCapitalDispActual(cdant.add(estadoCta.getAporteTotalSobreAguinaldo()));
			saldosh.setCapitalIntegActual(ciant.add(estadoCta.getAporteTotalSobreAguinaldo()));
			saldosh.setCapitalDispAntes(cdant);
			saldosh.setCapitalIntegAntes(ciant);
			saldosh.setNumerales(ciant.add(estadoCta.getAporteTotalSobreAguinaldo()));
			saldosh.setMotivo("Aportes Aguinaldo - Cierre de Cuenta");
			saldosHistoriaRepository.save(saldosh);
			
			saldosh = new SaldosHistoria();
			saldoshult = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(funcionario.getTarjeta(), mesliquidacion);
			
			cdant = saldoshult.getCapitalDispActual();
			ciant = saldoshult.getCapitalIntegActual();

			saldosh.setFecha(new Date());
			saldosh.setGplanta_id(funcionario.getIdgplanta());
			saldosh.setMesliquidacion(mesliquidacion);
			saldosh.setTarjeta(funcionario.getTarjeta());			
			
		}
		
		if(estadoCta.getAporteTotLicencia().compareTo(BigDecimal.ZERO) > 0) {
			saldosh.setCapitalDispActual(cdant.add(estadoCta.getAporteTotLicencia()));
			saldosh.setCapitalIntegActual(ciant.add(estadoCta.getAporteTotLicencia()));
			saldosh.setCapitalDispAntes(cdant);
			saldosh.setCapitalIntegAntes(ciant);
			saldosh.setNumerales(ciant.add(estadoCta.getAporteTotLicencia()));
			saldosh.setMotivo("Aportes Licencia - Cierre de Cuenta");
			saldosHistoriaRepository.save(saldosh);
			
			saldosh = new SaldosHistoria();
			saldoshult = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(funcionario.getTarjeta(), mesliquidacion);
			
			cdant = saldoshult.getCapitalDispActual();
			ciant = saldoshult.getCapitalIntegActual();

			saldosh.setFecha(new Date());
			saldosh.setGplanta_id(funcionario.getIdgplanta());
			saldosh.setMesliquidacion(mesliquidacion);
			saldosh.setTarjeta(funcionario.getTarjeta());						
		}
		
		if(estadoCta.getInteresesporcolocaciones().compareTo(BigDecimal.ZERO) > 0) {
			saldosh.setCapitalDispActual(cdant.add(estadoCta.getInteresesporcolocaciones()));
			saldosh.setCapitalIntegActual(ciant.add(estadoCta.getInteresesporcolocaciones()));
			saldosh.setCapitalDispAntes(cdant);
			saldosh.setCapitalIntegAntes(ciant);
			saldosh.setNumerales(ciant.add(estadoCta.getInteresesporcolocaciones()));
			saldosh.setMotivo("Intereses - Cierre de Cuenta");
			saldosHistoriaRepository.save(saldosh);
			
			saldosh = new SaldosHistoria();
			saldoshult = saldosHistoriaRepository.getLastByTarjetaAndMesliquidacion(funcionario.getTarjeta(), mesliquidacion);
			
			cdant = saldoshult.getCapitalDispActual();
			ciant = saldoshult.getCapitalIntegActual();

			saldosh.setFecha(new Date());
			saldosh.setGplanta_id(funcionario.getIdgplanta());
			saldosh.setMesliquidacion(mesliquidacion);
			saldosh.setTarjeta(funcionario.getTarjeta());						
			
		}
		
		//pasaje a históricos
		
		try {
			if(pasajeAHistoricosMovimientos(funcionario.getTarjeta())) {
				eliminarMovimientos(funcionario.getTarjeta());
			}
		}
		catch(Exception e) {
			throw new Exception("falló el proceso de pasaje a históricos de Movimientos");
		}
		
//		if(estadoCta.getLstPrst() != null && !estadoCta.getLstPrst().isEmpty()) {
		if(!estadoCta.getLstPrst().isEmpty()) {
			try {
				prestamoshistRepository.insertByTarjeta(funcionario.getTarjeta());
				prestamoRepository.deleteByTarjeta(funcionario.getTarjeta());
				saldoPrestamosRepository.deleteByTarjeta(funcionario.getTarjeta());
			}
			catch(Exception e) {
				throw new Exception("falló el proceso de pasaje a históricos de Prestamos");
			}
		}
		
		try {
			resultDistribHistRepository.insertByTarjeta(funcionario.getTarjeta());
			sueldoMesHistRepository.insertByTarjeta(funcionario.getTarjeta());
			gplantahistRepository.insertByTarjeta(funcionario.getTarjeta());
			resultadoDistribRepository.deleteByTarjeta(funcionario.getTarjeta());
			sueldoMesRepository.deleteByTarjeta(funcionario.getTarjeta());
			User user = userService.getUserByTarjeta(funcionario.getTarjeta());
			userService.deleteUser(user.getId());
			saldosRepository.deleteByTarjeta(funcionario.getTarjeta());
			gvinculoRepository.deleteByTarjeta(funcionario.getTarjeta());
			
			List<SolicitudPrestamo> lstSolPrst = solicitudPrstRepository.getAllByGplantaId(funcionario.getIdgplanta());
			if(!lstSolPrst.isEmpty()) {
				solicitudPrstRepository.deleteByTarjeta(funcionario.getTarjeta());				
			}
			saldoPrestamosRepository.deleteByTarjeta(funcionario.getTarjeta());
			gplantaRepository.deleteByTarjeta(funcionario.getTarjeta());	
			guardarDatosCierre(estadoCta);		
		}catch(Exception e) {
			throw new Exception("falló el proceso de pasaje a histórico del reg.del funcionario");
		}
		
		return true;
	}

	private void guardarDatosCierre(EstadoDeCtaCierre estadoCta) {
	
		DatosCierreCta datoscierre = new DatosCierreCta();
		datoscierre.setGplanta_id(estadoCta.getFuncionario().getIdgplanta());
		datoscierre.setTarjeta(estadoCta.getFuncionario().getTarjeta());
		datoscierre.setNombre(estadoCta.getFuncionario().getNombre());
		datoscierre.setFechaEgreso(estadoCta.getFechaEgreso());
		datoscierre.setCapitalIntegrado(estadoCta.getCapitalIntegrado());
		datoscierre.setHaberesUltimoMes(estadoCta.getSueldoUltimoMes());
		datoscierre.setAporteHaberes(estadoCta.getAporteTotal());
		datoscierre.setImporteAguinaldo(estadoCta.getImporteAguinaldo());
		datoscierre.setInteresesUtilidades(estadoCta.getInteresesporcolocaciones());
		datoscierre.setAporteAguinaldo(estadoCta.getAporteTotalSobreAguinaldo());
		datoscierre.setSaldoPrestamos(estadoCta.getSaldoPrestamos());
		datoscierre.setSaldoCuenta(estadoCta.getSaldoCuenta());
		datoscierre.setImporteLicencia(estadoCta.getImporteLicencia());
		datoscierre.setAporteLicencia(estadoCta.getAporteTotLicencia());
		
		String prstDescontados = "";
		if(estadoCta.getLstPrst() != null && !estadoCta.getLstPrst().isEmpty()) {
			for(Prestamo prst : estadoCta.getLstPrst()) {
				prstDescontados += prst.getNroprestamo() + ",";
			}
		}
		datoscierre.setPrstDescontados(prstDescontados);
		datoscierreRepository.save(datoscierre);
	}
	
	private Boolean pasajeAHistoricosMovimientos(Integer tarjeta) throws Exception {
		try {
			movhistRepository.insertAllByTarjeta(tarjeta);
			return true;
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private void eliminarMovimientos(Integer tarjeta) throws Exception {
		try {
			movimientosRepository.deleteAllByTarjeta(tarjeta);
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
}
