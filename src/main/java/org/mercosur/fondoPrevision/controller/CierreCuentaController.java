package org.mercosur.fondoPrevision.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.mercosur.fondoPrevision.dto.CierreCtaForm;
import org.mercosur.fondoPrevision.dto.EstadoDeCtaCierre;
import org.mercosur.fondoPrevision.entities.Ayuda;
import org.mercosur.fondoPrevision.entities.DatosCierreCta;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Gplantahist;
import org.mercosur.fondoPrevision.entities.Movimientos;
import org.mercosur.fondoPrevision.entities.MovimientosHist;
import org.mercosur.fondoPrevision.entities.Prestamo;
import org.mercosur.fondoPrevision.entities.Prestamohist;
import org.mercosur.fondoPrevision.entities.SueldoMes;
import org.mercosur.fondoPrevision.excel.CierreCtaExcelExporter;
import org.mercosur.fondoPrevision.excel.CierreCtaExcelExportResumen;
import org.mercosur.fondoPrevision.repository.DatosCierreCtaRepository;
import org.mercosur.fondoPrevision.repository.DatosDistribucionRepository;
import org.mercosur.fondoPrevision.repository.GcargoRepository;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
import org.mercosur.fondoPrevision.repository.GplantahistRepository;
import org.mercosur.fondoPrevision.repository.MovimientosHistRepository;
import org.mercosur.fondoPrevision.repository.MovimientosRepository;
import org.mercosur.fondoPrevision.repository.PrestamohistRepository;
import org.mercosur.fondoPrevision.repository.SaldosRepository;
import org.mercosur.fondoPrevision.repository.SueldoMesRepository;
import org.mercosur.fondoPrevision.service.AyudaService;
import org.mercosur.fondoPrevision.service.CierreCuentaService;
import org.mercosur.fondoPrevision.service.GplantaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lowagie.text.DocumentException;

@Controller
public class CierreCuentaController {

	@Autowired
	AyudaService ayudaService;
	
	@Autowired
	SaldosRepository saldosRepository;
	
	@Autowired
	GcargoRepository gcargoRepository;
	
	@Autowired
	GplantaRepository gplantaRepository;
	
	@Autowired
	GplantahistRepository gplantahistRepository;
	
	@Autowired
	GplantaService gplantaService;
	
	@Autowired
	SueldoMesRepository sueldoMesRepository;
	
	@Autowired
	MovimientosRepository movimientosRepository;
	
	@Autowired
	MovimientosHistRepository movimientosHistRepository;

	@Autowired
	CierreCuentaService cierreCtaService;
	
	@Autowired
	DatosDistribucionRepository datosdistribRepository;
	
	@Autowired
	DatosCierreCtaRepository datosCierreRepository;
	
	@Autowired
	PrestamohistRepository prsthistRepository;
	
	@GetMapping("/cierrectaForm")
	public String getCierreCtaForm(Model model) {
		
		try {
			model.addAttribute("plantaList", gplantaService.getAllPlanta());
			String clave = "fcierreCta";
			Ayuda help = ayudaService.getByClave(clave);
			EstadoDeCtaCierre estadodecuenta = new EstadoDeCtaCierre();
			List<Prestamo> lstPrst = new ArrayList<Prestamo>();
			estadodecuenta.setLstPrst(lstPrst);
			model.addAttribute("cierrectaForm", new CierreCtaForm());
			model.addAttribute("estadoDeCtaFinal", estadodecuenta);
			model.addAttribute("outputMode", false);
			model.addAttribute("help", help);
			model.addAttribute("procname", "CierreCta");
		}
		catch(Exception e){
			model.addAttribute("formError", e.getMessage());
		}

		return "cierre-cuenta/cierrecuenta-form";
	}

