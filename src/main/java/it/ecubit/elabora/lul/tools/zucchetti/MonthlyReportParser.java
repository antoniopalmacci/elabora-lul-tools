package it.ecubit.elabora.lul.tools.zucchetti;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import it.ecubit.elabora.lul.tools.enums.AbsenceType;
import it.ecubit.elabora.lul.tools.enums.Month;
import it.ecubit.elabora.lul.tools.enums.WeekDay;
import it.ecubit.elabora.lul.tools.enums.WorkAttendanceType;
import it.ecubit.elabora.lul.tools.model.Company;
import it.ecubit.elabora.lul.tools.model.DailyReport;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import it.ecubit.elabora.lul.tools.utils.FiscalCodeParser;
import it.ecubit.elabora.lul.tools.utils.NationalHolidays;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class MonthlyReportParser {

    // ======================================================
    // PUBLIC ENTRY POINT
    // ======================================================
    public MonthlyReport parsePageContent(@NonNull String pageContent) {

        String[] lines = pageContent.split("\\r?\\n");

        if (isPageTypeA(lines)) {
            return parsePageTypeA(lines);
        }

        if (isPageTypeB(lines)) {
            return parsePageTypeB(lines);
        }

        MonthlyReport empty = new MonthlyReport();
        empty.setEmployee(new Employee());
        return empty;
    }

    // ======================================================
    // PAGE TYPE DETECTION
    // ======================================================
    private boolean isPageTypeA(String[] lines) {
        return Arrays.stream(lines).anyMatch(l -> l.contains("AZIENDA") ||
                l.contains("INFORMAZIONI AGGIUNTIVE") ||
                l.contains("PERIODO DI RIFERIMENTO") ||
                l.contains("GIORNO ORE GIUSTIFICATIVI"));
    }

    private boolean isPageTypeB(String[] lines) {
        return Arrays.stream(lines).anyMatch(l -> l.contains("Filiale/Dipendenza") ||
                l.startsWith("Sede di ") ||
                l.contains("PERIODOsDIsRETRIBUZIONE"));
    }

    // ======================================================
    // PAGE TYPE B → SEDE, CODICE DIPENDENTE, LIVELLO, DATE
    // ======================================================
    private MonthlyReport parsePageTypeB(String[] lines) {

        MonthlyReport result = new MonthlyReport();
        Employee employee = new Employee();
        Company company = new Company();

        boolean nextCompanyName = false;
        boolean nextCompanyAddress = false;
        boolean nextCompanyCap = false;

        boolean nextIsHeadquartersCode = false;
        boolean nextIsEmployeeName = false;
        boolean nextIsEmployeeCode = false;
        boolean nextIsLevel = false;
        boolean nextIsDates = false;

        boolean nextIsPeriod = false;
        int periodScanCount = 0;

        boolean headquartersFound = false;

        String rawEmployeeFullName = null;
        String fiscalCode = null;

        for (String raw : lines) {

            String s = raw.trim().replaceAll(" +", " ");
            if (s.isEmpty())
                continue;

            // ============================================================
            // COMPANY NAME
            // ============================================================
            if (s.contains("CodicesAzienda RagionesSociale")) {
                nextCompanyName = true;
                continue;
            }

            if (nextCompanyName) {
                String[] parts = s.split(" ", 2);
                if (parts.length == 2) {
                    company.setName(parts[1].trim());
                }
                nextCompanyName = false;
                continue;
            }

            // ============================================================
            // COMPANY ADDRESS
            // ============================================================
            if (s.equals("Indirizzo")) {
                nextCompanyAddress = true;
                continue;
            }

            if (nextCompanyAddress) {
                company.setAddress(s);
                nextCompanyAddress = false;
                nextCompanyCap = true;
                continue;
            }

            if (nextCompanyCap && s.matches("^\\d{5}.*")) {
                company.setAddress(company.getAddress() + ", " + s);
                nextCompanyCap = false;
                continue;
            }

            // ============================================================
            // PERIODO (robusto)
            // ============================================================
            if (s.contains("PERIODOsDIsRETRIBUZIONE")) {
                nextIsPeriod = true;
                periodScanCount = 0;
                continue;
            }

            if (nextIsPeriod) {

                String[] parts = s.trim().split("\\s+");

                if (parts.length >= 2 && Month.isValidMonth(parts[0])) {
                    Month month = Month.of(parts[0]);
                    int year = Integer.parseInt(parts[1]);
                    result.setMonth(month);
                    result.setYear(year);
                    nextIsPeriod = false;
                    continue;
                }

                periodScanCount++;
                if (periodScanCount >= 3) {
                    nextIsPeriod = false;
                }

                continue;
            }

            // ============================================================
            // SEDE TESTUALE ("Sede di Bari")
            // ============================================================
            if (!headquartersFound && isHeadquartersText(s)) {
                employee.setHeadquarters(
                        s.substring("Sede di".length()).trim());
                headquartersFound = true;
                continue;
            }

            // ============================================================
            // SEDE ABBREVIATA (Bari, Rende, Centro)
            // ============================================================
            if (!headquartersFound && isLikelyCity(s)) {
                employee.setHeadquarters(s.trim());
                headquartersFound = true;
                continue;
            }

            // ============================================================
            // HEADER CODICE SEDE
            // ============================================================
            if (isHeadquartersCodeHeader(s)) {
                nextIsHeadquartersCode = true;
                continue;
            }

            // ============================================================
            // CODICE SEDE
            // ============================================================
            if (nextIsHeadquartersCode) {

                if (s.matches("\\d{6,12}")) {
                    employee.setHeadquarters(s.trim());
                }

                nextIsHeadquartersCode = false;
                continue;
            }

            // ============================================================
            // EMPLOYEE NAME
            // ============================================================
            if (s.contains("Codicesdipendente")) {
                nextIsEmployeeName = true;
                continue;
            }

            if (nextIsEmployeeName) {
                rawEmployeeFullName = s.trim();
                nextIsEmployeeName = false;
                nextIsEmployeeCode = true;
                continue;
            }

            // ============================================================
            // EMPLOYEE CODE + FISCAL CODE
            // ============================================================
            if (nextIsEmployeeCode) {

                if (!s.matches("^\\d+.*")) {
                    continue;
                }

                String[] parts = s.split("\\s+");
                if (parts.length >= 1)
                    employee.setEmployeeCode(parts[0]);
                if (parts.length >= 2)
                    fiscalCode = parts[1];

                nextIsEmployeeCode = false;
                continue;
            }

            // ============================================================
            // LEVEL
            // ============================================================
            if (s.contains("DatasdisNascita")) {
                nextIsLevel = true;
                continue;
            }

            if (nextIsLevel) {
                employee.setLevel(s.trim());
                nextIsLevel = false;
                nextIsDates = true;
                continue;
            }

            // ============================================================
            // DATES
            // ============================================================
            if (nextIsDates) {
                String[] parts = s.split("\\s+");

                /*
                 * employee.setBirthDate ora è gestita dalla funzione parseEmployeeName
                 * if (parts.length >= 1)
                 * employee.setBirthDate(parts[0]);
                 */
                if (parts.length >= 2)
                    employee.setHireDate(parts[1]);
                if (parts.length >= 3)
                    employee.setTerminationDate(parts[2]);

                nextIsDates = false;
                continue;
            }
        }

        // ============================================================
        // PARSING NOME + COGNOME SOLO ORA CHE ABBIAMO IL CF
        // ============================================================
        if (rawEmployeeFullName != null && fiscalCode != null) {
            parseEmployeeName(rawEmployeeFullName, fiscalCode, employee);
            employee.setFiscalCode(fiscalCode);
        }

        employee.setCompany(company);
        result.setEmployee(employee);
        return result;

    }

    // ======================================================
    // PAGE TYPE A → MACCHINA A STATI COMPLETA
    // ======================================================
    private MonthlyReport parsePageTypeA(String[] lines) {

        ParserContext ctx = new ParserContext();
        ParserStatus currentStatus = ParserStatus.NO_DATA;

        for (String line : lines) {

            String s = line.trim().replaceAll(" +", " ");
            if (s.isEmpty())
                continue;

            // --- riconoscimento mese ---
            if (startsWithMonth(s)) {
                parseMonthYear(s, ctx);
                currentStatus = ParserStatus.MONTH_YEAR_CONTRACT_TYPE;
                continue;
            }

            // --- riconoscimento daily report ---
            if (startsWithWeekDayNum(s)) {
                parseDailyReport(s, ctx);
                currentStatus = ParserStatus.DAILY_REPORT;
                continue;
            }

            // --- MACCHINA A STATI ---
            switch (currentStatus) {

                case NO_DATA:
                    currentStatus = handleNoData(s, ctx);
                    break;

                case COMPANY_START:
                    currentStatus = handleCompanyStart(s, ctx);
                    break;

                case COMPANY_PARSING:
                    currentStatus = handleCompanyParsing(s, ctx);
                    break;

                case EMPLOYEE_START:
                    currentStatus = handleEmployeeStart(s, ctx);
                    break;

                case EMPLOYEE_PARSING:
                    currentStatus = handleEmployeeParsing(s, ctx);
                    break;

                case MONTH_YEAR_CONTRACT_TYPE:
                    currentStatus = handleMonthYear(s, ctx);
                    break;

                case DAILY_REPORT:
                    currentStatus = handleDailyReport(s, ctx);
                    break;
            }
        }

        ctx.employee.setCompany(ctx.company);
        ctx.result.setEmployee(ctx.employee);

        return ctx.result;
    }

    // ======================================================
    // HANDLER DELLA STATE MACHINE (PageTypeA)
    // ======================================================

    private ParserStatus handleNoData(String s, ParserContext ctx) {

        if (s.equalsIgnoreCase("AZIENDA")) {
            return ParserStatus.COMPANY_START;
        }

        if (ParserPattern.EMPLOYEE_ID_REGEX_PATTERN.matcher(s).matches()) {
            ctx.employee.setId(s);
            return ParserStatus.EMPLOYEE_START;
        }

        return ParserStatus.NO_DATA;
    }

    private ParserStatus handleCompanyStart(String s, ParserContext ctx) {
        ctx.company.setName(s);
        return ParserStatus.COMPANY_PARSING;
    }

    private ParserStatus handleCompanyParsing(String s, ParserContext ctx) {
        ctx.company.setAddress(s);
        return ParserStatus.NO_DATA;
    }

    private ParserStatus handleEmployeeStart(String s, ParserContext ctx) {
        ctx.rawEmployeeFullName = s; // memorizziamo il nome grezzo
        return ParserStatus.EMPLOYEE_PARSING;
    }

    // ======================================================
    // LOGICA ADDRESS DIPENDENTE
    // ======================================================
    private ParserStatus handleEmployeeParsing(String s, ParserContext ctx) {

        Employee e = ctx.employee;

        // 1) Prima riga dopo nome/cognome → via
        if (e.getAddress() == null) {
            e.setAddress(s); // es. "VIA EURIPIDE, 171"
            return ParserStatus.EMPLOYEE_PARSING;
        }

        // 2) Seconda riga → CAP + città
        if (e.getCap() == null && s.matches("^\\d{5}.*")) {
            String cap = s.substring(0, 5);
            e.setCap(cap);
            e.setAddress(e.getAddress() + " " + s);
            return ParserStatus.NO_DATA;
        }

        return ParserStatus.NO_DATA;
    }

    private ParserStatus handleMonthYear(String s, ParserContext ctx) {

        // ============================================================
        // 1) Estrazione codice fiscale (come prima, ma più robusta)
        // ============================================================
        // Prima logica: CF = primo token della riga
        String[] tokens = s.split("\\s+");
        if (tokens.length > 0) {
            String fiscalCode = tokens[0].trim();
            ctx.employee.setFiscalCode(fiscalCode);

            // ============================================================
            // 2) Ora che abbiamo il CF, possiamo parsare il nome completo
            // ============================================================
            if (ctx.rawEmployeeFullName != null && !ctx.rawEmployeeFullName.isBlank()) {
                parseEmployeeName(
                        ctx.rawEmployeeFullName,
                        fiscalCode,
                        ctx.employee);
            }
        }

        // ============================================================
        // 3) Ritorna allo stato originale della tua macchina
        // ============================================================
        return ParserStatus.NO_DATA;
    }

    private ParserStatus handleDailyReport(String s, ParserContext ctx) {

        if (startsWithWeekDayNum(s)) {
            parseDailyReport(s, ctx);
            return ParserStatus.DAILY_REPORT;
        }

        return ParserStatus.NO_DATA;
    }

    // ======================================================
    // FUNZIONI DI PARSING
    // ======================================================

    private void parseMonthYear(String s, ParserContext ctx) {
        String[] parts = s.split("\\s+");
        Month month = Month.of(parts[0]);
        int year = Integer.parseInt(parts[1]);

        StringBuilder contract = new StringBuilder();
        for (int i = 2; i < parts.length; i++)
            contract.append(" ").append(parts[i]);

        ctx.result.setMonth(month);
        ctx.result.setYear(year);
        ctx.result.setWorkContractType(contract.toString().trim());
    }

    private void parseEmployeeName(String fullName, String fiscalCode, Employee employee) {

        if (fullName == null || fullName.isBlank()) {
            employee.setSurname("");
            employee.setName("");
            return;
        }

        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            employee.setSurname(parts[0]);
            employee.setName("");
            return;
        }

        // --- Estrai consonanti dal CF ---
        FiscalCodeParser parser = new FiscalCodeParser(fiscalCode);
        String cfSurname = parser.getSurnameConsonants(); // prime 3 consonanti
        String cfName = parser.getNameConsonants(); // successive 3 consonanti

        String bestSurname = parts[0];
        String bestName = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
        int bestScore = -1;

        // --- Prova tutte le possibili divisioni ---
        for (int split = 1; split < parts.length; split++) {

            String surnameCandidate = String.join(" ", Arrays.copyOfRange(parts, 0, split));
            String nameCandidate = String.join(" ", Arrays.copyOfRange(parts, split, parts.length));

            String surnameCons = extractConsonants(surnameCandidate);
            String nameCons = extractConsonants(nameCandidate);

            int score = matchScore(surnameCons, cfSurname) +
                    matchScore(nameCons, cfName);

            if (score > bestScore) {
                bestScore = score;
                bestSurname = surnameCandidate;
                bestName = nameCandidate;
            }
        }

        employee.setSurname(bestSurname.trim());
        employee.setName(bestName.trim());

        LocalDate birth = parser.getBirthDate();
        String formatted = birth.format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        employee.setBirthDate(formatted);
    }

    private String extractConsonants(String s) {
        return s.replaceAll("(?i)[^BCDFGHJKLMNPQRSTVWXYZ]", "").toUpperCase();
    }

    private int matchScore(String extracted, String cfPart) {
        int score = 0;
        for (int i = 0; i < Math.min(extracted.length(), cfPart.length()); i++) {
            if (extracted.charAt(i) == cfPart.charAt(i))
                score++;
        }
        return score;
    }

    private void parseDailyReport(String s, ParserContext ctx) {

        String[] info = s.trim().split("\\s+");
        if (info.length < 2)
            return;

        WeekDay weekDay = WeekDay.of(info[0]);
        int day = Integer.parseInt(info[1]);

        Month month = ctx.result.getMonth();
        int year = ctx.result.getYear();

        DailyReport dr = new DailyReport();
        dr.setDay(day);
        dr.setWeekDay(weekDay);
        dr.setAbscenceMinutes(new ArrayList<>());

        // FESTIVO?
        boolean holiday = NationalHolidays.isPublicHoliday(month, day, year);

        int index = 2;

        // Primo numero → ordinarie (feriale) oppure straordinarie (festivo)
        if (index < info.length && isNumber(info[index])) {
            int minutes = getMinutes(info[index]);

            if (holiday) {
                dr.setExtraWorkAttendanceMinutes(
                        Pair.of(WorkAttendanceType.FESTIVA, minutes));
            } else {
                dr.setOrdinaryWorkAttendanceMinutes(
                        Pair.of(WorkAttendanceType.ORDINARIA, minutes));
            }

            index++;
        }

        // Secondo numero → straordinarie (solo se NON festivo)
        if (!holiday && index < info.length && isNumber(info[index])) {
            int minutes = getMinutes(info[index]);
            dr.setExtraWorkAttendanceMinutes(
                    Pair.of(WorkAttendanceType.STRAORDINARIA, minutes));
            index++;
        }

        // Coppie assenze (codice + ore)
        while (index + 1 < info.length) {

            String code = info[index];
            String value = info[index + 1];

            // Gestione "ON" (Orario Notturno) → NON è un'assenza
            if (code.equalsIgnoreCase("ON")) {

                if (isNumber(value)) {
                    int minutes = getMinutes(value);
                    dr.setExtraWorkAttendanceMinutes(
                            Pair.of(WorkAttendanceType.STRAORDINARIA, minutes));
                } else {
                    JOptionPane.showMessageDialog(
                            null,
                            "Valore minuti non valido per codice ON (orario notturno): " + value +
                                    "\nDipendente: " + ctx.rawEmployeeFullName +
                                    "\nRiga: " + String.join(" ", info),
                            "Errore nel parsing",
                            JOptionPane.ERROR_MESSAGE);
                }

                index += 2;
                continue; // passa alla prossima coppia
            }

            // Gestione assenze normali
            AbsenceType type;

            try {
                type = AbsenceType.of(code);
            } catch (Exception e) {

                JOptionPane.showMessageDialog(
                        null,
                        "Codice assenza non riconosciuto: " + code +
                                "\nDipendente: " + ctx.rawEmployeeFullName +
                                "\nRiga: " + String.join(" ", info),
                        "Errore nel parsing",
                        JOptionPane.ERROR_MESSAGE);

                break; // interrompe il parsing delle assenze
            }

            // Valore minuti valido?
            if (isNumber(value)) {
                dr.getAbscenceMinutes().add(
                        Pair.of(type, getMinutes(value)));
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Valore minuti non valido per il codice assenza: " + code +
                                "\nValore trovato: " + value +
                                "\nDipendente: " + ctx.rawEmployeeFullName +
                                "\nRiga: " + String.join(" ", info),
                        "Errore nel parsing",
                        JOptionPane.ERROR_MESSAGE);
            }

            index += 2;
        }

        ctx.result.addDailyReport(dr);
    }

    // ======================================================
    // FUNZIONI ACCESSORIE
    // ======================================================

    private boolean startsWithMonth(@NonNull String line) {
        String firstWord = line.split("\\s+")[0].toLowerCase();
        for (Month month : Month.values()) {
            if (firstWord.equals(month.getName().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean startsWithWeekDayNum(@NonNull String line) {
        return ParserPattern.WEEK_DAY_NUM_PATTERN.matcher(line.trim()).find();
    }

    private boolean isNumber(String s) {
        try {
            Float.parseFloat(s.replace(",", "."));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private int getMinutes(String repr) {
        float hours = Float.parseFloat(repr.replace(",", "."));
        int integer = (int) hours;
        float decimal = hours - integer;
        float minutes = integer * 60 + decimal * 100;
        return Math.round(minutes);
    }

    private boolean isHeadquartersCodeHeader(String s) {
        return s.toLowerCase().contains("filiale/dipendenza");
    }

    private boolean isHeadquartersText(String s) {
        return s.toLowerCase().startsWith("sede di ");
    }

    private boolean isLikelyCity(String s) {

        // Esclude stringhe con spazi (es. "Santa Maria")
        if (s.contains(" "))
            return false;

        // Esclude numeri
        if (s.matches(".*\\d.*"))
            return false;

        // Esclude orari tipo "17:48"
        if (s.matches("^\\d{1,2}:\\d{2}$"))
            return false;

        // Deve essere capitalizzata come un nome di città
        if (!s.matches("^[A-Z][a-zàèéìòù]+$"))
            return false;

        // Lunghezza ragionevole
        if (s.length() < 3 || s.length() > 20)
            return false;

        return true;
    }

    // ======================================================
    // CONTEXT CLASS
    // ======================================================
    private static class ParserContext {
        public final MonthlyReport result = new MonthlyReport();
        public final Employee employee = new Employee();
        public final Company company = new Company();

        public String rawEmployeeFullName; // <--- aggiunto
    }
}
