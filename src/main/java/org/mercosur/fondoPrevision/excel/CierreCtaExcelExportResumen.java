package org.mercosur.fondoPrevision.excel;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.mercosur.fondoPrevision.dto.EstadoDeCtaCierre;
import org.mercosur.fondoPrevision.repository.MovimientosRepository;
import org.springframework.beans.factory.annotation.Autowired;

public class CierreCtaExcelExportResumen {
	
	@Autowired
	MovimientosRepository movimientosRepository;
	
	private XSSFWorkbook workbook;
	private XSSFSheet sheet;
	private EstadoDeCtaCierre estadoCta;
	private int rowcount;
	private String fegreso;
	private String mesDistrib;
	private String mesLiqEgreso;
	private String ultimoMesLiq;
	
	public CierreCtaExcelExportResumen(EstadoDeCtaCierre estadoCta, String mesDistrib, String mesLiqEgreso, String ultimoMesLiq) {
		this.workbook = new XSSFWorkbook();
		this.setSheet(workbook.createSheet("CierreDeCuenta"));
		this.estadoCta = estadoCta;
		this.rowcount = 5;
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		this.fegreso = dtf.format(estadoCta.getFechaEgreso());
		this.mesDistrib = "01/" + mesDistrib.substring(4) + "/" + mesDistrib.substring(0, 4);
		this.mesLiqEgreso = mesLiqEgreso;
		this.ultimoMesLiq = ultimoMesLiq;

	}
	
	private void writeTitleRow() {
		Row row = sheet.createRow(2);

		CellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setFontHeight(16);
		style.setFont(font);

		Cell cell = row.createCell(0);
		cell.setCellValue("FONDO DE PREVISIÓN DE LOS FUNCIONARIOS DE LA SECRETARÍA DEL MERCOSUR");
		cell.setCellStyle(style);
		
	}

	private void writeSubTitleRow() {
		Row row = sheet.createRow(rowcount++);

		CellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setFontHeight(16);
		style.setFont(font);

		Cell cell = row.createCell(2);
		cell.setCellValue("LIQUIDACION FINAL CUENTA INDIVIDUAL " + estadoCta.getFuncionario().getNombre());
		cell.setCellStyle(style);
		
		row = sheet.createRow(rowcount++);
		cell = row.createCell(2);
		cell.setCellValue("FECHA DE EGRESO " + fegreso);
		cell.setCellStyle(style);
	}
	
	private void writeDataRows() {

		rowcount = 11;
		Row row = sheet.createRow(rowcount++);
		
		CellStyle styb =workbook.createCellStyle();
		XSSFFont fontb = workbook.createFont();
		fontb.setBold(true);
		fontb.setFontHeight(12);
		styb.setFont(fontb);
		
		CellStyle style = workbook.createCellStyle();
		XSSFFont font = workbook.createFont();
		font.setBold(true);
		font.setUnderline(FontUnderline.SINGLE);
		font.setFontHeight(12);
		style.setFont(font);
		
		Cell cell = row.createCell(0);
		cell.setCellValue("Cuenta de Integracion");
		cell.setCellStyle(style);
				
		LocalDate fechaEgreso = estadoCta.getFechaEgreso();
		
		LocalDate mesMenosUno = fechaEgreso.minusDays(fechaEgreso.getDayOfMonth());
		
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		String fMenosUno = dtf.format(mesMenosUno);

		row = sheet.createRow(rowcount);
		cell = row.createCell(1);
		if(this.ultimoMesLiq.equals(this.mesLiqEgreso)) {
			cell.setCellValue("Saldo");
		}
		else {
			cell.setCellValue("Saldo al " + fMenosUno);
		}
		
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getCapitalIntegrado().doubleValue());
		cell.setCellStyle(styb);
		
		rowcount += 2;
		