	@RequestMapping(value= {"/ejecutarCierreCta"}, params= {"calcularCierre"})
	public String calcularCierre(@Valid @ModelAttribute("cierrectaForm") CierreCtaForm form, BindingResult result, ModelMap model) {
		if(result.hasErrors()) {
			model.addAttribute("cierrectaForm", form);
			model.addAttribute("formError", result.getAllErrors().toString());
			return "cierre-cuenta/cierrecuenta-form.html";
		}
		try {
			EstadoDeCtaCierre estadoFinal = cierreCtaService.cerrarCuenta(form);
			model.addAttribute("plantaList", gplantaService.getAllPlanta());
			String clave = "fcierreCta";
			Ayuda help = ayudaService.getByClave(clave);
			model.addAttribute("help", help);
			model.addAttribute("cierrectaForm", form);
			model.addAttribute("estadoDeCtaFinal", estadoFinal);
			model.addAttribute("outputMode", true);
			model.addAttribute("conlink", true);
			
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
			model.addAttribute("cierrectaForm", form);
		}
		model.addAttribute("plantaList", gplantaService.getAllPlanta());
		return "cierre-cuenta/cierrecuenta-form";
	}

	@RequestMapping(value= {"/ejecutarCierreCta"}, params= {"cerrarCta"})
	public String cerrarCuenta(@Valid @ModelAttribute("cierrectaForm") CierreCtaForm form, BindingResult result, ModelMap model) {
		if(result.hasErrors()) {
			model.addAttribute("cierrectaForm", form);
			model.addAttribute("formError", result.getAllErrors().toString());
			return "cierre-cuenta/cierrecuenta-form.html";
		}
		try {
			EstadoDeCtaCierre estadoFinal = cierreCtaService.cerrarCuenta(form);
			if(cierreCtaService.consolidarCierredeCuenta(estadoFinal)) {
				model.addAttribute("formSuccess", "Cierre de Cuenta Exitoso !!");
				model.addAttribute("cierrectaForm", form);
				model.addAttribute("estadoDeCtaFinal", estadoFinal);
				model.addAttribute("outputMode", true);
			}
			else {
				model.addAttribute("formError", "La consolidación de la información no terminó bien!");
				model.addAttribute("cierrectaForm", form);
				model.addAttribute("estadoDeCtaFinal", estadoFinal);
				model.addAttribute("outputMode", false);
			}
			String clave = "fcierreCta";
			Ayuda help = ayudaService.getByClave(clave);
			model.addAttribute("help", help);
			model.addAttribute("plantaList", gplantaService.getAllPlanta());
			model.addAttribute("conlink", false);
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
			model.addAttribute("cierrectaForm", form);
		}
		return "cierre-cuenta/cierrecuenta-form";
	}
	
	@RequestMapping(value={"/ejecutarCierreCta"}, params= {"exportExcel"})
	public void liquidacionToExcel(@Valid @ModelAttribute("cierrectaForm") CierreCtaForm form,
			BindingResult result, ModelMap model, HttpServletResponse response) throws IOException, Exception{

		String mesDistribucion = datosdistribRepository.getUltimoMesDistrib();
		String headerKey = "Content-Disposition";
		EstadoDeCtaCierre estadoFinal = new EstadoDeCtaCierre();

		String mesLiqEgreso = form.getFechaEgreso().substring(0,4).concat(form.getFechaEgreso().substring(5,7));
		
		Optional<Gplanta> funcionario = gplantaRepository.findById(form.getIdfuncionario());
		if(funcionario.isPresent()) {
			estadoFinal = cierreCtaService.cerrarCuenta(form);							
		}
		else {
			DatosCierreCta datosCierre = datosCierreRepository.getDatosByGplantaid(form.getIdfuncionario());
			estadoCtaFromDatosCierre(estadoFinal, datosCierre);				
		}
		String headerValue = "attachment; filename=liqEgreso_" + estadoFinal.getFuncionario().getTarjeta() + ".xlsx";
		String ultimoMesLiq;
		
		try {
			Movimientos mov = movimientosRepository.getLastByFuncAndTipo(estadoFinal.getFuncionario().getTarjeta(), 2);
			ultimoMesLiq = mov.getMesliquidacion(); 				

			response.setHeader(headerKey, headerValue);
			CierreCtaExcelExporter exporter = new CierreCtaExcelExporter(estadoFinal, mesDistribucion, mesLiqEgreso, ultimoMesLiq);
			exporter.export(response);
			
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
			model.addAttribute("cierrectaFrom", form);

		}
	}

