package it.ecubit.elabora.lul.tools.zucchetti;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        // Pagina rumore → restituiamo un MonthlyReport vuoto
        MonthlyReport empty = new MonthlyReport();
        empty.setEmployee(new Employee());
        return empty;
    }

    // ======================================================
    // PAGE TYPE DETECTION (rafforzato)
    // ======================================================
    private boolean isPageTypeA(String[] lines) {
        return Arrays.stream(lines).anyMatch(l -> l.contains("AZIENDA") ||
                l.contains("INFORMAZIONI AGGIUNTIVE") ||
                l.contains("PERIODO DI RIFERIMENTO") ||
                l.contains("GIORNO ORE GIUSTIFICATIVI"));
    }

    private boolean isPageTypeB(String[] lines) {
        return Arrays.stream(lines).anyMatch(l -> l.contains("Filiale/Dipendenza") ||
                l.startsWith("Sede di "));
    }

    // ======================================================
    // PAGE TYPE B → SEDE, CODICE DIPENDENTE, LIVELLO, DATE
    // ======================================================
    private MonthlyReport parsePageTypeB(String[] lines) {

        MonthlyReport result = new MonthlyReport();
        Employee employee = new Employee();

        boolean nextIsHeadquartersCode = false;
        boolean nextIsEmployeeCode = false;
        boolean nextIsLevel = false;
        boolean nextIsDates = false;

        for (String raw : lines) {

            String s = raw.trim().replaceAll(" +", " ");
            if (s.isEmpty())
                continue;

            // --- SEDE TESTUALE ---
            if (isHeadquartersText(s)) {
                employee.setHeadquarters(
                        s.substring("Sede di".length()).trim());
                continue;
            }

            // --- HEADER CODICE SEDE ---
            if (isHeadquartersCodeHeader(s)) {
                nextIsHeadquartersCode = true;
                continue;
            }

            // --- CODICE SEDE ---
            if (nextIsHeadquartersCode) {

                if (s.matches("\\d{6,12}")) {
                    employee.setHeadquarters(s.trim());
                }

                nextIsHeadquartersCode = false;
                continue;
            }

            // --- HEADER CODICE DIPENDENTE ---
            if (s.contains("Codicesdipendente")) {
                nextIsEmployeeCode = true; // da qui in poi cerchiamo la prima riga numerica
                continue;
            }

            // --- RIGA CODICE DIPENDENTE (prima riga che inizia con cifra) ---
            if (nextIsEmployeeCode) {

                // se NON inizia con cifra → salta
                if (!s.matches("^\\d+.*")) {
                    continue;
                }

                // qui siamo sulla riga tipo: "0000009 RRGBBR68T49H501K"
                String[] parts = s.split("\\s+");
                if (parts.length >= 1) {
                    employee.setEmployeeCode(parts[0]); // 0000009
                }

                nextIsEmployeeCode = false;
                continue;
            }

            // --- HEADER DATE ---
            if (s.contains("DatasdisNascita")) {
                nextIsLevel = true;
                continue;
            }

            // --- LIVELLO ---
            if (nextIsLevel) {
                employee.setLevel(s.trim()); // IMP A1
                nextIsLevel = false;
                nextIsDates = true;
                continue;
            }

            // --- DATE ---
            if (nextIsDates) {
                String[] parts = s.split("\\s+");

                if (parts.length >= 1)
                    employee.setBirthDate(parts[0]);
                if (parts.length >= 2)
                    employee.setHireDate(parts[1]);
                if (parts.length >= 3)
                    employee.setTerminationDate(parts[2]);

                nextIsDates = false;
                continue;
            }
        }

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
        parseEmployeeName(s, ctx.employee);
        return ParserStatus.EMPLOYEE_PARSING;
    }

    // ======================================================
    // NUOVA LOGICA ADDRESS DIPENDENTE (corretta)
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
        ctx.employee.setFiscalCode(s.split(" ")[0]);
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

    private void parseEmployeeName(String fullName, Employee employee) {

        String[] parts = fullName.split("\\s+");
        String name = "";
        String surname = "";

        if (fullName.toLowerCase().startsWith("di ") && parts.length > 2) {
            surname = parts[0] + " " + parts[1];
            for (int i = 2; i < parts.length; i++)
                name += " " + parts[i];

        } else if (parts.length > 2) {
            surname = parts[0];
            for (int i = 1; i < parts.length; i++)
                name += " " + parts[i];

        } else {
            surname = parts[0];
            name = parts[1];
        }

        employee.setName(name.trim());
        employee.setSurname(surname.trim());
    }

    private void parseDailyReport(String s, ParserContext ctx) {

        String[] info = s.split("\\s+");

        WeekDay weekDay = WeekDay.of(info[0]);
        int day = Integer.parseInt(info[1]);

        DailyReport dr = new DailyReport();
        dr.setDay(day);
        dr.setWeekDay(weekDay);
        dr.setAbscenceMinutes(new ArrayList<>());

        if (info.length >= 3)
            parseDailyElement(info[2], dr, weekDay);
        if (info.length >= 4)
            parseDailyElement(info[3], dr, weekDay);
        if (info.length >= 5)
            parseDailyElement(info[4], dr, weekDay);

        if (info.length >= 6 && isNumber(info[5])) {
            List<Pair<AbsenceType, Integer>> list = dr.getAbscenceMinutes();
            AbsenceType type = list.get(list.size() - 1).getLeft();
            list.add(Pair.of(type, getMinutes(info[5])));
        }

        ctx.result.addDailyReport(dr);
    }

    private void parseDailyElement(String element, DailyReport dr, WeekDay weekDay) {

        if (isNumber(element)) {

            if (!dr.getAbscenceMinutes().isEmpty()) {
                List<Pair<AbsenceType, Integer>> list = dr.getAbscenceMinutes();
                AbsenceType type = list.get(list.size() - 1).getLeft();
                list.add(Pair.of(type, getMinutes(element)));

            } else if (weekDay == WeekDay.SATURDAY || weekDay == WeekDay.SUNDAY) {
                dr.setOrdinaryWorkAttendanceMinutes(
                        Pair.of(WorkAttendanceType.FESTIVA, getMinutes(element)));

            } else {
                dr.setOrdinaryWorkAttendanceMinutes(
                        Pair.of(WorkAttendanceType.ORDINARIA, getMinutes(element)));
            }

        } else {
            AbsenceType type = AbsenceType.of(element);
            dr.getAbscenceMinutes().add(Pair.of(type, 0));
        }
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
        if (line.length() > 4) {
            String prefix = line.substring(0, 4);
            return ParserPattern.WEEK_DAY_NUM_PATTERN.matcher(prefix).matches();
        }
        return false;
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

    // ======================================================
    // CONTEXT CLASS
    // ======================================================
    private static class ParserContext {
        public final MonthlyReport result = new MonthlyReport();
        public final Employee employee = new Employee();
        public final Company company = new Company();
    }
}
