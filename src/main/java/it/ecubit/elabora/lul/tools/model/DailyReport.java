package it.ecubit.elabora.lul.tools.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

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
    private List<Pair<AbsenceType, Integer>> abscenceMinutes;

    public boolean isPresent() {
        int ordinaryWorkMinutes = (ordinaryWorkAttendanceMinutes != null && ordinaryWorkAttendanceMinutes.getRight() != null)? ordinaryWorkAttendanceMinutes.getRight() : 0;
        int extraWorkMinutes = (extraWorkAttendanceMinutes != null && extraWorkAttendanceMinutes.getRight() != null)? extraWorkAttendanceMinutes.getRight() : 0;
        return (ordinaryWorkMinutes + extraWorkMinutes) > 0;
    }

    public float getTotWorkingHours() {
        int ordinaryWorkMinutes = (ordinaryWorkAttendanceMinutes != null && ordinaryWorkAttendanceMinutes.getRight() != null)? ordinaryWorkAttendanceMinutes.getRight() : 0;
        int extraWorkMinutes = (extraWorkAttendanceMinutes != null && extraWorkAttendanceMinutes.getRight() != null)? extraWorkAttendanceMinutes.getRight() : 0;
        return (((float) (ordinaryWorkMinutes + extraWorkMinutes)) / 60.0f);
    }

    public float getTotNonWorkingHours() {
        int nonWorkingMinutes = 0;

        if (abscenceMinutes != null) {
            for (Pair<AbsenceType, Integer> p : abscenceMinutes) {
                if (p != null && p.getRight() != null) {
                    nonWorkingMinutes += p.getRight();
                }
            }
        }

        return (((float) nonWorkingMinutes) / 60.0f);
    }
}
