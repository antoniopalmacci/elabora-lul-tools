package it.ecubit.elabora.lul.tools.enums;

import java.util.NoSuchElementException;

import lombok.Getter;
import lombok.NonNull;

public enum WeekDay {
	MONDAY("LU", "lun", "Lunedì", 1),
	TUESDAY("MA", "mar", "Martedì", 2),
	WEDNESDAY("ME", "mer", "Mercoledì", 3),
	THURSDAY("GI", "gio", "Giovedì", 4),
	FRIDAY("VE", "ven", "Venerdì", 5),
	SATURDAY("SA", "sab", "Sabato", 6),
	SUNDAY("DO", "dom", "Domenica", 7);	

	private final @Getter String abbreviation;
	private final @Getter String shortName;
	private final @Getter String fullName;
	private final @Getter int dayInWeek;

	WeekDay(String abbreviation, String shortName, String fullName, int dayInWeek) {
		this.abbreviation = abbreviation;
		this.shortName = shortName;
		this.fullName = fullName;
		this.dayInWeek = dayInWeek;
	}

    public static WeekDay of(@NonNull String weekDayStr) throws NoSuchElementException {
        for(WeekDay weekDay: values()) {
            if(
            	weekDayStr.equalsIgnoreCase(weekDay.name()) ||
            	weekDayStr.equalsIgnoreCase(weekDay.getAbbreviation()) ||
            	weekDayStr.equalsIgnoreCase(weekDay.getShortName()) ||
            	weekDayStr.equalsIgnoreCase(weekDay.getFullName()) ||
            	weekDayStr.equals(String.valueOf(weekDay.getDayInWeek()))
            ) {
                return weekDay;
            }
        }
        throw new NoSuchElementException(String.format("Giorno della settimana non riconosciuto: %s", weekDayStr));
    }

    public String toString() {
    	return this.fullName;
    }

}