	@GetMapping("/excelEstadoFinal/{id}")
    public void excelEstado(HttpServletResponse response, @PathVariable(name="id") Long idfuncionario, Model model) throws DocumentException, IOException, Exception{
		String headerKey = "Content-Disposition";
		try {
			String mesDistribucion = datosdistribRepository.getUltimoMesDistrib();
			EstadoDeCtaCierre estadoFinal = new EstadoDeCtaCierre();

			DatosCierreCta datosCierre = datosCierreRepository.getDatosByGplantaid(idfuncionario);

			estadoCtaFromDatosCierre(estadoFinal, datosCierre);
						
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			String mesActual = datosCierre.getFechaEgreso().format(dtf);
			String mesLiqEgreso = mesActual.substring(0, 4).concat(mesActual.substring(5,7));

			model.addAttribute("estadoDeCtaFinal", estadoFinal);
			String headerValue = "attachment; filename=liqEgreso_" + estadoFinal.getFuncionario().getTarjeta() + ".xlsx";
			
			MovimientosHist mov = movimientosHistRepository.getLastByFuncAndTipo(estadoFinal.getFuncionario().getTarjeta(), 2);
			String ultimoMesLiq = mov.getMesliquidacion(); 
			
			response.setHeader(headerKey, headerValue);
			CierreCtaExcelExportResumen exporter = new CierreCtaExcelExportResumen(estadoFinal, mesDistribucion, mesLiqEgreso, ultimoMesLiq);
			exporter.export(response);			
			}
			catch(Exception e) {
				throw new Exception(e.getMessage());
			}
	}
	
	private BigDecimal sumaBasicos(String aniomes1, String aniomes2, Integer tarjeta) throws Exception {
		BigDecimal totbasico = BigDecimal.ZERO;

		List<SueldoMes> lstSueldoMes = sueldoMesRepository.getByPeriodoAndTarjeta(aniomes1, aniomes2, tarjeta);
		for(SueldoMes sm : lstSueldoMes) {
			totbasico = totbasico.add(sm.getSueldomes());
		}
		return totbasico;
	}

