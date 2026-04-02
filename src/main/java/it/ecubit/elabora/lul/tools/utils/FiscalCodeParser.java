package it.ecubit.elabora.lul.tools.utils;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

public class FiscalCodeParser {

    private final String fiscalCode;

    private static final Map<Character, Month> MONTH_MAP = Map.ofEntries(
            Map.entry('A', Month.JANUARY),
            Map.entry('B', Month.FEBRUARY),
            Map.entry('C', Month.MARCH),
            Map.entry('D', Month.APRIL),
            Map.entry('E', Month.MAY),
            Map.entry('H', Month.JUNE),
            Map.entry('L', Month.JULY),
            Map.entry('M', Month.AUGUST),
            Map.entry('P', Month.SEPTEMBER),
            Map.entry('R', Month.OCTOBER),
            Map.entry('S', Month.NOVEMBER),
            Map.entry('T', Month.DECEMBER)
    );

    public FiscalCodeParser(String fiscalCode) {
        if (fiscalCode == null || fiscalCode.length() < 16) {
            throw new IllegalArgumentException("Codice fiscale non valido: " + fiscalCode);
        }
        this.fiscalCode = fiscalCode.toUpperCase();
    }

    // ------------------------------------------------------------
    // 1) CONSONANTI COGNOME (prime 3)
    // ------------------------------------------------------------
    public String getSurnameConsonants() {
        return fiscalCode.substring(0, 3);
    }

    // ------------------------------------------------------------
    // 2) CONSONANTI NOME (successive 3)
    // ------------------------------------------------------------
    public String getNameConsonants() {
        return fiscalCode.substring(3, 6);
    }

    // ------------------------------------------------------------
    // 3) ANNO DI NASCITA (2 cifre → 1900/2000)
    // ------------------------------------------------------------
    public int getBirthYear() {
        int yy = Integer.parseInt(fiscalCode.substring(6, 8));
        int currentYear = LocalDate.now().getYear() % 100;

        // Se > anno corrente → 1900 + yy, altrimenti 2000 + yy
        return (yy > currentYear) ? (1900 + yy) : (2000 + yy);
    }

    // ------------------------------------------------------------
    // 4) MESE DI NASCITA (lettera)
    // ------------------------------------------------------------
    public Month getBirthMonth() {
        char m = fiscalCode.charAt(8);
        Month month = MONTH_MAP.get(m);
        if (month == null) {
            throw new IllegalArgumentException("Lettera mese non valida nel CF: " + m);
        }
        return month;
    }

    // ------------------------------------------------------------
    // 5) GIORNO DI NASCITA (con offset per sesso)
    // ------------------------------------------------------------
    public int getBirthDay() {
        int day = Integer.parseInt(fiscalCode.substring(9, 11));
        return (day > 40) ? (day - 40) : day;
    }

    // ------------------------------------------------------------
    // 6) SESSO
    // ------------------------------------------------------------
    public String getGender() {
        int day = Integer.parseInt(fiscalCode.substring(9, 11));
        return (day > 40) ? "F" : "M";
    }

    // ------------------------------------------------------------
    // 7) DATA DI NASCITA COMPLETA
    // ------------------------------------------------------------
    public LocalDate getBirthDate() {
        return LocalDate.of(getBirthYear(), getBirthMonth(), getBirthDay());
    }

    // ------------------------------------------------------------
    // 8) CODICE COMUNE / STATO ESTERO
    // ------------------------------------------------------------
    public String getBirthPlaceCode() {
        return fiscalCode.substring(11, 15);
    }

    // ------------------------------------------------------------
    // 9) CARATTERE DI CONTROLLO (non validato qui)
    // ------------------------------------------------------------
    public char getControlChar() {
        return fiscalCode.charAt(15);
    }

    // ------------------------------------------------------------
    // 10) VALIDAZIONE MINIMA (solo lunghezza e charset)
    // ------------------------------------------------------------
    public boolean isValidBasic() {
        return fiscalCode.matches("^[A-Z0-9]{16}$");
    }
}
