package it.ecubit.elabora.lul.tools.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import it.ecubit.elabora.lul.tools.enums.Month;
import lombok.NonNull;

public class NationalHolidays {

	public static List<Holiday> getItalianNationalHolidaysForYear(int year) {
		Holiday NEW_YEAR_DAY = new Holiday(Month.JANUARY, 1, year, "Capodanno");
		Holiday EPIPHANY = new Holiday(Month.JANUARY, 6, year, "Epifania");
		Holiday EASTER = calculateEasterDate(year);
		Holiday EASTER_MONDAY = new Holiday(nextDay(EASTER.getMonth(), EASTER.getDayOfMonth(), year), "Lunedì dell'Angelo");
		Holiday LIBERATION_DAY = new Holiday(Month.APRIL, 25, year, "Liberazione dal nazifascismo");
		Holiday LABOUR_DAY = new Holiday(Month.MAY, 1, year, "Festa del lavoro");
		Holiday REPUBLIC_DAY = new Holiday(Month.JUNE, 2, year, "Festa della Repubblica");
		Holiday ASSUMPTION_FEAST_DAY = new Holiday(Month.AUGUST, 15, year, "Assunzione di Maria");
		Holiday ALL_SAINTS_DAY = new Holiday(Month.NOVEMBER, 1, year, "Ognissanti");
		Holiday IMMACULATE_CONCEPTION = new Holiday(Month.DECEMBER, 8, year, "Immacolata Concezione");
		Holiday CHRISTMAS = new Holiday(Month.DECEMBER, 25, year, "Natale di Gesù");
		Holiday SAINT_STEPHEN_DAY = new Holiday(Month.DECEMBER, 26, year, "Santo Stefano");
		return Arrays.asList(NEW_YEAR_DAY, EPIPHANY, EASTER, EASTER_MONDAY, LIBERATION_DAY, LABOUR_DAY, REPUBLIC_DAY, ASSUMPTION_FEAST_DAY, ALL_SAINTS_DAY, IMMACULATE_CONCEPTION, CHRISTMAS, SAINT_STEPHEN_DAY);
	}

	public static boolean isPublicHoliday(@NonNull LocalDate date) {
		int month = date.getMonthValue();
		int dayOfMonth = date.getDayOfMonth();
		List<Holiday> italianNationalHolidays = getItalianNationalHolidaysForYear(date.getYear());
		boolean isHoliday = italianNationalHolidays.stream().anyMatch(h -> (h.getDayOfMonth() == dayOfMonth && h.getMonth().getMonthInYear() == month));
		boolean isSaturdayOrSunday = (date.getDayOfWeek() == DayOfWeek.SATURDAY) || (date.getDayOfWeek() == DayOfWeek.SUNDAY);
		return isHoliday || isSaturdayOrSunday;
	}

	public static boolean isPublicHoliday(@NonNull Month month, int dayOfMonth, int year) {
		List<Holiday> italianNationalHolidays = getItalianNationalHolidaysForYear(year);
		boolean isHoliday = italianNationalHolidays.stream().anyMatch(h -> (h.getDayOfMonth() == dayOfMonth && h.getMonth().getMonthInYear() == month.getMonthInYear()));
		LocalDate date = LocalDate.of(year, month.getMonthInYear(), dayOfMonth);
		boolean isSaturdayOrSunday = (date.getDayOfWeek() == DayOfWeek.SATURDAY) || (date.getDayOfWeek() == DayOfWeek.SUNDAY);
		return isHoliday || isSaturdayOrSunday;
	}

	private static Holiday calculateEasterDate(int year) {
        int a = year % 19,
            b = year / 100,
            c = year % 100,
            d = b / 4,
            e = b % 4,
            g = (8 * b + 13) / 25,
            h = (19 * a + b - d - g + 15) % 30,
            j = c / 4,
            k = c % 4,
            m = (a + 11 * h) / 319,
            r = (2 * e + 2 * j - k - h + m + 32) % 7,
            n = (h - m + r + 90) / 25,
            p = (h - m + r + n + 19) % 32;
        Holiday result = new Holiday();
        result.setName("Pasqua di Resurrezione");
        result.setYear(year);
        result.setDayOfMonth(p);
        switch(n) {
            case 1:
                result.setMonth(Month.JANUARY);
                break;
            case 2:
            	result.setMonth(Month.FEBRUARY);
                break;
            case 3:
            	result.setMonth(Month.MARCH);
                break;
            case 4:
            	result.setMonth(Month.APRIL);
                break;
            case 5:
            	result.setMonth(Month.MAY);
                break;
            case 6:
            	result.setMonth(Month.JUNE);
                break;
            case 7:
            	result.setMonth(Month.JULY);
                break;
            case 8:
            	result.setMonth(Month.AUGUST);
                break;
            case 9:
            	result.setMonth(Month.SEPTEMBER);
                break;
            case 10:
            	result.setMonth(Month.OCTOBER);
                break;
            case 11:
            	result.setMonth(Month.NOVEMBER);
                break;
            case 12:
            	result.setMonth(Month.DECEMBER);
                break;
        }
        return result;
    }

	private static LocalDate nextDay(@NonNull Month month, int dayOfMonth, int year) {
		LocalDate startDate = LocalDate.of(year, month.getMonthInYear(), dayOfMonth);
		return startDate.plusDays(1);
	}

}
