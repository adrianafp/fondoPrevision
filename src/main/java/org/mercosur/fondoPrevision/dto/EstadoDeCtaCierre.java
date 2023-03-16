package org.mercosur.fondoPrevision.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.mercosur.fondoPrevision.entities.Gplanta;
import org.mercosur.fondoPrevision.entities.Prestamo;

public class EstadoDeCtaCierre implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 8364365931652052601L;

	private Gplanta funcionario;
	
	private Long idfuncionario;
	
	private LocalDate fechaEgreso;
	
	private BigDecimal capitalIntegrado;
	
	private BigDecimal liquidacionfinal;
	
	private BigDecimal sueldoUltimoMes;
	
	private BigDecimal basico;
	
	private BigDecimal aporteFun;
	
	private BigDecimal aportePat;
	
	private BigDecimal aporteTotal;
	
	private BigDecimal importeAguinaldo;
	
	private BigDecimal sumaBasicos;
	
	private BigDecimal importeLicencia;
	
	private BigDecimal aporteFunAgui;
	
	private BigDecimal aportePatAgui;
	
	private BigDecimal aporteTotalSobreAguinaldo;
	
	private BigDecimal aporteFunLicencia;
	
	private BigDecimal aportePatLicencia;
	
	private BigDecimal aporteTotLicencia;
	
	private BigDecimal interesesporcolocaciones;
	
	private BigDecimal saldoPrestamos;
	
	private BigDecimal saldoCuenta;
	
	private List<Prestamo> lstPrst;
	
	public EstadoDeCtaCierre() {
		super();
	}

	public Long getIdfuncionario() {
		return idfuncionario;
	}

	public void setIdfuncionario(Long idfuncionario) {
		this.idfuncionario = idfuncionario;
	}

	public BigDecimal getCapitalIntegrado() {
		return capitalIntegrado;
	}

	public void setCapitalIntegrado(BigDecimal capitalIntegrado) {
		this.capitalIntegrado = capitalIntegrado;
	}

	public BigDecimal getLiquidacionfinal() {
		return liquidacionfinal;
	}

	public void setLiquidacionfinal(BigDecimal liquidacionfinal) {
		this.liquidacionfinal = liquidacionfinal;
	}

	public BigDecimal getAporteTotal() {
		return aporteTotal;
	}

	public void setAporteTotal(BigDecimal aporteTotal) {
		this.aporteTotal = aporteTotal;
	}

	public BigDecimal getInteresesporcolocaciones() {
		return interesesporcolocaciones;
	}

	public void setInteresesporcolocaciones(BigDecimal interesesporcolocaciones) {
		this.interesesporcolocaciones = interesesporcolocaciones;
	}

	public BigDecimal getSaldoPrestamos() {
		return saldoPrestamos;
	}

	public void setSaldoPrestamos(BigDecimal saldoPrestamos) {
		this.saldoPrestamos = saldoPrestamos;
	}

	public BigDecimal getSaldoCuenta() {
		return saldoCuenta;
	}

	public void setSaldoCuenta(BigDecimal saldoCuenta) {
		this.saldoCuenta = saldoCuenta;
	}

	public List<Prestamo> getLstPrst() {
		return lstPrst;
	}

	public void setLstPrst(List<Prestamo> lstPrst) {
		this.lstPrst = lstPrst;
	}

	public Gplanta getFuncionario() {
		return funcionario;
	}

	public void setFuncionario(Gplanta funcionario) {
		this.funcionario = funcionario;
	}

	public LocalDate getFechaEgreso() {
		return fechaEgreso;
	}

	public void setFechaEgreso(LocalDate fechaEgreso) {
		this.fechaEgreso = fechaEgreso;
	}

	public BigDecimal getAporteTotalSobreAguinaldo() {
		return aporteTotalSobreAguinaldo;
	}

	public void setAporteTotalSobreAguinaldo(BigDecimal aporteTotalSobreAguinaldo) {
		this.aporteTotalSobreAguinaldo = aporteTotalSobreAguinaldo;
	}

	public BigDecimal getSueldoUltimoMes() {
		return sueldoUltimoMes;
	}

	public void setSueldoUltimoMes(BigDecimal sueldoUltimoMes) {
		this.sueldoUltimoMes = sueldoUltimoMes;
	}

	public BigDecimal getImporteAguinaldo() {
		return importeAguinaldo;
	}

	public void setImporteAguinaldo(BigDecimal importeAguinaldo) {
		this.importeAguinaldo = importeAguinaldo;
	}

	public BigDecimal getAporteFun() {
		return aporteFun;
	}

	public void setAporteFun(BigDecimal aporteFun) {
		this.aporteFun = aporteFun;
	}

	public BigDecimal getAportePat() {
		return aportePat;
	}

	public void setAportePat(BigDecimal aportePat) {
		this.aportePat = aportePat;
	}

	public BigDecimal getAporteFunAgui() {
		return aporteFunAgui;
	}

	public void setAporteFunAgui(BigDecimal aporteFunAgui) {
		this.aporteFunAgui = aporteFunAgui;
	}

	public BigDecimal getAportePatAgui() {
		return aportePatAgui;
	}

	public void setAportePatAgui(BigDecimal aportePatAgui) {
		this.aportePatAgui = aportePatAgui;
	}

	public BigDecimal getAporteFunLicencia() {
		return aporteFunLicencia;
	}

	public void setAporteFunLicencia(BigDecimal aporteFunLicencia) {
		this.aporteFunLicencia = aporteFunLicencia;
	}

	public BigDecimal getAportePatLicencia() {
		return aportePatLicencia;
	}

	public void setAportePatLicencia(BigDecimal aportePatLicencia) {
		this.aportePatLicencia = aportePatLicencia;
	}

	public BigDecimal getAporteTotLicencia() {
		return aporteTotLicencia;
	}

	public void setAporteTotLicencia(BigDecimal aporteTotLicencia) {
		this.aporteTotLicencia = aporteTotLicencia;
	}

	public BigDecimal getImporteLicencia() {
		return importeLicencia;
	}

	public void setImporteLicencia(BigDecimal importeLicencia) {
		this.importeLicencia = importeLicencia;
	}

	public BigDecimal getBasico() {
		return basico;
	}

	public void setBasico(BigDecimal basico) {
		this.basico = basico;
	}

	public BigDecimal getSumaBasicos() {
		return sumaBasicos;
	}

	public void setSumaBasicos(BigDecimal sumaBasicos) {
		this.sumaBasicos = sumaBasicos;
	}

}
