package it.ecubit.elabora.lul.tools.enums;

import java.util.NoSuchElementException;

import lombok.NonNull;

public enum WorkAttendanceType {
	ORDINARIA,
	STRAORDINARIA,
	NOTTURNA,
	FESTIVA;

    public static WorkAttendanceType of(@NonNull String attendanceTypeStr) throws NoSuchElementException {
        for(WorkAttendanceType attendanceType: values()) {
            if(attendanceTypeStr.equalsIgnoreCase(attendanceType.name())) {
                return attendanceType;
            }
        }
        throw new NoSuchElementException(String.format("Tipologia di presenza lavorativa non riconosciuta: %s", attendanceTypeStr));
    }

    public String toString() {
    	return this.name().substring(0, 1).toUpperCase() + this.name().substring(1).toLowerCase();
    }

}
