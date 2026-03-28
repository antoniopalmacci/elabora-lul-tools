package it.ecubit.elabora.lul.tools.enums;

import java.util.NoSuchElementException;

import lombok.Getter;
import lombok.NonNull;

public enum Month {
	JANUARY("gen", "Gennaio", 1),
	FEBRUARY("feb", "Febbraio", 2),
	MARCH("mar", "Marzo", 3),
	APRIL("apr", "Aprile", 4),
	MAY("mag", "Maggio", 5),
	JUNE("giu", "Giugno", 6),
	JULY("lug", "Luglio", 7),
	AUGUST("ago", "Agosto", 8),
	SEPTEMBER("set", "Settembre", 9),
	OCTOBER("ott", "Ottobre", 10),
	NOVEMBER("nov", "Novembre", 11),
	DECEMBER("dic", "Dicembre", 12);

	private final @Getter String abbreviation;
	private final @Getter String name;
	private final @Getter int monthInYear;

	Month(String abbreviation, String name, int monthInYear) {
		this.abbreviation = abbreviation;
		this.name = name;
		this.monthInYear = monthInYear;
	}

    public static Month of(@NonNull String monthStr) throws NoSuchElementException {
        for(Month month: values()) {
            if(
            	monthStr.equalsIgnoreCase(month.name()) ||
            	monthStr.equalsIgnoreCase(month.getAbbreviation()) ||
            	monthStr.equalsIgnoreCase(month.getName()) ||
            	monthStr.equals(String.valueOf(month.getMonthInYear()))
            ) {
                return month;
            }
        }
        throw new NoSuchElementException(String.format("Mese non riconosciuto: %s", monthStr));
    }

    public String toString() {
    	return this.name;
    }


}
