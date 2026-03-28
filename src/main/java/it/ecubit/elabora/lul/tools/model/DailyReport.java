package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;

import org.apache.commons.math3.util.Pair;

import it.ecubit.elabora.lul.tools.enums.AbsenceType;
import it.ecubit.elabora.lul.tools.enums.WeekDay;
import it.ecubit.elabora.lul.tools.enums.WorkAttendanceType;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@EqualsAndHashCode
public class DailyReport implements Serializable {

	private static final long serialVersionUID = 3653696124341974718L;
	
	private int day;
	private WeekDay weekDay;
	private Pair<WorkAttendanceType, Integer> ordinaryWorkAttendanceMinutes;
	private Pair<WorkAttendanceType, Integer> extraWorkAttendanceMinutes;
	private Pair<AbsenceType, Integer> abscenceMinutes;

	public boolean isPresent() {
		// at least one minute of effective work in the day
		int ordinaryWorkMinutes = (ordinaryWorkAttendanceMinutes != null && ordinaryWorkAttendanceMinutes.getSecond() != null)? ordinaryWorkAttendanceMinutes.getSecond() : 0;
		int extraWorkMinutes = (extraWorkAttendanceMinutes != null && extraWorkAttendanceMinutes.getSecond() != null)? extraWorkAttendanceMinutes.getSecond() : 0;
		return (ordinaryWorkMinutes + extraWorkMinutes) > 0;
	}

	public float getTotWorkingHours() {
		// effective working hours in the day
		int ordinaryWorkMinutes = (ordinaryWorkAttendanceMinutes != null && ordinaryWorkAttendanceMinutes.getSecond() != null)? ordinaryWorkAttendanceMinutes.getSecond() : 0;
		int extraWorkMinutes = (extraWorkAttendanceMinutes != null && extraWorkAttendanceMinutes.getSecond() != null)? extraWorkAttendanceMinutes.getSecond() : 0;
		return (((float) (ordinaryWorkMinutes + extraWorkMinutes)) / 60.0f);
	}

	public float getTotNonWorkingHours() {
		// effective working hours in the day
		int nonWorkingMinutes = (abscenceMinutes != null && abscenceMinutes.getSecond() != null)? abscenceMinutes.getSecond() : 0;
		return (((float) nonWorkingMinutes) / 60.0f);
	}

}
