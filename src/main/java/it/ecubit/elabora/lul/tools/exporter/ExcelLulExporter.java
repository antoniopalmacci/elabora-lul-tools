package it.ecubit.elabora.lul.tools.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.apache.commons.lang3.tuple.Pair;


import org.springframework.stereotype.Component;

import it.ecubit.elabora.lul.tools.enums.AbsenceType;
import it.ecubit.elabora.lul.tools.enums.Month;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import it.ecubit.elabora.lul.tools.utils.NationalHolidays;

import lombok.NonNull;

	@Component
	public class ExcelLulExporter {

		private static final DecimalFormat EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER = new DecimalFormat("0.00");

		public void export(@NonNull Map<String, List<MonthlyReport>> reportsForMonthMap,
					@NonNull Path storageDirPath,
					@NonNull String filename) {

		Workbook wb = new XSSFWorkbook();

		List<String> monthYearsList = new ArrayList<>(reportsForMonthMap.keySet());
		monthYearsList.sort(new MonthYearStringComparator());

		monthYearsList.forEach(monthYear -> {
			Sheet sheet = wb.createSheet(monthYear);
			sheet.setDisplayGridlines(false);
			createHeader(wb, sheet);

			List<MonthlyReport> monthlyReports = reportsForMonthMap.get(monthYear);
			int startRow = 1;

			for (MonthlyReport monthlyReport : monthlyReports) {
				startRow = createRowsForEmployeeMonthlyReport(wb, sheet, monthlyReport, startRow);
			}
		});

		Sheet templateSheet = wb.createSheet(WorkbookInfo.TEMPLATE_SHEET_NAME);
		templateSheet.setDisplayGridlines(false);
		createHeader(wb, templateSheet);

		Path outputPath = storageDirPath.resolve(filename);

		final String RED = "\u001B[31m";
		final String YELLOW = "\u001B[33m";
		final String RESET = "\u001B[0m";

		try (OutputStream fileOut = new FileOutputStream(outputPath.toFile())) {
			wb.write(fileOut);
			System.out.println("File Excel generato correttamente: " + outputPath);

		} catch (FileNotFoundException e) {
			System.err.println(YELLOW +
				"\n============================================================\n" +
				"   ERRORE: IL FILE È APERTO IN EXCEL\n" +
				"------------------------------------------------------------\n" +
				"   Il file \"" + filename + "\" non può essere sovrascritto.\n" +
				"   Chiudi il file Excel e riprova l'esportazione.\n" +
				"============================================================\n" +
				RESET
			);
			return;

		} catch (IOException e) {
			System.err.println(RED +
				"\n============================================================\n" +
				"   ERRORE DI SCRITTURA DEL FILE EXCEL\n" +
				"------------------------------------------------------------\n" +
				"   Dettagli: " + e.getMessage() + "\n" +
				"============================================================\n" +
				RESET
			);
		}
	}


	private void createHeader(@NonNull Workbook wb, @NonNull Sheet sheet) {
		CellStyle headerStyle = createHeaderStyle(wb);
		Row headerRow = sheet.createRow(0);
		headerRow.setHeightInPoints(25);
		for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {
			Cell headerCell = headerRow.createCell(column);
			headerCell.setCellStyle(headerStyle);
			headerCell.setCellValue(WorkbookInfo.SHEET_COLUMN_NAMES[column]);
			sheet.setColumnWidth(column, WorkbookInfo.SHEET_COLUMN_WIDTHS[column]);
		}
	}

	// ------------------------------------------------------------
	//  Utility: converte indice → lettera colonna Excel (0=A, 1=B...)
	// ------------------------------------------------------------
	private static String columnIndexToLetter(int index) {
		StringBuilder sb = new StringBuilder();
		while (index >= 0) {
			sb.insert(0, (char) ('A' + (index % 26)));
			index = (index / 26) - 1;
		}
		return sb.toString();
	}

	// ------------------------------------------------------------
	//  Mappa lettera colonna Excel → indice colonna
	// ------------------------------------------------------------
	private static final Map<String, Integer> COLUMN_MAP = new HashMap<>();

	static {
		for (int i = 0; i < WorkbookInfo.SHEET_COLUMN_NAMES.length; i++) {
			String letter = columnIndexToLetter(i);
			COLUMN_MAP.put(letter, i);
		}
	}

	// ------------------------------------------------------------
	//  Funzione principale
	// ------------------------------------------------------------
	private int createRowsForEmployeeMonthlyReport(
			@NonNull Workbook wb,
			@NonNull Sheet sheet,
			@NonNull MonthlyReport monthlyReport,
			int startRow) {

		AtomicInteger counter = new AtomicInteger(0);
		final CellStyle dataCellStyle = createDataCellStyle(wb);
		final Month month = monthlyReport.getMonth();
		final int year = monthlyReport.getYear();
		final Employee employee = monthlyReport.getEmployee();
		final DateTimeFormatter fullDayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		monthlyReport.getDailyReports().forEach(dailyReport -> {

			if (!NationalHolidays.isPublicHoliday(month, dailyReport.getDay(), year)) {

				Row row = sheet.createRow(startRow + counter.get());
				row.setHeightInPoints(12);
				counter.incrementAndGet();

				// ------------------------------------------------------------
				// 1) Pre-calcolo: somma minuti per colonna Excel (dinamico)
				// ------------------------------------------------------------
				Map<Integer, Integer> absenceMinutesByColumn = new HashMap<>();

				if (dailyReport.getAbscenceMinutes() != null) {
					for (Pair<AbsenceType, Integer> pair : dailyReport.getAbscenceMinutes()) {

						AbsenceType type = pair.getLeft();
						Integer minutes = pair.getRight();

						if (type == null || minutes == null || minutes <= 0) {
							continue;
						}

						String colLetter = type.getColumnName();
						if (colLetter == null) {
							continue;
						}

						Integer colIndex = COLUMN_MAP.get(colLetter);
						if (colIndex == null) {
							continue;
						}

						absenceMinutesByColumn.merge(colIndex, minutes, Integer::sum);
					}
				}

				// ------------------------------------------------------------
				// 2) Scrittura colonne
				// ------------------------------------------------------------
				for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {

					Cell cell = row.createCell(column);
					cell.setCellStyle(dataCellStyle);

					// --- Se la colonna è una colonna di assenza, la gestiamo dinamicamente ---
					if (absenceMinutesByColumn.containsKey(column)) {
						int minutes = absenceMinutesByColumn.get(column);
						cell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
						cell.setCellValue(minutes);
						continue;
					}

					// --- Altrimenti gestiamo le colonne NON assenze ---
					switch (column) {

						case 0: // Cognome
							cell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							cell.setCellValue(employee.getSurname());
							break;

						case 1: // Nome
							cell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							cell.setCellValue(employee.getName());
							break;

						case 2: // Giorno (data completa)
							LocalDate date = LocalDate.of(year, month.getMonthInYear(), dailyReport.getDay());
							cell.setCellValue(fullDayFormatter.format(date));
							break;

						case 3: // Presenza
							cell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							cell.setCellValue(dailyReport.isPresent() ? 1 : 0);
							break;

						case 25: // ore lavorate
							cell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							cell.setCellValue(EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER.format(
									dailyReport.getTotWorkingHours()));
							break;

						case 26: // ore non lavorate
							cell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							cell.setCellValue(EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER.format(
									dailyReport.getTotNonWorkingHours()));
							break;

						case 27: // Totale ore
							float totHours = dailyReport.getTotWorkingHours() + dailyReport.getTotNonWorkingHours();
							cell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							cell.setCellValue(EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER.format(totHours));
							break;

						case 28: // Codice Fiscale
							cell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							cell.setCellValue(employee.getFiscalCode());
							break;

						default:
							// tutte le altre colonne non usate rimangono vuote
							cell.setCellValue("");
							break;
					}
				}
			}
		});

		return startRow + counter.get();
	}

	private CellStyle createHeaderStyle(@NonNull Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
		style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 11);
		font.setFontName("Calibri");
		font.setBold(true);
		style.setFont(font);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		style.setWrapText(true);
		return style;
	}

	private CellStyle createDataCellStyle(@NonNull Workbook wb) {
		CellStyle style = wb.createCellStyle();
		style.setFillBackgroundColor(IndexedColors.WHITE.getIndex());
		Font font = wb.createFont();
		font.setFontHeightInPoints((short) 11);
		font.setFontName("Calibri");
		font.setBold(false);
		style.setFont(font);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderBottom(BorderStyle.THIN);
		style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
		style.setRightBorderColor(IndexedColors.BLACK.getIndex());
		style.setTopBorderColor(IndexedColors.BLACK.getIndex());
		style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
		style.setAlignment(HorizontalAlignment.CENTER);
		style.setVerticalAlignment(VerticalAlignment.CENTER);
		return style;
	}

}
