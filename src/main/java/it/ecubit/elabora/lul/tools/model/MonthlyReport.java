package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import it.ecubit.elabora.lul.tools.enums.Month;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class MonthlyReport implements Serializable {

	private static final long serialVersionUID = -4089282504683358729L;

	private Month month;
	private int year;
	private List<DailyReport> dailyReports = new ArrayList<>();
	private Employee employee;
	private String workContractType;

	public void addDailyReport(@NonNull DailyReport dailyReport) {
		dailyReports.add(dailyReport);
	}

}
