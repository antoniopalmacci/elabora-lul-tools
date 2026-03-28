package it.ecubit.elabora.lul.tools.utils;

import java.io.Serializable;
import java.time.LocalDate;

import it.ecubit.elabora.lul.tools.enums.Month;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"month", "dayOfMonth", "year"})
public class Holiday implements Serializable {

	private static final long serialVersionUID = 3724058510208255925L;

	public Holiday(@NonNull LocalDate localDate, @NonNull String holidayName) {
		this.month = Month.of(String.valueOf(localDate.getMonthValue()));
		this.dayOfMonth = localDate.getDayOfMonth();
		this.year = localDate.getYear();
		this.name = holidayName;
	}

	private Month month;
	private int dayOfMonth;
	private int year;
	private String name;

	public String toString() {
		return String.valueOf(dayOfMonth) + "/" + String.valueOf(month.getMonthInYear()) + "/" + year + " (" + name + ")";
	}

}
