package it.ecubit.elabora.lul.tools.zucchetti;

import java.io.IOException;

/* import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException; */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import org.springframework.stereotype.Component;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.LocationTextExtractionStrategy;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextExtractionStrategy;

import it.ecubit.elabora.lul.tools.enums.AbsenceType;
import it.ecubit.elabora.lul.tools.enums.Month;
import it.ecubit.elabora.lul.tools.enums.WeekDay;
import it.ecubit.elabora.lul.tools.enums.WorkAttendanceType;
import it.ecubit.elabora.lul.tools.model.Company;
import it.ecubit.elabora.lul.tools.model.DailyReport;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import it.ecubit.elabora.lul.tools.model.PdfDocumentInfo;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class PdfReportParser {

    public PdfDocumentInfo inspect(@NonNull String filename) throws IOException {
        PdfDocumentInfo result = new PdfDocumentInfo();
        PdfReader reader = new PdfReader(filename);
        result.setFilename(filename);
        result.setNumPages(reader.getNumberOfPages());
        Rectangle mediabox = reader.getPageSize(1);
        float[] writingAreaRect = new float[4];
        writingAreaRect[0] = mediabox.getLeft();
        writingAreaRect[1] = mediabox.getBottom();
        writingAreaRect[2] = mediabox.getRight();
        writingAreaRect[3] = mediabox.getTop();
        result.setWritingAreaRect(writingAreaRect);
        result.setRotationAngle(reader.getPageRotation(1));
        result.setFilesize(reader.getFileLength());
        result.setRebuilt(reader.isRebuilt());
        result.setEncrypted(reader.isEncrypted());
        return result;
    }

    public String extractTextFromPage(@NonNull String filename, int pageNum) throws IOException {
        PdfReader reader = new PdfReader(filename);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        TextExtractionStrategy strategy = parser.processContent(pageNum, new LocationTextExtractionStrategy());
        return strategy.getResultantText();
    }

    public List<String> extractTextFromAllPages(@NonNull String filename) throws IOException {
        List<String> result = new ArrayList<>();
        PdfReader reader = new PdfReader(filename);
        PdfReaderContentParser parser = new PdfReaderContentParser(reader);
        int numberOfPages = reader.getNumberOfPages();
        for (int pageNum = 1; pageNum <= numberOfPages; pageNum++) {
            TextExtractionStrategy strategy = parser.processContent(pageNum, new LocationTextExtractionStrategy());
            String extractedText = strategy.getResultantText();
            if (extractedText != null && !extractedText.trim().isEmpty()) {
                result.add(extractedText);
            }
        }
        return result;
    }

    /*
     * public MonthlyReport parsePageContent(@NonNull String pageContent) {
     * MonthlyReport result = new MonthlyReport();
     * Employee employee = new Employee();
     * Company company = new Company();
     * String lines[] = pageContent.split("\\r?\\n");
     * List<Pair<String, ParserStatus>> elemsWithLabels = new ArrayList<>();
     * int elemIndex = 0;
     * 
     * for (String line : lines) {
     * String stringToParse = line.trim().replaceAll(" +", " ");
     * if (!stringToParse.isEmpty()) {
     * 
     * if (stringToParse.equalsIgnoreCase(ParserPattern.COMPANY)) {
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.COMPANY_START));
     * 
     * } else if (elemsWithLabels.size() >= 1
     * && elemsWithLabels.get(elemIndex - 1).getValue() ==
     * ParserStatus.COMPANY_START) {
     * company.setName(stringToParse);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.COMPANY_PARSING));
     * 
     * } else if (elemsWithLabels.size() >= 2
     * && elemsWithLabels.get(elemIndex - 2).getValue() ==
     * ParserStatus.COMPANY_START) {
     * company.setAddress(stringToParse);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.COMPANY_PARSING));
     * 
     * } else if
     * (ParserPattern.EMPLOYEE_ID_REGEX_PATTERN.matcher(stringToParse).matches()) {
     * employee.setId(stringToParse);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.EMPLOYEE_START));
     * 
     * } else if (elemsWithLabels.size() >= 1
     * && elemsWithLabels.get(elemIndex - 1).getValue() ==
     * ParserStatus.EMPLOYEE_START) {
     * 
     * String employeeFullName = stringToParse;
     * String[] nameElements = employeeFullName.split("\\s");
     * String name = "";
     * String surname = "";
     * 
     * if (employeeFullName.toLowerCase().startsWith("di ") && nameElements.length >
     * 2) {
     * surname = nameElements[0] + " " + nameElements[1];
     * for (int i = 2; i < nameElements.length; i++) name += " " + nameElements[i];
     * 
     * } else if (nameElements.length > 2) {
     * surname = nameElements[0];
     * for (int i = 1; i < nameElements.length; i++) name += " " + nameElements[i];
     * 
     * } else {
     * surname = nameElements[0];
     * name = nameElements[1];
     * }
     * 
     * employee.setName(name.trim());
     * employee.setSurname(surname.trim());
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.EMPLOYEE_PARSING));
     * 
     * } else if (elemsWithLabels.size() >= 2
     * && elemsWithLabels.get(elemIndex - 2).getValue() ==
     * ParserStatus.EMPLOYEE_START) {
     * employee.setAddress(stringToParse);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.EMPLOYEE_PARSING));
     * 
     * } else if (elemsWithLabels.size() >= 3
     * && elemsWithLabels.get(elemIndex - 3).getValue() ==
     * ParserStatus.EMPLOYEE_START) {
     * employee.setCap(stringToParse);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.EMPLOYEE_PARSING));
     * 
     * } else if (startsWithMonth(stringToParse)) {
     * 
     * String[] elements = stringToParse.split("\\s");
     * Month month = Month.of(elements[0]);
     * int year = Integer.parseInt(elements[1]);
     * String workContractType = "";
     * for (int j = 2; j < elements.length; j++) workContractType += " " +
     * elements[j];
     * 
     * result.setMonth(month);
     * result.setYear(year);
     * result.setWorkContractType(workContractType);
     * elemsWithLabels.add(Pair.of(stringToParse,
     * ParserStatus.MONTH_YEAR_CONTRACT_TYPE));
     * 
     * } else if (elemsWithLabels.size() >= 1
     * && elemsWithLabels.get(elemIndex - 1).getValue() ==
     * ParserStatus.MONTH_YEAR_CONTRACT_TYPE) {
     * 
     * employee.setFiscalCode(stringToParse.split(" ")[0]);
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.EMPLOYEE_PARSING));
     * 
     * } else if (startsWithWeekDayNum(stringToParse)) {
     * 
     * String[] dailyReportInfo = stringToParse.split("\\s");
     * if (dailyReportInfo.length < 2) {
     * log.error(String.format(
     * "Impossibile riconoscere il formato del report giornaliero per la riga: '%s'. Giorno della settimana non presente."
     * ,
     * stringToParse));
     * } else {
     * // daily report
     * // Expected format:
     * // MA 4 5,00 (opzionale, ordinarie) 1,00 (opzionale, straordinarie)
     * // RL (opzionale, giustificativo assenza) 2,00 (opzionale, ore assenza)
     * // AI (opzionale, giustificativo assenza) 4,00 (opzionale, ore assenza)
     * // .....
     * 
     * WeekDay weekDay = WeekDay.of(dailyReportInfo[0]);
     * int monthDay = Integer.parseInt(dailyReportInfo[1]);
     * DailyReport dailyReport = new DailyReport();
     * dailyReport.setDay(monthDay);
     * dailyReport.setWeekDay(weekDay);
     * 
     * // inizializziamo la lista delle assenze
     * dailyReport.setAbscenceMinutes(new ArrayList<>());
     * 
     * // --- ELEMENTO 3 ---
     * if (dailyReportInfo.length >= 3) {
     * String element = dailyReportInfo[2];
     * if (isNumber(element)) {
     * if (weekDay == WeekDay.SATURDAY || weekDay == WeekDay.SUNDAY) {
     * dailyReport.setOrdinaryWorkAttendanceMinutes(
     * Pair.of(WorkAttendanceType.FESTIVA, getMinutes(element)));
     * } else {
     * dailyReport.setOrdinaryWorkAttendanceMinutes(
     * Pair.of(WorkAttendanceType.ORDINARIA, getMinutes(element)));
     * }
     * } else {
     * AbsenceType absenceType = AbsenceType.of(element);
     * dailyReport.getAbscenceMinutes().add(Pair.of(absenceType, 0));
     * }
     * }
     * 
     * // --- ELEMENTO 4 ---
     * if (dailyReportInfo.length >= 4) {
     * String element = dailyReportInfo[3];
     * if (isNumber(element)) {
     * if (!dailyReport.getAbscenceMinutes().isEmpty()) {
     * List<Pair<AbsenceType, Integer>> list = dailyReport.getAbscenceMinutes();
     * AbsenceType absenceType = list.get(list.size() - 1).getLeft();
     * list.add(Pair.of(absenceType, getMinutes(element)));
     * } else {
     * dailyReport.setExtraWorkAttendanceMinutes(
     * Pair.of(WorkAttendanceType.STRAORDINARIA, getMinutes(element)));
     * }
     * } else {
     * AbsenceType absenceType = AbsenceType.of(element);
     * dailyReport.getAbscenceMinutes().add(Pair.of(absenceType, 0));
     * }
     * }
     * 
     * // --- ELEMENTO 5 ---
     * if (dailyReportInfo.length >= 5) {
     * String element = dailyReportInfo[4];
     * if (isNumber(element)) {
     * List<Pair<AbsenceType, Integer>> list = dailyReport.getAbscenceMinutes();
     * AbsenceType absenceType = list.get(list.size() - 1).getLeft();
     * list.add(Pair.of(absenceType, getMinutes(element)));
     * } else {
     * AbsenceType absenceType = AbsenceType.of(element);
     * dailyReport.getAbscenceMinutes().add(Pair.of(absenceType, 0));
     * }
     * }
     * 
     * // --- ELEMENTO 6 ---
     * if (dailyReportInfo.length >= 6 && isNumber(dailyReportInfo[5])) {
     * List<Pair<AbsenceType, Integer>> list = dailyReport.getAbscenceMinutes();
     * AbsenceType absenceType = list.get(list.size() - 1).getLeft();
     * list.add(Pair.of(absenceType, getMinutes(dailyReportInfo[5])));
     * }
     * 
     * result.addDailyReport(dailyReport);
     * }
     * 
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.DAILY_REPORT));
     * 
     * } else {
     * elemsWithLabels.add(Pair.of(stringToParse, ParserStatus.NO_DATA));
     * }
     * 
     * elemIndex += 1;
     * }
     * }
     * 
     * 
     * employee.setCompany(company);
     * result.setEmployee(employee);
     * return result;
     * }
     * 
     * 
     * private boolean startsWithMonth(@NonNull String line) {
     * boolean result = false;
     * String firstWord = line.split(" ")[0];
     * for (Month month : Month.values()) {
     * // if (line.toLowerCase().startsWith(month.getName().toLowerCase())) {
     * if (firstWord.toLowerCase().equals(month.getName().toLowerCase())) {
     * result = true;
     * break;
     * }
     * }
     * return result;
     * }
     * 
     * private boolean startsWithWeekDayNum(@NonNull String line) {
     * boolean result = false;
     * if (line.length() > 4) {
     * String prefix = line.substring(0, 4);
     * result = ParserPattern.WEEK_DAY_NUM_PATTERN.matcher(prefix).matches();
     * }
     * return result;
     * }
     * 
     * private boolean isNumber(@NonNull String element) {
     * return (parseFloat(element) > 0);
     * }
     * 
     * private int getMinutes(@NonNull String repr) {
     * float hours = parseFloat(repr);
     * if (hours < 0) {
     * return -1;
     * } else {
     * int integer = (int) Math.floor(hours);
     * float decimal = hours - integer;
     * float minutes = integer * 60 + decimal * 100;
     * return Math.round(minutes);
     * }
     * }
     * 
     * private float parseFloat(@NonNull String repr) {
     * float result = -1.0f;
     * try {
     * result = Float.parseFloat(repr);
     * } catch (NumberFormatException nfExc) {
     * }
     * if (result < 0) {
     * DecimalFormatSymbols symbols = new DecimalFormatSymbols();
     * symbols.setDecimalSeparator(',');
     * DecimalFormat decimalFormat = new DecimalFormat("#.00");
     * decimalFormat.setDecimalFormatSymbols(symbols);
     * try {
     * result = decimalFormat.parse(repr).floatValue();
     * } catch (ParseException pExc) {
     * }
     * }
     * return result;
     * }
     */
}
