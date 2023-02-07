package org.mercosur.fondoPrevision.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mercosur.fondoPrevision.dto.FuncionarioConSueldoMes;
import org.mercosur.fondoPrevision.entities.Gcargo;
import org.mercosur.fondoPrevision.entities.Gorganigrama;
import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.GvinculoFuncionarioCargo;
import org.mercosur.fondoPrevision.entities.Movimientos;
import org.mercosur.fondoPrevision.entities.Parametro;
import org.mercosur.fondoPrevision.entities.Saldos;
import org.mercosur.fondoPrevision.entities.SaldosHistoria;
import org.mercosur.fondoPrevision.entities.SueldoMes;
import org.mercosur.fondoPrevision.entities.TipoMovimiento;
import org.mercosur.fondoPrevision.entities.User;
import org.mercosur.fondoPrevision.exceptions.FuncionarioNoEncontradoException;
import org.mercosur.fondoPrevision.exceptions.TarjetaYaRegistradaException;
import org.mercosur.fondoPrevision.repository.FcodigosRepository;
import org.mercosur.fondoPrevision.repository.GcargoRepository;
import org.mercosur.fondoPrevision.repository.GorganigramaRepository;
import org.mercosur.fondoPrevision.repository.GplantaRepository;
import org.mercosur.fondoPrevision.repository.GplantahistRepository;
import org.mercosur.fondoPrevision.repository.GvinculoFuncionarioCargoRepository;
import org.mercosur.fondoPrevision.repository.MovimientosRepository;
import org.mercosur.fondoPrevision.repository.ParametroRepository;
import org.mercosur.fondoPrevision.repository.SaldosHistoriaRepository;
import org.mercosur.fondoPrevision.repository.SaldosRepository;
import org.mercosur.fondoPrevision.repository.SueldoMesRepository;
import org.mercosur.fondoPrevision.repository.TipoMovimientoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GplantaServiceImpl implements GplantaService {

	@Autowired
	GcargoRepository gcargoRepository;
	
	@Autowired
	GorganigramaRepository gorganigramaRepository;
	
	@Autowired
	GplantaRepository gplantaRepository;
	
	@Autowired
	GplantahistRepository gplantaHistRepository;

	@Autowired
	SaldosHistoriaRepository saldosHistoriaRepository;
	
	@Autowired
	ParametroRepository parametroRepository;
	
	@Autowired
	SaldosRepository saldosRepository;
	
	@Autowired
	SueldoMesRepository sueldomesRepository;
	
	@Autowired
	MovimientosRepository movimientosRepository;
	
	@Autowired
	TipoMovimientoRepository tipoMovimientoRepository;
		
	@Autowired
	FcodigosRepository fcodigosRepository;
	
	@Autowired
	GvinculoFuncionarioCargoRepository gvinculoRepository;
	
	@Autowired
	ParamService paramService;
	
	@Autowired
	UserService userService;
	
	@Autowired
	LogfondoService logfondoService;
	
	@Override
	public Iterable<Gplanta> getAllPlanta() {
		
		Iterable<Gplanta> lista = gplantaRepository.findAll();
		
		return lista;
	}

	@Override
	public Gplanta getFuncionarioByTarjeta(Integer tarjeta) throws FuncionarioNoEncontradoException {		
		return gplantaRepository.findByTarjeta(tarjeta);
	}

	@Override
	public Boolean checkFuncionarioByTarjeta(Integer tarjeta) throws FuncionarioNoEncontradoException {
		try {
			return gplantaRepository.findTarjeta(tarjeta);			
		}
		catch(Exception e) {
			throw new FuncionarioNoEncontradoException();
		}
	}

	private Boolean checkTarjetaNoRegistrada(Integer tarjeta) throws TarjetaYaRegistradaException {
		try {
			gplantaRepository.findTarjeta(tarjeta);
			throw new TarjetaYaRegistradaException();
		}
		catch(Exception e) {
			return true;
		}
	}
	
	@Override
	public Long getIdGplantaByTarjeta(Integer tarjeta) throws FuncionarioNoEncontradoException {
		Gplanta funcionario = gplantaRepository.findByTarjeta(tarjeta);
		
		return funcionario.getIdgplanta();
	}

	
	@Override
	public Gplanta createCuenta(Gplanta funcionario, Integer idcargo, Integer idgorganigrama, BigDecimal valorBasico, BigDecimal complemento) throws Exception {
		
		try {
			Gplanta func = funcionario;
			Optional<Gcargo> cargo = gcargoRepository.findById(idcargo);
			if(cargo.isPresent()) {
				funcionario.setGcargo(cargo.get());
			}
			else {
				throw new Exception("No fue posible acceder al cargo elegido");
			}
			Optional<Gorganigrama> optsector = gorganigramaRepository.findById(idgorganigrama);
			
			Gorganigrama sector = optsector.isPresent()? optsector.get() : null;
			if(sector == null) {
				throw new Exception("No se puede acceder al sector elegido!");
			}
			
			if(checkTarjetaNoRegistrada(funcionario.getTarjeta())) {
				funcionario.setUltimoIngreso(true);
				funcionario.setUnidad(sector.getUnidad());
				funcionario.setSector(sector.getSiglaSector());
				// se crea el registro el funcionario en planta
				func = gplantaRepository.save(funcionario);
				// chequeo que no exista registro en la tabla de saldos
				Optional<Saldos> optsaldos = saldosRepository.findByTarjeta(func.getTarjeta());
				if(optsaldos.isPresent()) {
					throw new Exception("el funcionario ya posee cuenta");
				}
				//objetos que se crearán en la BD
				Saldos saldos = new Saldos();
				SaldosHistoria saldoshist = new SaldosHistoria();
				Movimientos mov = new Movimientos();
				SueldoMes sueldomes = new SueldoMes();
				Calendar calendar = Calendar.getInstance();
				Date currentDate = calendar.getTime();
				java.sql.Date fecha = new java.sql.Date(currentDate.getTime());
				String mesLiquidacion = paramService.getMesliquidacion();
				BigDecimal aporteSec = BigDecimal.ZERO;
				BigDecimal aporteFunc = BigDecimal.ZERO;
				BigDecimal divisor = new BigDecimal("100");
				Iterable<Parametro> lstPar = parametroRepository.getSomeByDesc("Aporte");
				for(Parametro p : lstPar) {
					if(p.getDescripcion().contains("patronal")) {
						BigDecimal bascomp = valorBasico.add(complemento);
						aporteSec = bascomp.multiply(p.getValor()).divide(divisor);
					}
					if(p.getDescripcion().contains("funcionario")) {
						aporteFunc = valorBasico.multiply(p.getValor()).divide(divisor);
					}
				}
				BigDecimal aporteTot = aporteSec.add(aporteFunc);
				saldos.setCapitalDispActual(aporteTot);
				saldos.setCapitalDispAntes(new BigDecimal("0"));
				saldos.setCapitalIntegActual(aporteTot);
				saldos.setCapitalIntegAntes(new BigDecimal("0"));
				saldos.setFecha(fecha);
				saldos.setFuncionario(func);
				saldos.setMesliquidacion(mesLiquidacion);
				saldos.setNumerales(aporteTot);
				saldos.setTarjeta(func.getTarjeta());
				saldosRepository.save(saldos);
				
				saldoshist.setCapitalDispActual(aporteTot);
				saldoshist.setCapitalDispAntes(new BigDecimal("0"));
				saldoshist.setCapitalIntegActual(aporteTot);
				saldoshist.setCapitalIntegAntes(new BigDecimal("0"));
				saldoshist.setFecha(fecha);
				saldoshist.setGplanta_id(func.getIdgplanta());
				saldoshist.setMesliquidacion(mesLiquidacion);
				saldoshist.setMotivo("Apertura de Cuenta");
				saldoshist.setNumerales(aporteTot);
				saldoshist.setTarjeta(func.getTarjeta());
				saldosHistoriaRepository.save(saldoshist);
				
				sueldomes.setAniomes(mesLiquidacion);
				sueldomes.setFecha(fecha);
				sueldomes.setFuncionario(func);
				sueldomes.setMotivo("apertura de cuenta");
				sueldomes.setSueldomes(valorBasico);
				sueldomes.setComplemento(complemento);
				sueldomes.setTarjeta(func.getTarjeta());
				sueldomesRepository.save(sueldomes);
				
				TipoMovimiento tipmov = tipoMovimientoRepository.findByCodigoMovimiento(Short.valueOf("1"));
				mov.setFechaMovimiento(fecha);
				mov.setFuncionario(func);
				mov.setImporteCapSec(aporteSec);
				mov.setImporteIntFunc(aporteFunc);
				mov.setImporteMov(aporteTot);
				mov.setMesliquidacion(mesLiquidacion);
				mov.setTipoMovimiento(tipmov);
				mov.setCodigoMovimiento(Short.valueOf("1"));
				mov.setSaldoActual(aporteTot);
				mov.setSaldoAnterior(new BigDecimal("0"));
				mov.setTarjeta(func.getTarjeta());
				mov.setObservaciones("Apertura de Cuenta");
				movimientosRepository.save(mov);
								
				try {
					logfondoService.agregarLog("Apertura de Cuenta");
				}
				catch(Exception e) {
					throw new Exception(e.getMessage());
				}
			}
			
			return func;

		}
		catch(TarjetaYaRegistradaException tyre) {
			throw new Exception(tyre.getMessage());
		}
	}

	@Override
	public String crearCuentaDeFuncionarioDePlanta(Gplanta funcionario) throws Exception {
		//objetos que se crearán en la BD
		Saldos saldos = new Saldos();
		Movimientos mov = new Movimientos();
		SueldoMes sueldomes = new SueldoMes();
		Calendar calendar = Calendar.getInstance();
		Date currentDate = calendar.getTime();
		java.sql.Date fecha = new java.sql.Date(currentDate.getTime());
		String mesLiquidacion = "202005";
		
		BigDecimal basico = BigDecimal.ZERO;
		BigDecimal aporteSec = BigDecimal.ZERO;
		BigDecimal aporteFunc = BigDecimal.ZERO;
		BigDecimal divisor = new BigDecimal("100");
		
		Optional<Saldos> sf = saldosRepository.findByTarjeta(funcionario.getTarjeta());
		if(sf.isPresent()) {
			throw new Exception("El funcionario ya tiene la cuenta creada!!!");
		}
		Optional<Gcargo> cargo = gcargoRepository.findById(funcionario.getGcargo().getIdgcargo());
		if(cargo.isPresent()) {
			basico = cargo.get().getBasico();
		}
		else {
			throw new Exception("No fue posible encontrar el Grado en la BD.");
		}

		Iterable<Parametro> lstPar = parametroRepository.getSomeByDesc("Aporte");
		for(Parametro p : lstPar) {
			if(p.getDescripcion().contains("patronal")) {
				aporteSec = basico.multiply(p.getValor()).divide(divisor);
			}
			if(p.getDescripcion().contains("funcionario")) {
				aporteFunc = basico.multiply(p.getValor()).divide(divisor);
			}
		}
		BigDecimal aporteTot = aporteSec.add(aporteFunc);
		saldos.setCapitalDispActual(aporteTot);
		saldos.setCapitalDispAntes(new BigDecimal("0"));
		saldos.setCapitalIntegActual(aporteTot);
		saldos.setCapitalIntegAntes(new BigDecimal("0"));
		saldos.setFecha(fecha);
		saldos.setFuncionario(funcionario);
		saldos.setMesliquidacion(mesLiquidacion);
		saldos.setNumerales(aporteTot);
		saldos.setTarjeta(funcionario.getTarjeta());
		saldosRepository.save(saldos);
		
		sueldomes.setAniomes(mesLiquidacion);
		sueldomes.setFecha(fecha);
		sueldomes.setFuncionario(funcionario);
		sueldomes.setMotivo("apertura de cuenta");
		sueldomes.setSueldomes(basico);
		sueldomes.setTarjeta(funcionario.getTarjeta());
		sueldomesRepository.save(sueldomes);
		
		TipoMovimiento tipmov = tipoMovimientoRepository.findByCodigoMovimiento(Short.valueOf("1"));
		mov.setFechaMovimiento(fecha);
		mov.setFuncionario(funcionario);
		mov.setImporteCapSec(aporteSec);
		mov.setImporteIntFunc(aporteFunc);
		mov.setImporteMov(aporteTot);
		mov.setMesliquidacion(mesLiquidacion);
		mov.setTipoMovimiento(tipmov);
		mov.setCodigoMovimiento(Short.valueOf("1"));
		mov.setSaldoActual(aporteTot);
		mov.setSaldoAnterior(new BigDecimal("0"));
		mov.setTarjeta(funcionario.getTarjeta());
		mov.setObservaciones("Apertura de Cuenta");
		movimientosRepository.save(mov);
				
		try {
			logfondoService.agregarLog("Apertura de Cuenta");
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
		
		return "Ejecución Exitosa";

	}

	@Override
	public String updateCuentaNueva(Gplanta funcionario, BigDecimal valorBasico, BigDecimal complemento)
			throws Exception {
		
		List<Movimientos> lstMovs = movimientosRepository.findAllByTarjeta(funcionario.getTarjeta());
		if(lstMovs.size() == 1 && lstMovs.get(0).getCodigoMovimiento() == 1) {
			Movimientos m = lstMovs.get(0);
			String mesliquidacion = m.getMesliquidacion();
			BigDecimal aporteSec = BigDecimal.ZERO;
			BigDecimal aporteFunc = BigDecimal.ZERO;
			BigDecimal divisor = new BigDecimal("100");
			BigDecimal aporteTot = BigDecimal.ZERO;
			
			Iterable<Parametro> lstPar = parametroRepository.getSomeByDesc("Aporte");
			for(Parametro p : lstPar) {
				if(p.getDescripcion().contains("patronal")) {
					BigDecimal bascomp = valorBasico.add(complemento);
					aporteSec = bascomp.multiply(p.getValor()).divide(divisor);
				}
				if(p.getDescripcion().contains("funcionario")) {
					aporteFunc = valorBasico.multiply(p.getValor()).divide(divisor);
				}
			}
			aporteTot = aporteSec.add(aporteFunc);
			
			//objetos que se acutalizan en la BD

			m.setImporteCapSec(aporteSec);
			m.setImporteIntFunc(aporteFunc);
			m.setImporteMov(aporteTot);
			m.setSaldoActual(aporteTot);
			m.setSaldoAnterior(new BigDecimal("0"));
			m.setObservaciones("Apertura de Cuenta");
			movimientosRepository.save(m);

			Optional<Saldos> optsaldos = saldosRepository.findByTarjeta(funcionario.getTarjeta());
			if(optsaldos.isPresent()) {
				Saldos saldos = optsaldos.get();
				saldos.setCapitalDispActual(aporteTot);
				saldos.setCapitalDispAntes(new BigDecimal("0"));
				saldos.setCapitalIntegActual(aporteTot);
				saldos.setCapitalIntegAntes(new BigDecimal("0"));
				saldos.setNumerales(aporteTot);
				saldosRepository.save(saldos);
			}
						
			SaldosHistoria saldoshist = saldosHistoriaRepository.getUltimoByTarjeta(funcionario.getTarjeta());
			saldoshist.setCapitalDispActual(aporteTot);
			saldoshist.setCapitalDispAntes(new BigDecimal("0"));
			saldoshist.setCapitalIntegActual(aporteTot);
			saldoshist.setCapitalIntegAntes(new BigDecimal("0"));
			saldoshist.setMotivo("Apertura de Cuenta");
			saldoshist.setNumerales(aporteTot);
			saldosHistoriaRepository.save(saldoshist);
			
			SueldoMes sueldomes = sueldomesRepository.getSueldoMesByAniomesAndTarjeta(mesliquidacion, funcionario.getTarjeta());			
			sueldomes.setMotivo("apertura de cuenta");
			sueldomes.setSueldomes(valorBasico);
			sueldomes.setComplemento(complemento);
			sueldomesRepository.save(sueldomes);
										
			try {
				logfondoService.agregarLog("Apertura de Cuenta", "Actualización de valores");
			}
			catch(Exception e) {
				throw new Exception(e.getMessage());
			}
			return "Ejecución exitosa";
		}
		else {
			throw new Exception("La cuenta no cumple las condiciones para ejecutar la actualización");
		}
	}

	@Override
	public void actualizarUltimosIngresos() throws Exception {
		
		gplantaRepository.updateUltimosIngresos();
		
	}

	@Override
	public List<Integer> getTarjetasIngresos(Date fecha) throws Exception {
		return gplantaRepository.getTarjetasIngresos(fecha);
	}

	@Override
	public List<Gplanta> getAllByUnidad(String unidad) throws Exception {
		
		return gplantaRepository.getAllByUnidad(unidad);
	}

	@Override
	public FuncionarioConSueldoMes getFuncionarioConSueldoMes(Integer tarjeta) throws FuncionarioNoEncontradoException {
		Gplanta gp = gplantaRepository.findByTarjeta(tarjeta);
		BigDecimal basico = BigDecimal.ZERO;
		BigDecimal complemento = BigDecimal.ZERO;
		String mesliq = parametroRepository.findByMesliquidacionDistinct();
		
			basico = gp.getGcargo().getBasico();
			complemento = gp.getGcargo().getComplemento();
			List<SueldoMes> lstsueldos = gp.getLstsueldos();
			if(lstsueldos != null) {
				for(SueldoMes sm : lstsueldos) {
					if(sm.getAniomes().equals(mesliq)) {
						basico = sm.getSueldomes();
						complemento = sm.getComplemento();
					}
				}
			}
			FuncionarioConSueldoMes funcsm = new FuncionarioConSueldoMes(gp.getIdgplanta(), gp.getTarjeta(),
					gp.getNombre(), basico, complemento, gp.getIngreso(), gp.getGcargo().getDescripCargo());
			return funcsm;
	}

	@Override
	public Gplanta actualizarCargo(Long idfuncionario, Integer idcargonuevo, String fechaAcceso, 
			String firstBasico, String firstComplemento) throws Exception {

		try {
			Gplanta funcionario = gplantaRepository.getOne(idfuncionario);
			
			Gcargo newCargo = gcargoRepository.getOne(idcargonuevo);
		
			funcionario.setGcargo(newCargo);
			funcionario = gplantaRepository.save(funcionario);
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Date fecha = sdf.parse(fechaAcceso);
			Calendar cal = Calendar.getInstance();
			cal.setTime(fecha);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Date yesterday = cal.getTime();
			
			GvinculoFuncionarioCargo vinculo = gvinculoRepository.getLastByTarjeta(funcionario.getTarjeta());
			vinculo.setFecha_final(yesterday);
			vinculo = gvinculoRepository.save(vinculo);
			
			GvinculoFuncionarioCargo newvinculo = new GvinculoFuncionarioCargo();
			newvinculo.setFecha_inicial(fecha);
			newvinculo.setGcargos_id(newCargo.getIdgcargo());
			newvinculo.setGplanta_id(funcionario.getIdgplanta());
			newvinculo.setTarjeta(funcionario.getTarjeta());
			newvinculo = gvinculoRepository.save(newvinculo);
			
			String mesliquidacion = paramService.getMesliquidacion();
			SueldoMes sueldoMes = new SueldoMes();
			sueldoMes.setAniomes(mesliquidacion);
			sueldoMes.setComplemento(new BigDecimal(firstComplemento));
			sueldoMes.setFecha(fecha);
			sueldoMes.setFuncionario(funcionario);
			sueldoMes.setMotivo("Cambio de Cargo");
			sueldoMes.setSueldomes(new BigDecimal(firstBasico));
			sueldoMes.setTarjeta(funcionario.getTarjeta());
			sueldoMes = sueldomesRepository.save(sueldoMes);

			return funcionario;
		}
		catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}

	@Override
	public Integer getLastTarjeta() throws Exception {
		Integer ultimo = gplantaRepository.getLastTarjeta();
		Integer ultimohist = gplantaHistRepository.getLastTarjeta();
		
		return (ultimo > ultimohist? ultimo + 1: ultimohist + 1);
	}

	@Override
	public List<Gplanta> getAllNuevaCuentas() throws Exception {
		
		String mesLiquidacion = paramService.getMesliquidacion();
		List<Movimientos> lstMovs = movimientosRepository.getByCodigosAndMes("1", mesLiquidacion);
		List<Integer> lstT = lstMovs.stream().map(x -> x.getTarjeta()).collect(Collectors.toList());
		String tarQL = "";
		for(Integer t:lstT){
			tarQL += tarQL.equals("")?t:", "+t;
		}

		List<Gplanta> lstNuevos = gplantaRepository.getFuncsInGroup(tarQL);
		
		return lstNuevos;
	}

	@Override
	public String deleteCuentaNueva(Integer tarjeta) throws Exception {
		User user = userService.getUserByTarjeta(tarjeta);
		saldosRepository.deleteByTarjeta(tarjeta);
		saldosHistoriaRepository.deleteByTarjeta(tarjeta);
		movimientosRepository.deleteAllByTarjeta(tarjeta);
		sueldomesRepository.deleteByTarjeta(tarjeta);
		gvinculoRepository.deleteByTarjeta(tarjeta);
		userService.deleteUser(user.getId());
		gplantaRepository.deleteByTarjeta(tarjeta);
		return "Cuenta Eliminada";
	}

}
