package org.mercosur.fondoPrevision.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.NoResultException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import org.mercosur.fondoPrevision.dto.EstadoDeCta;
import org.mercosur.fondoPrevision.dto.PrstNuevoForm;
import org.mercosur.fondoPrevision.entities.Ayuda;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Parametro;
import org.mercosur.fondoPrevision.entities.Prestamo;
import org.mercosur.fondoPrevision.entities.Prestamohist;
import org.mercosur.fondoPrevision.entities.Saldos;
import org.mercosur.fondoPrevision.entities.SolicitudPrestamo;
import org.mercosur.fondoPrevision.entities.TipoPrestamo;
import org.mercosur.fondoPrevision.exceptions.ParamNotFoundException;
import org.mercosur.fondoPrevision.exceptions.TipoPrestamoNotFoundException;
import org.mercosur.fondoPrevision.pdfs.PrestamosPdfExporter;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
import org.mercosur.fondoPrevision.repository.ParamPrestamoRepository;
import org.mercosur.fondoPrevision.repository.PrestamoRepository;
import org.mercosur.fondoPrevision.repository.PrestamohistRepository;
import org.mercosur.fondoPrevision.repository.SolicitudPrestamoRepository;
import org.mercosur.fondoPrevision.service.AyudaService;
import org.mercosur.fondoPrevision.service.EstadoDeCtaService;
import org.mercosur.fondoPrevision.service.GplantaService;
import org.mercosur.fondoPrevision.service.ParamService;
import org.mercosur.fondoPrevision.service.PrestamoService;
import org.mercosur.fondoPrevision.service.SolicitudPrestamoService;
import org.mercosur.fondoPrevision.service.TipoPrestamoService;
import org.mercosur.fondoPrevision.service.UserService;

import com.lowagie.text.DocumentException;

@Controller
public class PrestamosController {
	
	@Autowired
	UserService userService;
	
	@Autowired
	GplantaService gplantaService;
	
	@Autowired
	GplantaRepository gplantaRepository;
	
	@Autowired
	TipoPrestamoService tipoPrestamoService;
	
	@Autowired
	ParamService parametroService;
	
	@Autowired
	PrestamoService prestamoService;
	
	@Autowired
	PrestamoRepository prestamoRepository;
	
	@Autowired
	EstadoDeCtaService estadoDeCtaService;
	
	@Autowired
	SolicitudPrestamoRepository solicitudPrestamoRepository;
	
	@Autowired
	SolicitudPrestamoService solicitudPrestamoService;
	
	@Autowired
	PrestamohistRepository prestamoHistRepository;
	
	@Autowired
	ParamPrestamoRepository paramPrstRepository;
	
	@Autowired
	AyudaService ayudaService;
	
	@SuppressWarnings("unchecked")
	@ModelAttribute("plantaList")
	public List<Gplanta> populatePlanta(){
		return (List<Gplanta>) gplantaService.getAllPlanta();
	}
	
	@ModelAttribute("prestamosList")
	public List<Prestamo> populatePrestamos(){
		return prestamoService.getAllPrst();
	}
	
	@ModelAttribute("tiposPrstList")
	public List<TipoPrestamo> populateTipoPrst(){
		return prestamoService.getAllTipoPrst();
	}
		
	@GetMapping("/prestForm")
	public String getPrestForm(ModelMap model) {
	
		try {
			String clave = "fprst";
			Ayuda help = ayudaService.getByClave(clave);
			model.addAttribute("help", help);			
			model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
			model.addAttribute("prstNuevoForm", new PrstNuevoForm(prestamoService.getProxNroPrst()));
			model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
			model.addAttribute("menu", true);
			model.addAttribute("editMode", false);
			if(userService.isLoggedUserADMIN()) {
				model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			}
			
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
		}

		model.addAttribute("procname", "Intermedio");
		model.addAttribute("listTab", "active");
		return("prestamos/prestamos-view");
	}

