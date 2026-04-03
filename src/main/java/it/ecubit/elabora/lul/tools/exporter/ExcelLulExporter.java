package it.ecubit.elabora.lul.tools.exporter;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
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
import it.ecubit.elabora.lul.tools.model.DailyReport;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import it.ecubit.elabora.lul.tools.zucchetti.PdfToExcelProcessor.ProgressListener;
import lombok.NonNull;
import java.util.function.BiConsumer;

@Component
public class ExcelLulExporter {

	private ProgressListener progressListener;

	public void setProgressListener(ProgressListener listener) {
		this.progressListener = listener;
	}

	private void updateProgress(int percent) {
		if (progressListener != null) {
			progressListener.onProgress(percent);
		}
	}

	public boolean export(@NonNull Map<String, List<MonthlyReport>> reportsForMonthMap,
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

			// ORDINAMENTO ALFABETICO PER COGNOME + NOME
			monthlyReports.sort(Comparator.comparing(
					r -> (r.getEmployee().getSurname() + " " + r.getEmployee().getName()).toLowerCase()));

			int total = monthlyReports.size();
			int index = 0;

			int startRow = 1;
			for (MonthlyReport monthlyReport : monthlyReports) {

				startRow = createRowsForEmployeeMonthlyReport(wb, sheet, monthlyReport, startRow);

				index++;
				int percent = 20 + (int) ((index / (double) total) * 80);
				updateProgress(percent);
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
					RESET);
			return false;

		} catch (IOException e) {
			System.err.println(RED +
					"\n============================================================\n" +
					"   ERRORE DI SCRITTURA DEL FILE EXCEL\n" +
					"------------------------------------------------------------\n" +
					"   Dettagli: " + e.getMessage() + "\n" +
					"============================================================\n" +
					RESET);
			return false;
		}
		return true;
	}

	private void createHeader(@NonNull Workbook wb, @NonNull Sheet sheet) {
		CellStyle headerStyle = createHeaderStyle(wb);
		Row headerRow = sheet.createRow(0);
		headerRow.setHeightInPoints(30);
		for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {
			Cell headerCell = headerRow.createCell(column);
			headerCell.setCellStyle(headerStyle);
			headerCell.setCellValue(WorkbookInfo.SHEET_COLUMN_NAMES[column]);
			sheet.setColumnWidth(column, WorkbookInfo.SHEET_COLUMN_WIDTHS[column]);
		}
	}

	// ------------------------------------------------------------
	// Utility: converte indice → lettera colonna Excel (0=A, 1=B...)
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
	// Mappa lettera colonna Excel → indice colonna
	// ------------------------------------------------------------
	private static final Map<String, Integer> COLUMN_MAP = new HashMap<>();

	static {
		for (int i = 0; i < WorkbookInfo.SHEET_COLUMN_NAMES.length; i++) {
			String letter = columnIndexToLetter(i);
			COLUMN_MAP.put(letter, i);
		}
	}

	// ------------------------------------------------------------
	// Funzione principale
	// ------------------------------------------------------------
	private int createRowsForEmployeeMonthlyReport(
			@NonNull Workbook wb,
			@NonNull Sheet sheet,
			@NonNull MonthlyReport monthlyReport,
			int startRow) {

		AtomicInteger dayCounter = new AtomicInteger(0); // avanza sempre
		AtomicInteger rowCounter = new AtomicInteger(0); // avanza solo se scrivi la riga

		// Stili indipendenti
		final CellStyle left = createLeftStyle(wb);
		final CellStyle center = createCenterStyle(wb);
		final CellStyle twoDecimalsStyle = createTwoDecimalsStyle(wb);

		final Month month = monthlyReport.getMonth();
		final int year = monthlyReport.getYear();
		final Employee employee = monthlyReport.getEmployee();
		final DateTimeFormatter fullDayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		List<DailyReport> dailyReports = monthlyReport.getDailyReports();

		// ============================================================
		// Funzione interna per scrivere una cella in base alla colonna
		// ============================================================
		BiConsumer<Cell, Integer> writeCell = (cell, column) -> {

			DailyReport dr = null;
			if (dailyReports != null && !dailyReports.isEmpty())
				dr = dailyReports.get(dayCounter.get() - 1);

			switch (column) {

				case 0: // Cognome
					cell.setCellStyle(left);
					cell.setCellValue(employee.getSurname());
					break;

				case 1: // Nome
					cell.setCellStyle(left);
					cell.setCellValue(employee.getName());
					break;

				case 2: // Data
					cell.setCellStyle(center);
					LocalDate date = LocalDate.of(year, month.getMonthInYear(),
							(dr != null) ? dr.getDay() : 1);
					cell.setCellValue(fullDayFormatter.format(date));
					break;

				case 3: // Presenza
					cell.setCellStyle(center);
					if (dr != null) {
						cell.setCellValue(dr.isPresent() ? 1 : 0);
					} else {
						cell.setCellValue(0);
					}
					break;

				case 17: // Straordinari (STRAORDINARIA + FESTIVA)
					cell.setCellStyle(center);
					if (dr != null) {
						int extraMinutes = dr.getExtraMinutes();
						if (extraMinutes > 0) {
							cell.setCellValue(extraMinutes);
						}
					}
					break;

				case 25: // Ore lavorate (ordinarie + straordinarie/festive)
					cell.setCellStyle(twoDecimalsStyle);
					if (dr != null) {
						cell.setCellValue(dr.getWorkedHours());
					}
					break;

				case 26: // Ore non lavorate
					cell.setCellStyle(twoDecimalsStyle);
					if (dr != null) {
						cell.setCellValue(dr.getNonWorkedHours());
					}
					break;

				case 27: // Totale ore (ordinarie + straordinarie/festive + assenze)
					cell.setCellStyle(twoDecimalsStyle);
					if (dr != null) {
						cell.setCellValue(dr.getTotalHours());
					}
					break;

				case 28: // Codice fiscale
					cell.setCellStyle(center);
					cell.setCellValue(employee.getFiscalCode());
					break;

				case 29: // Sede
					cell.setCellStyle(center);
					cell.setCellValue(employee.getHeadquarters());
					break;

				case 30: // Matricola
					cell.setCellStyle(center);
					cell.setCellValue(employee.getEmployeeCode());
					break;

				case 31: // Livello
					cell.setCellStyle(left);
					cell.setCellValue(employee.getLevel());
					break;

				case 32: // Data nascita
					cell.setCellStyle(center);
					cell.setCellValue(employee.getBirthDate());
					break;

				case 33: // Data assunzione
					cell.setCellStyle(center);
					cell.setCellValue(employee.getHireDate());
					break;

				case 34: // Data cessazione
					cell.setCellStyle(center);
					cell.setCellValue(employee.getTerminationDate());
					break;

				default:
					cell.setCellStyle(center);
					cell.setCellValue("");
					break;
			}
		};

		// ============================================================
		// CASO 1: ci sono presenze → comportamento normale
		// ============================================================
		if (dailyReports != null && !dailyReports.isEmpty()) {

			for (DailyReport dailyReport : dailyReports) {

				// Incremento SEMPRE il dayCounter
				dayCounter.incrementAndGet();

				// Se totale ore = 0 → NON scrivere la riga
				if (dailyReport.getTotalHours() <= 0f) {
					continue;
				}

				// Salta dimissioni
				List<Pair<AbsenceType, Integer>> absences = dailyReport.getAbscenceMinutes();
				if (absences != null && !absences.isEmpty()) {
					Pair<AbsenceType, Integer> firstPair = absences.get(0);
					if (firstPair != null && firstPair.getLeft() == AbsenceType.DIM) {
						continue;
					}
				}

				Row row = sheet.createRow(startRow + rowCounter.get());
				row.setHeightInPoints(12);
				rowCounter.incrementAndGet();

				// Pre-calcolo assenze
				Map<Integer, Integer> absenceMinutesByColumn = new HashMap<>();
				if (dailyReport.getAbscenceMinutes() != null) {
					for (Pair<AbsenceType, Integer> pair : dailyReport.getAbscenceMinutes()) {
						AbsenceType type = pair.getLeft();
						Integer minutes = pair.getRight();
						if (type == null || minutes == null || minutes <= 0)
							continue;
						Integer colIndex = COLUMN_MAP.get(type.getColumnName());
						if (colIndex != null) {
							absenceMinutesByColumn.compute(colIndex, (k, v) -> (v == null ? minutes : v + minutes));
						}
					}
				}

				// Scrittura celle
				for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {

					Cell cell = row.createCell(column);

					// Colonne assenze
					if (absenceMinutesByColumn.containsKey(column)) {
						cell.setCellStyle(center);
						cell.setCellValue(absenceMinutesByColumn.get(column));
						continue;
					}

					writeCell.accept(cell, column);
				}
			}

			return startRow + rowCounter.get();
		}

		// ========================================================================
		// CASO 2: nessuna presenza → Opzione C (una sola riga per il primo giorno)
		// ========================================================================

		Row row = sheet.createRow(startRow + rowCounter.get());
		row.setHeightInPoints(12);
		rowCounter.incrementAndGet();

		for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {
			Cell cell = row.createCell(column);
			writeCell.accept(cell, column);
		}

		return startRow + rowCounter.get();
	}

	private CellStyle createTwoDecimalsStyle(@NonNull Workbook wb) {
		CellStyle style = createDataCellStyle(wb);
		DataFormat format = wb.createDataFormat();
		style.setDataFormat(format.getFormat("0.00"));
		return style;
	}	

	private CellStyle createLeftStyle(Workbook wb) {
		CellStyle style = createDataCellStyle(wb);
		style.setAlignment(HorizontalAlignment.LEFT);
		return style;
	}

	private CellStyle createCenterStyle(Workbook wb) {
		CellStyle style = createDataCellStyle(wb);
		style.setAlignment(HorizontalAlignment.CENTER);
		return style;
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
