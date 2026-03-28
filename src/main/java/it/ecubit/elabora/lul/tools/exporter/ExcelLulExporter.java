package it.ecubit.elabora.lul.tools.exporter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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

	public void export(@NonNull Map<String, List<MonthlyReport>> reportsForMonthMap, @NonNull Path storageDirPath,
			@NonNull String filename) throws IOException {
		Workbook wb = new XSSFWorkbook();
		List<String> monthYearsList = new ArrayList<>(reportsForMonthMap.keySet());
		monthYearsList.sort(new MonthYearStringComparator());
		monthYearsList.forEach(
				monthYear -> {
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
		try (OutputStream fileOut = new FileOutputStream(storageDirPath.resolve(filename).toFile())) {
			wb.write(fileOut);
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

	private int createRowsForEmployeeMonthlyReport(@NonNull Workbook wb, @NonNull Sheet sheet,
			@NonNull MonthlyReport monthlyReport, int startRow) {
		AtomicInteger counter = new AtomicInteger(0);
		final CellStyle dataCellStyle = createDataCellStyle(wb);
		final Month month = monthlyReport.getMonth();
		final int year = monthlyReport.getYear();
		final Employee employee = monthlyReport.getEmployee();
		final DateTimeFormatter fullDayFormmater = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		monthlyReport.getDailyReports().forEach(dailyReport -> {
			if (!NationalHolidays.isPublicHoliday(month, dailyReport.getDay(), year)) {
				Row dailyReportRow = sheet.createRow(startRow + counter.get());
				dailyReportRow.setHeightInPoints(12);
				counter.incrementAndGet();
				for (int column = 0; column < WorkbookInfo.SHEET_COLUMN_NAMES.length; column++) {
					Cell dataCell = dailyReportRow.createCell(column);
					dataCell.setCellStyle(dataCellStyle);
					switch (column) {
						case 0:
							// surname
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							dataCell.setCellValue(employee.getSurname());
							break;
						case 1:
							// name
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							dataCell.setCellValue(employee.getName());
							break;
						case 2:
							// day (full day format)
							LocalDate date = LocalDate.of(year, month.getMonthInYear(), dailyReport.getDay());
							dataCell.setCellValue(fullDayFormmater.format(date));
							break;
						case 3:
							// presence
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							dataCell.setCellValue(dailyReport.isPresent() ? 1 : 0);
							break;
						case 4:
							// vacation
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.FER &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 5:
							// sickness
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									(dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.MAL 
									|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.IN) 
									&&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 6:
							// paid leave
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									(dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.PR
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.CGR
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.CP
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.PG)
									&&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 7:
							// unpaid leave or not worked
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									(dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.PNR
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.NL)
									&&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 14:
							// blood donation
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.DS &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 12:
							// study
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.PS &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 15:
							// maternity or breastfeeding
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									(dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.MAT
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.ALL)
									&&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 17:
							// overtime or night work
							int overtimeOrNightWorkMinutes = 0;
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.NOT &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								overtimeOrNightWorkMinutes += dailyReport.getAbscenceMinutes().getSecond();
							}
							if (dailyReport.getExtraWorkAttendanceMinutes() != null &&
									dailyReport.getExtraWorkAttendanceMinutes().getSecond() != null &&
									dailyReport.getExtraWorkAttendanceMinutes().getSecond() > 0) {
								overtimeOrNightWorkMinutes += dailyReport.getExtraWorkAttendanceMinutes().getSecond();
							}
							if (overtimeOrNightWorkMinutes > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(overtimeOrNightWorkMinutes);
							}
							break;
						case 18:
							// strike
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.SC &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 19:
							// marital leave
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.CM &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 20:
							// absences related to situations egulated by the 104 law
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.L104 &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 22:
							// layoffs
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									(dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.CIG
											|| dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.ASS)
									&&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 24:
							// mourning
							if (dailyReport.getAbscenceMinutes() != null &&
									dailyReport.getAbscenceMinutes().getFirst() != null &&
									dailyReport.getAbscenceMinutes().getFirst() == AbsenceType.LUT &&
									dailyReport.getAbscenceMinutes().getSecond() != null &&
									dailyReport.getAbscenceMinutes().getSecond() > 0) {
								dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
								dataCell.setCellValue(dailyReport.getAbscenceMinutes().getSecond());
							}
							break;
						case 25:
							// tot working hours
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							dataCell.setCellValue(
									EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER.format(dailyReport.getTotWorkingHours()));
							break;
						case 26:
							// tot non working hours
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							dataCell.setCellValue(EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER
									.format(dailyReport.getTotNonWorkingHours()));
							break;
						case 27:
							// tot hours
							float totHours = dailyReport.getTotWorkingHours() + dailyReport.getTotNonWorkingHours();
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.RIGHT);
							dataCell.setCellValue(EXCEL_EXPORTER_DEFAULT_NUMERIC_FORMATTER.format(totHours));
							break;
						case 28:
							// fiscal code
							dataCell.getCellStyle().setAlignment(HorizontalAlignment.LEFT);
							dataCell.setCellValue(employee.getFiscalCode());
							break;
						default:
							// all other cases
							dataCell.setCellValue("");
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