	@GetMapping("/procesarSolicitud/{idsol}")
	public String procesarSolicitud(@PathVariable(name="idsol") Long idsol, ModelMap model) throws Exception{
	
		try {
			String clave = "fprst";
			Ayuda help = ayudaService.getByClave(clave);
			model.addAttribute("help", help);				
			model.addAttribute("editMode", false);

			Optional<SolicitudPrestamo> solicitud = solicitudPrestamoRepository.findById(idsol); 
			Integer tarjeta = solicitud.get().getTarjeta();
			Gplanta funcionario = gplantaService.getFuncionarioByTarjeta(tarjeta);
			model.addAttribute("funcionario", funcionario);
			PrstNuevoForm prstNuevoForm = new PrstNuevoForm(prestamoService.getProxNroPrst());
			prstNuevoForm.setFechaprestamo(solicitud.get().getFechaSolicitud());
			prstNuevoForm.setCantCuotas(solicitud.get().getCantCuotas());
			prstNuevoForm.setCapitalPrst(solicitud.get().getCapitalPrestamo());
			prstNuevoForm.setCuotaPrst(solicitud.get().getCuota());
			prstNuevoForm.setIdparamPrst(solicitud.get().getParamPrestamo().getIdfparamprst());
			prstNuevoForm.setTasa(solicitud.get().getInteresPrestamo());
			prstNuevoForm.setSaldoPrst(solicitud.get().getCapitalPrestamo());
			prstNuevoForm.setTarjeta(solicitud.get().getTarjeta());
			prstNuevoForm.setIdfuncionario(funcionario.getIdgplanta());
			prstNuevoForm.setIdsolicitud(solicitud.get().getIdfsolicitud());
			prstNuevoForm.setPrstAcancelar(solicitud.get().getCancelPrstNros());
			prstNuevoForm.setIdtipoprst(solicitud.get().getTipoPrestamo().getIdftipoprestamo());
			
			model.addAttribute("prstNuevoForm", prstNuevoForm);
			model.addAttribute("disabledFields", true);
			model.addAttribute("tipoPrestamo", solicitud.get().getTipoPrestamo().getDescripcion());
			model.addAttribute("plantaList", gplantaService.getAllPlanta());
			model.addAttribute("prestamosList", prestamoService.getAllPrst());
			model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
			
		}
		catch(Exception e) {
			model.addAttribute("formError", e.getMessage());
		}

		model.addAttribute("procname", "Intermedio");
		model.addAttribute("prstNewTab", "active");
		
		return("/prestamos/prestamos-view");
	}
	
	@GetMapping("/editPrst/{idprst}")
	public String getEditPrst(@PathVariable(name="idprst") Long idprst, ModelMap model) throws Exception{
		try {
			Prestamo prst = prestamoService.getPrstById(idprst);
			Gplanta funcionario = gplantaService.getFuncionarioByTarjeta(prst.getTarjeta());
			model.addAttribute("funcionario", funcionario);
			TipoPrestamo tipo = tipoPrestamoService.getById(prst.getTipoPrestamo().getIdftipoprestamo());

			PrstNuevoForm prstNuevoForm = new PrstNuevoForm();
			prstNuevoForm.setCantCuotas(prst.getCantCuotas());
			prstNuevoForm.setCuotasPagas(prst.getCuotasPagas());
			prstNuevoForm.setCapitalPrst(prst.getCapitalPrestamo());
			prstNuevoForm.setCuotaPrst(prst.getCuota());
			prstNuevoForm.setIdfprestamos(prst.getIdfprestamos());
			prstNuevoForm.setNroprestamo(prst.getNroprestamo());
			prstNuevoForm.setPrstNuevo(prst.getPrestamoNuevo());
			prstNuevoForm.setTasa(prst.getInteresPrestamo());
			prstNuevoForm.setSaldoPrst(prst.getSaldoPrestamo());
			prstNuevoForm.setTarjeta(prst.getTarjeta());
			prstNuevoForm.setIdfuncionario(funcionario.getIdgplanta());
			prstNuevoForm.setIdtipoprst(tipo.getIdftipoprestamo());
			
			
			model.addAttribute("prstNuevoForm", prstNuevoForm);
			model.addAttribute("editMode", true);
			model.addAttribute("tipoPrst", tipo);
			model.addAttribute("plantaList", gplantaService.getAllPlanta());
			model.addAttribute("prestamosList", prestamoService.getAllPrst());
			model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			model.addAttribute("prstNewTab", "active");
			
		}
		catch(TipoPrestamoNotFoundException tpnf) {
			throw new Exception(tpnf.getMessage());
		}
		return("prestamos/prestamos-view");
	}
	
