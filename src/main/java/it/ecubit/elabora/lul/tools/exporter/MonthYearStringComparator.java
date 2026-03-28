package it.ecubit.elabora.lul.tools.exporter;

import java.time.LocalDate;
import java.util.Comparator;

import it.ecubit.elabora.lul.tools.enums.Month;
import lombok.NonNull;

public class MonthYearStringComparator implements Comparator<String> {

	@Override
	public int compare(@NonNull String monthYear1, @NonNull String monthYear2) {
		// Parse first and second strings as local dates
		LocalDate date1 = parseMonthYearAsDate(monthYear1);
		LocalDate date2 = parseMonthYearAsDate(monthYear2);
		return date1.compareTo(date2);
	}

	private LocalDate parseMonthYearAsDate(@NonNull String monthYear) {
		String[] parts = monthYear.split("\\s");
		Month month = Month.of(parts[0]);
		int year = Integer.parseInt(parts[1]);
		return LocalDate.of(year, month.getMonthInYear(), 1);
	}

}
