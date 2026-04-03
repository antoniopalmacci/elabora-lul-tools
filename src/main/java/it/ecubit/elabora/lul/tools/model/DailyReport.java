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
    private Pair<WorkAttendanceType, Integer> ordinaryWorkAttendanceMinutes; // minuti
    private Pair<WorkAttendanceType, Integer> extraWorkAttendanceMinutes;    // minuti
    private List<Pair<AbsenceType, Integer>> abscenceMinutes;               // minuti

    // ------------------------------------------------------------
    // PRESENZA
    // ------------------------------------------------------------
    public boolean isPresent() {
        return getWorkedMinutes() > 0;
    }

    // ------------------------------------------------------------
    // MINUTI
    // ------------------------------------------------------------

    /** Minuti ordinari */
    public int getOrdinaryMinutes() {
        if (ordinaryWorkAttendanceMinutes == null || ordinaryWorkAttendanceMinutes.getRight() == null)
            return 0;
        return ordinaryWorkAttendanceMinutes.getRight();
    }

    /** Minuti straordinari o festivi */
    public int getExtraMinutes() {
        if (extraWorkAttendanceMinutes == null || extraWorkAttendanceMinutes.getRight() == null)
            return 0;
        return extraWorkAttendanceMinutes.getRight();
    }

    /** Minuti lavorati totali (ordinarie + straordinarie/festive) */
    public int getWorkedMinutes() {
        return getOrdinaryMinutes() + getExtraMinutes();
    }

    /** Minuti non lavorati (assenze) */
    public int getNonWorkedMinutes() {
        int total = 0;
        if (abscenceMinutes != null) {
            for (Pair<AbsenceType, Integer> p : abscenceMinutes) {
                if (p != null && p.getRight() != null) {
                    total += p.getRight();
                }
            }
        }
        return total;
    }

    /** Minuti totali (lavorati + assenze) */
    public int getTotalMinutes() {
        return getWorkedMinutes() + getNonWorkedMinutes();
    }

    // ------------------------------------------------------------
    // ORE (float)
    // ------------------------------------------------------------

    /** Ore lavorate totali (ordinarie + straordinarie/festive) */
    public float getWorkedHours() {
        return getWorkedMinutes() / 60.0f;
    }

    /** Ore straordinarie/festive */
    public float getExtraHours() {
        return getExtraMinutes() / 60.0f;
    }

    /** Ore ordinarie */
    public float getOrdinaryHours() {
        return getOrdinaryMinutes() / 60.0f;
    }

    /** Ore non lavorate (assenze) */
    public float getNonWorkedHours() {
        return getNonWorkedMinutes() / 60.0f;
    }

    /** Ore totali (lavorate + assenze) */
    public float getTotalHours() {
        return getTotalMinutes() / 60.0f;
    }

    // ------------------------------------------------------------
    // Compatibilità con i vecchi metodi
    // ------------------------------------------------------------

    /*
    public float getTotWorkingHours() {
        return getWorkedHours();
    }

 
    public float getTotNonWorkingHours() {
        return getNonWorkedHours();
    }
        */
}