	@RequestMapping(value= {"/prestnuevoForm", "/editPrst"}, params= {"estadoCta"})
	public String traerEstadoDeCta(@Valid @ModelAttribute("prstNuevoForm") PrstNuevoForm form, BindingResult result,
						HttpServletRequest request, ModelMap model) {

		if(result.hasErrors()) {
			model.addAttribute("formError", result.getAllErrors().toString());
			model.addAttribute("prstNewTab", "active");
			return "prestamos/prestamos-view"; 
		}
		Gplanta funcionario = gplantaRepository.getOne(form.getIdfuncionario());
		
		try {
			EstadoDeCta newEstado = estadoDeCtaService.getEstadoDeCtabyFuncionario(form.getIdfuncionario());
			newEstado.setFuncionario(funcionario);
			model.addAttribute("estadoDeCta", newEstado);
			model.addAttribute("outputMode", true);
			String uri = request.getRequestURI();
			if(uri.equals("/editPrst")) {
				TipoPrestamo tipo = tipoPrestamoService.getById(form.getIdtipoprst());
				model.addAttribute("tipoPrst", tipo);
				model.addAttribute("editMode", true);
			}
		}
		catch(Exception e) {
			model.addAttribute("estadoDeCta", new EstadoDeCta());
			model.addAttribute("formError", e.getMessage());
		}
		model.addAttribute("prstNuevoForm", form);
		model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
		model.addAttribute("funcionario", funcionario);
		model.addAttribute("prstNewTab", "active");
		return("prestamos/prestamos-view");
	}
	
	@RequestMapping(value= {"/prestnuevoForm", "/editPrst"}, params= {"calcularCuota"})
	public String calcularCuota(@Valid @ModelAttribute("prstNuevoForm") PrstNuevoForm form, BindingResult result,
			HttpServletRequest request, ModelMap model){
		form.setSuccessMessage(null);
		form.setErrorMessage(null);
		Gplanta funcionario;

		try {
			if(form.getCantCuotas() == null | form.getCapitalPrst()== null | form.getTasa() == null) {
				form.setErrorMessage("El capital, la cantidad de cuotas y la tasa de interés son datos requeridos!");
				form.setNroprestamo(prestamoService.getProxNroPrst());
				model.addAttribute("prstNewTab", "active");
				return("prestamos/prestamos-view");
			}
			if(form.getTarjeta() != null) {
				funcionario = gplantaService.getFuncionarioByTarjeta(form.getTarjeta());							
			}
			else {
				funcionario = gplantaRepository.getOne(form.getIdfuncionario());		
			}
			model.addAttribute("funcionario", funcionario);
			if(!chequeoDisponible(funcionario, form.getCapitalPrst()) ) {
				form.setErrorMessage("El funcionario no está en condiciones de contratar el prestamo. No posee capital suficiente!");
			}
			else if(!puedeOperar(funcionario)) {
				form.setErrorMessage("El funcionario no está en condiciones de operar. Posee un prestamo reciente!");
			}
		}
		catch(ParamNotFoundException pfe) {
			form.setErrorMessage(pfe.getMessage());
		}
		catch(Exception e) {
			form.setErrorMessage(e.getMessage());
		}
				
		String uri = request.getRequestURI();
		if(uri.equals("/editPrst")) {
			try {
				Prestamo prst = prestamoService.getPrstById(form.getIdfprestamos());
				form.setCuotasPagas(prst.getCuotasPagas());
				form.setSaldoPrst(prst.getSaldoPrestamo());
				form.setPrstNuevo(prst.getPrestamoNuevo());
				TipoPrestamo tipo = tipoPrestamoService.getById(form.getIdtipoprst());
				model.addAttribute("editMode", true);
				model.addAttribute("tipoPrst", tipo);	
			}
			catch(Exception e) {
				form.setErrorMessage(e.getMessage());
			}
			
		}
		if(result.hasErrors()) {
			form.setErrorMessage(result.getAllErrors().toString());				
		}
		else {
			BigDecimal interes = form.getTasa().divide(new BigDecimal("1200"), MathContext.DECIMAL32);
			BigDecimal factor = BigDecimal.ONE.add(interes).pow(Integer.valueOf(form.getCantCuotas().toString())).setScale(9,  BigDecimal.ROUND_HALF_UP);
			BigDecimal numerador = interes.multiply(factor);
			BigDecimal denominador = factor.subtract(BigDecimal.ONE);
			BigDecimal coeficiente = numerador.divide(denominador, MathContext.DECIMAL32);
			BigDecimal valorCuota = coeficiente.multiply(form.getCapitalPrst()).multiply(new BigDecimal("100")).divide(new BigDecimal("100"), RoundingMode.HALF_UP)
									.setScale(2, BigDecimal.ROUND_HALF_UP);
			form.setCuotaPrst(valorCuota);
		}
		model.addAttribute("prstNuevoForm", form);
		model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
		model.addAttribute("prstNewTab", "active");
		return("prestamos/prestamos-view");
	}
	