	private void estadoCtaFromDatosCierre(EstadoDeCtaCierre estadoFinal, DatosCierreCta datosCierre) throws Exception{

		Gplantahist funch = gplantahistRepository.getByTarjeta(datosCierre.getTarjeta());
		
		BigDecimal basicoActual = gcargoRepository.findById(funch.getGcargos_id()).get().getBasico();

		int days = datosCierre.getFechaEgreso().getDayOfMonth();
		BigDecimal bddays = new BigDecimal(String.valueOf(days));
		
		BigDecimal bdTreinta = new BigDecimal(String.valueOf(30));
		BigDecimal divDoce = new BigDecimal(String.valueOf(12));

		basicoActual = basicoActual.divide(bdTreinta, 2, RoundingMode.HALF_DOWN);
		basicoActual = basicoActual.multiply(bddays);
		estadoFinal.setBasico(basicoActual);

		LocalDate mesAnterior = datosCierre.getFechaEgreso().minusMonths(1);
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		String mesAntes = mesAnterior.format(dtf);
		String mesliquidacion = mesAntes.substring(0, 4).concat(mesAntes.substring(5, 7));
		String mesliqinicial;
		if(datosCierre.getFechaEgreso().getMonth().compareTo(Month.JUNE) <= 0) {
			mesliqinicial = mesAntes.substring(0, 4).concat("01");
		}
		else {
			mesliqinicial = mesAntes.substring(0, 4).concat("07");
		}
		BigDecimal bdSumabasicos = sumaBasicos(mesliqinicial, mesliquidacion, datosCierre.getTarjeta());
		bdSumabasicos = bdSumabasicos.add(basicoActual);
		bdSumabasicos = bdSumabasicos.divide(divDoce, 2, RoundingMode.HALF_DOWN);
		estadoFinal.setSumaBasicos(bdSumabasicos);

		
		Gplanta funcionario = new Gplanta();
		funcionario.setNombre(funch.getNombre());
		funcionario.setTarjeta(funch.getTarjeta());
		funcionario.setEmail(funch.getEmail());
		
		estadoFinal.setAporteTotal(datosCierre.getAporteHaberes());
		estadoFinal.setAporteTotalSobreAguinaldo(datosCierre.getAporteAguinaldo());
		estadoFinal.setCapitalIntegrado(datosCierre.getCapitalIntegrado());
		estadoFinal.setFechaEgreso(datosCierre.getFechaEgreso());
		estadoFinal.setImporteAguinaldo(datosCierre.getImporteAguinaldo());
		estadoFinal.setFuncionario(funcionario);
		estadoFinal.setIdfuncionario(datosCierre.getGplanta_id());
		estadoFinal.setInteresesporcolocaciones(datosCierre.getInteresesUtilidades());
		estadoFinal.setSaldoCuenta(datosCierre.getSaldoCuenta());
		estadoFinal.setSaldoPrestamos(datosCierre.getSaldoPrestamos());
		estadoFinal.setSueldoUltimoMes(datosCierre.getHaberesUltimoMes());
		estadoFinal.setImporteLicencia(datosCierre.getImporteLicencia());
		estadoFinal.setAporteTotLicencia(datosCierre.getAporteLicencia());
		
		List<Prestamo> lstPrst = new ArrayList<Prestamo>();
		
		if(!datosCierre.getPrstDescontados().isEmpty()) {
			String[] arrofStr = datosCierre.getPrstDescontados().split(",");
			for(String s : arrofStr) {
				Prestamohist prsth = prsthistRepository.findByNroprestamo(Integer.valueOf(s));
				Prestamo prst = new Prestamo();
				prst.setCantCuotas(prsth.getCantCuotas());
				prst.setCapitalPrestamo(prsth.getCapitalPrestamo());
				prst.setCodigoPrestamo(prsth.getCodigoPrestamo());
				prst.setCuota(prsth.getCuota());
				prst.setCuotasPagas(prsth.getCuotasPagas());
				prst.setFechaPrestamo(prsth.getFechaPrestamo());
				prst.setInteresPrestamo(prsth.getInteresPrestamo());
				prst.setFuncionario(funcionario);
				prst.setNroprestamo(prsth.getNroprestamo());
				prst.setSaldoPrestamo(prsth.getSaldoPrestamo());
				prst.setTarjeta(prsth.getTarjeta());
				lstPrst.add(prst);
			}
		}
	}

	@GetMapping("/consultaCierre")
	public String getConsultaCierre(ModelMap model) {
		
		List<DatosCierreCta> lstDatosCierre = datosCierreRepository.findAll();
		model.addAttribute("lstDatosCierre", lstDatosCierre);
		model.addAttribute("estadoDeCtaFinal", new EstadoDeCtaCierre());
		model.addAttribute("outputMode", false);
		return "cierre-cuenta/consultaCierre";
	}
	
	@GetMapping("/estadoFinal/cancel")
	public String cancelEstado(ModelMap model) {
		return "redirect:/consultaCierre";
	}

	@RequestMapping(value= {"/estadoCtaFinal"}, params= {"getInfo"})
	public String getInfoCierreCuenta(@Valid @ModelAttribute("estadoDeCtaFinal") EstadoDeCtaCierre form, BindingResult result, ModelMap model) {

		try {
			EstadoDeCtaCierre estadoFinal = new EstadoDeCtaCierre();

			DatosCierreCta datosCierre = datosCierreRepository.getDatosByGplantaid(form.getIdfuncionario());
			estadoCtaFromDatosCierre(estadoFinal, datosCierre);
			model.addAttribute("estadoDeCtaFinal", estadoFinal);
			
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
		}

		model.addAttribute("outputMode", true);
		return "cierre-cuenta/consultaCierre";
	}
	

		
}