		row = sheet.createRow(rowcount++);
		cell = row.createCell(0);
		cell.setCellValue("Aporte del último mes hasta el " + fegreso);
		cell.setCellStyle(style);
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getAporteTotal().doubleValue());
		cell.setCellStyle(style);
		
				
		rowcount += 2;
		String inicioAguinaldo;
		if(fechaEgreso.getMonth().compareTo(Month.JUNE) <= 0) {
			inicioAguinaldo = "01/01/" + String.valueOf(fechaEgreso.getYear());
		}
		else {
			inicioAguinaldo = "01/07/" + String.valueOf(fechaEgreso.getYear());
		}
		
		row = sheet.createRow(rowcount++);
		cell = row.createCell(0);
		cell.setCellValue("Aporte Aguinaldo período " + inicioAguinaldo + " - " + fegreso);
		cell.setCellStyle(style);
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getAporteTotalSobreAguinaldo().doubleValue());
		cell.setCellStyle(style);
		
		
		if(estadoCta.getImporteLicencia().compareTo(BigDecimal.ZERO) > 0) {
			rowcount += 2;
			row = sheet.createRow(rowcount++);
			cell = row.createCell(0);
			cell.setCellValue("Aporte sobre licencia no gozada");
			cell.setCellStyle(style);
			
			String slicencia = String.valueOf(estadoCta.getImporteLicencia().doubleValue());
			
			row = sheet.createRow(rowcount);
			cell = row.createCell(1);
			cell.setCellValue("USD " + slicencia);
			cell = row.createCell(5);
			cell.setCellValue(estadoCta.getAporteTotLicencia().doubleValue());
			cell.setCellStyle(styb);
		}
		
		rowcount += 2;
		row = sheet.createRow(rowcount);
		cell = row.createCell(0);
		cell.setCellValue("Menos saldo Préstamos ");
		cell.setCellStyle(style);
		
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getSaldoPrestamos().doubleValue());
		cell.setCellStyle(styb);
		
		rowcount += 2;
		row = sheet.createRow(rowcount);
		cell = row.createCell(0);
		cell.setCellValue("Distribución de intereses período " + this.mesDistrib + " - " + fegreso);
		cell.setCellStyle(style);
		
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getInteresesporcolocaciones().doubleValue());
		cell.setCellStyle(styb);
		
		rowcount += 2;
		row = sheet.createRow(rowcount);
		cell = row.createCell(2);
		cell.setCellValue("Neto a Cobrar USD ");
		cell.setCellStyle(style);
		
		cell = row.createCell(5);
		cell.setCellValue(estadoCta.getSaldoCuenta().doubleValue());
		cell.setCellStyle(styb);
	
	}
	
	public void export(HttpServletResponse response) throws IOException {

		writeTitleRow();
		writeSubTitleRow();
		writeDataRows();

		CellStyle style = workbook.createCellStyle();
		style.setBorderTop(BorderStyle.MEDIUM);
		rowcount += 4;
		Row row = sheet.createRow(rowcount);
		Cell cell = row.createCell(2);
		cell.setCellStyle(style);
		cell = row.createCell(3);
		cell.setCellValue("Por Comisión Administradora del Fondo De Previsión");
		cell.setCellStyle(style);
		cell = row.createCell(4);
		cell.setCellStyle(style);
		cell = row.createCell(5);
		cell.setCellStyle(style);
		
		
		
		ServletOutputStream outputStream = response.getOutputStream();
		workbook.write(outputStream);
		workbook.close();
		outputStream.close();
		
	}
	
	public XSSFWorkbook getWorkbook() {
		return workbook;
	}
	public void setWorkbook(XSSFWorkbook workbook) {
		this.workbook = workbook;
	}

	public XSSFSheet getSheet() {
		return sheet;
	}

	public void setSheet(XSSFSheet sheet) {
		this.sheet = sheet;
	}

	public EstadoDeCtaCierre getEstadoCta() {
		return estadoCta;
	}

	public void setEstadoCta(EstadoDeCtaCierre estadoCta) {
		this.estadoCta = estadoCta;
	}

	public String getFegreso() {
		return fegreso;
	}

	public void setFegreso(String fegreso) {
		this.fegreso = fegreso;
	}

	public String getMesDistrib() {
		return mesDistrib;
	}

	public void setMesDistrib(String mesDistrib) {
		this.mesDistrib = mesDistrib;
	}

	public String getMesLiqEgreso() {
		return mesLiqEgreso;
	}

	public void setMesLiqEgreso(String mesLiqEgreso) {
		this.mesLiqEgreso = mesLiqEgreso;
	}

	public String getUltimoMesLiq() {
		return ultimoMesLiq;
	}

	public void setUltimoMesLiq(String ultimoMesLiq) {
		this.ultimoMesLiq = ultimoMesLiq;
	}

}