	private Boolean chequeoDisponible(Gplanta funcionario, BigDecimal capSolicitado) throws Exception {
		Saldos saldos = funcionario.getSaldos();
		try {
			Parametro pctReserva = parametroService.getParametroByDescripcion("Pct de Reserva");
			BigDecimal reserva = saldos.getCapitalIntegActual().multiply(pctReserva.getValor(), MathContext.DECIMAL32).divide(new BigDecimal("100"), RoundingMode.HALF_EVEN);
			BigDecimal tope = saldos.getCapitalIntegActual().subtract(reserva, MathContext.DECIMAL32);
			if(capSolicitado.compareTo(tope) > 0) {
				if(capSolicitado.subtract(tope).compareTo(new BigDecimal("1")) < 0) {
					return true;
				}
				else {
					return false;
				}
			}
			else {
				return true;
			}
		} catch (ParamNotFoundException e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private Boolean puedeOperar(Gplanta funcionario) throws ParamNotFoundException {
		try {
			Parametro cantcuotasMin = parametroService.getParametroByDescripcion("Cuotas a pagar");
			Prestamo ultimoPrst = prestamoService.getLastByFuncionario(funcionario.getIdgplanta());
			if(ultimoPrst.getCuotasPagas().intValue() >= cantcuotasMin.getValor().intValue()) {
				return true;
			}
			else {
				return false;
			}
		}
		catch(NoResultException pne) {
			return true;
		}
	}
	
	@RequestMapping(value= {"/prestnuevoForm", "/editPrst"}, params= {"savePrst"})
	public String savePrestamo(@Valid @ModelAttribute("prstNuevoForm") PrstNuevoForm form, BindingResult result,
			HttpServletRequest request, ModelMap model) {
		String uri = request.getRequestURI();  
		form.setSuccessMessage(null);
		if(result.hasErrors()) {
			form.setErrorMessage(result.getAllErrors().toString());
		}
		else {
			if(uri.equals("/editPrst")) {
				Long idfuncionario = null;
				try {
					Gplanta funcionario = gplantaService.getFuncionarioByTarjeta(form.getTarjeta());
					model.addAttribute("funcionario", funcionario);
					idfuncionario = funcionario.getIdgplanta();
					TipoPrestamo tipo = tipoPrestamoService.getById(form.getIdtipoprst());

						// salva el prestamo nuevo.
					Prestamo prst = prestamoService.getPrstById(form.getIdfprestamos());
					prst.setCantCuotas(form.getCantCuotas());
					prst.setCapitalPrestamo(form.getCapitalPrst());
					prst.setCuota(form.getCuotaPrst());
					prst.setCuotasPagas(form.getCuotasPagas());
					prst.setInteresPrestamo(form.getTasa());
					prst.setSaldoPrestamo(form.getSaldoPrst());
					prst = prestamoService.updatePrst(prst, idfuncionario, form.getIdtipoprst());

					form.setErrorMessage(null);
					form.setSuccessMessage("El préstamo fue guardado exitosamente!. ");
					model.addAttribute("prstNuevoForm", form);
					model.addAttribute("editMode", true);
					model.addAttribute("tipoPrst", tipo);	
					model.addAttribute("prstNewTab", "active");
				}
				catch(Exception e) {
					form.setErrorMessage(e.getMessage());
					model.addAttribute("prstNuevoForm", form);
					model.addAttribute("editMode", true);
					model.addAttribute("prstNewTab", "active");
				}
				
			}
			else {
				try {
					if(form.getIdfuncionario() == null) {
						form.setErrorMessage("Se debe seleccionar el funcionario Titular del Préstamo");
						model.addAttribute("prstNuevoForm", form);
						model.addAttribute("prstNewTab", "active");
					}
					else if(form.getIdtipoprst() == null) {
						form.setErrorMessage("Por favor seleccione el tipo del Préstamo");
						model.addAttribute("prstNuevoForm", form);
						model.addAttribute("prstNewTab", "active");
					}
					else {
						form.setErrorMessage(null);
						if(form.getIdsolicitud() != null) {
							Optional<SolicitudPrestamo> solicitud = solicitudPrestamoRepository.findById(form.getIdsolicitud());
							if(solicitud.isPresent()) {
								SolicitudPrestamo sol = solicitud.get();
								sol.setProcesada(true);
								sol = solicitudPrestamoRepository.save(sol);
							}
						}
						Prestamo prst = new Prestamo(form.getCapitalPrst(), form.getTasa(), form.getCantCuotas(), form.getCuotaPrst());
							// Si hay prestamos para cancelar....
						if(!form.getPrstAcancelar().isEmpty()) {							
							String[] cancelprst = form.getPrstAcancelar().split(",");
							List<String> lstcancel = Arrays.asList(cancelprst);
							for(String nro : lstcancel) {
								Optional<Prestamo> pp = prestamoRepository.findByNroprestamo(Integer.valueOf(nro));	
								if(pp.isPresent()) {
									prestamoService.cancelPrst(pp.get().getIdfprestamos());
								}
							}
						}
						TipoPrestamo tipoPrst = tipoPrestamoService.getById(form.getIdtipoprst());
						if(tipoPrst.getCodigoPrestamo() == 1) {
							prst.setPrestamoNuevo(true);							
						}
						else {
							prst.setPrestamoNuevo(false);
						}
						prst.setFechaPrestamo(form.getFechaprestamo());
						prst.setNroprestamo(form.getNroprestamo());
						prst = prestamoService.savePrst(prst, form.getIdfuncionario(), form.getIdtipoprst());
						model.addAttribute("tipoPrestamo", tipoPrst.getDescripcion());
						Gplanta funcionario = gplantaService.getFuncionarioByTarjeta(form.getTarjeta());
						model.addAttribute("funcionario", funcionario);
						model.addAttribute("prstNuevoForm", new PrstNuevoForm(prestamoService.getProxNroPrst()));
						model.addAttribute("prestamosList", prestamoService.getAllPrst());
						model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
						model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
						model.addAttribute("listTab", "active");
					}
				}
				catch(Exception e) {
					form.setErrorMessage(e.getMessage());
					model.addAttribute("prstNuevoForm", form);
					model.addAttribute("prstNewTab", "active");
				}				
			}
		}
		model.addAttribute("procname", "Intermedio");
		model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());

		return("prestamos/prestamos-view");
	}
	
	@GetMapping("/editPrst/cancel")
	public String getCancelPrstForm(Model model) {
		return "redirect:/prestForm";
	}	

	@GetMapping("deletePrst/{id}")
	public String deletePrest(@PathVariable(name="id") Long id, ModelMap model ) {
		try {
			prestamoService.deletePrst(id);
			model.remove("prestamosList");
			model.addAttribute("prestamosList", prestamoService.getAllPrst());
			model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
			model.addAttribute("formSuccess", "El préstamo ha sido eliminado exitosamente");
		}
		catch(Exception pnf) {
			model.addAttribute("formError", pnf.getMessage());
		}
		return getPrestForm(model);
	}
	
	@GetMapping("/cancelPrst/{id}")
	public String cancelPrst(@PathVariable(name="id") Long id, @ModelAttribute("prstNuevoForm") PrstNuevoForm prstNuevoForm, ModelMap model) {
		try {
			prestamoService.cancelPrst(id);
			model.addAttribute("prestamosList", prestamoService.getAllPrst());
			model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
			model.addAttribute("formSuccess", "El préstamo ha sido cancelado exitosamente!");
		}
		catch(Exception e) {
			model.addAttribute("formError", "No ha sido posible cancelar el prestamo. " + e.getMessage());
		}
		return getPrestForm(model);			
	}
	
	@GetMapping("/undoCancelPrst/{nro}")
	public String undoCancelPrst(@PathVariable(name="nro") Integer nroPrst, ModelMap model) {
		Prestamohist ph = prestamoHistRepository.findByNroprestamo(nroPrst);
		try {
			Prestamo ppost = prestamoService.getLastByFuncionario(ph.getGplanta_id());
			if(ppost.getFechaPrestamo().isAfter(ph.getFechaSaldo())) {
				model.addAttribute("cancelErrorMessage", "En este caso no es posible realizar el procedimiento. Hay un Préstamo posterior");	
				model.addAttribute("prstNuevoForm", new PrstNuevoForm(prestamoService.getProxNroPrst()));
				model.addAttribute("editMode", false);
				model.addAttribute("plantaList", gplantaService.getAllPlanta());
				model.addAttribute("prestamosList", prestamoService.getAllPrst());
				model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
				model.addAttribute("pprstList", paramPrstRepository.findAllOrderBymeses());
				model.addAttribute("procname", "Intermedio");
				model.addAttribute("prstCancelTab", "active");
				
				return "prestamos/prestamos-view";
			}
		}
		catch(NoResultException nre) {
			// se puede continuar con el procedimiento
		}
		catch(ParamNotFoundException ex) {
			model.addAttribute("cancelErrorMessage", "No se encontró el prox. nro. préstamo");
		}
		Prestamo p = new Prestamo();
		p.setCantCuotas(ph.getCantCuotas());
		p.setCapitalPrestamo(ph.getCapitalPrestamo());
		p.setCodigoPrestamo(ph.getCodigoPrestamo());
		p.setCuota(ph.getCuota());
		p.setCuotasPagas(ph.getCuotasPagas());
		p.setFechaPrestamo(ph.getFechaPrestamo());
		p.setFechaSaldo(ph.getFechaSaldo());
		p.setFuncionario(gplantaRepository.getOne(ph.getGplanta_id()));
		p.setInteresPrestamo(ph.getInteresPrestamo());
		p.setNroprestamo(ph.getNroprestamo());
		p.setPrestamoNuevo(false);
		p.setSaldoPrestamo(ph.getSaldoPrestamo());
		p.setTarjeta(ph.getTarjeta());
		try {
			p = prestamoService.savePrstFromCancel(p, ph.getGplanta_id(), ph.getFtipoprestamo_id());
			prestamoHistRepository.delete(ph);
			model.addAttribute("prstCancelList", prestamoService.getPrestCancelados());
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			String clave = "fsolPrst";
			Ayuda help = ayudaService.getByClave(clave);
			model.addAttribute("help", help);				
			model.addAttribute("solicitudesList", solicitudPrestamoService.getdevueltasDeComision());
			model.addAttribute("cancelSuccessMessage", "El prestamo ha sido rehabilitado");
		} catch (Exception e) {
			model.addAttribute("cancelErrorMessage", e.getMessage());
		}

		try {
			model.addAttribute("prstNuevoForm", new PrstNuevoForm(prestamoService.getProxNroPrst()));			
		}
		catch(ParamNotFoundException pef) {
			model.addAttribute("cancelErrorMessage", "No se encontró prox. nro. de préstamo");
		}
		model.addAttribute("editMode", false);
		model.addAttribute("plantaList", gplantaService.getAllPlanta());
		model.addAttribute("prestamosList", prestamoService.getAllPrst());
		model.addAttribute("procname", "Intermedio");
		model.addAttribute("prstCancelTab", "active");
		
		return "prestamos/prestamos-view";
	}
	
	@GetMapping("/pdfPrstreport")
    public void prestReport(HttpServletResponse response) throws DocumentException, IOException, Exception{
		List<Prestamo> prstLst = new ArrayList<Prestamo>();
		
		DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy_HH-mm");
		String currDate = sdf.format(new Date());
		
		String mesLiquidacion = parametroService.getMesliquidacion();
        prstLst = prestamoService.getAllPrst();
		String headerKey = "Content-Disposition";
		String headerValue = "attachment; filename=prestamosreport_" + currDate + ".pdf";
		
		response.setHeader(headerKey, headerValue);
		PrestamosPdfExporter exporter = new PrestamosPdfExporter(prstLst, mesLiquidacion);
		exporter.export(response);
     }

}
