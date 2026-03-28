package it.ecubit.elabora.lul.tools.enums;

import java.util.NoSuchElementException;

import lombok.Getter;
import lombok.NonNull;

public enum AbsenceType {
	FER("FE", "Ferie", "E"), // first absence column
	MAL("MA", "Malattia", "F"),
	IN("IN", "Infortunio", "F"),
	INT("IR", "Assenza per intervento", "F"),
	PR("RL", "Permesso retribuito", "G"),
	CP("CP", "Congedo genitoriale retribuito", "G"),
	CGR("A8", "Congedo genitoriale retribuito", "G"),
	PG("PG", "Perm. disabili", "G"),
	CD("CD", "Congedi str.dis.", "G"),
	AP("AP", "Aspettativa non retribuita", "H"),
	PNR("PN", "Permesso non retribuito", "H"),
	NL("NL", "Non lavorato", "H"),
	PS("P1", "Permesso studio", "M"),
	DS("DS", "Donazione sangue", "O"),
	MAT("MT", "Maternità obbligatoria", "P"),
	ALL("AL", "Allattamento", "P"),
	NOT("ON", "Orario notturno", "R"),
	SC("SC", "Sciopero", "S"),
	CM("CM", "Congedo matrimoniale", "T"),
	L104("PH", "Assenza legge 104", "U"),
	CIG("CA", "Cassa integrazione", "W"),
	ASS("A1", "Assegno ordinario", "W"),
	LUT("PL", "Assenza per lutto", "Y"), // last absence column
	DIM("AH", "Assenza per dimissioni", null),
	RE("RE", "Reperibilità", null);

	private final @Getter String code;
	private final @Getter String description;
	private final @Getter String columnName;

	AbsenceType(String code, String description, String columnName) {
		this.code = code;
		this.description = description;
		this.columnName = columnName;
	}

	public static AbsenceType of(@NonNull String absenceTypeStr) throws NoSuchElementException {
		for (AbsenceType absenceType : values()) {
			if (absenceTypeStr.equalsIgnoreCase(absenceType.name()) ||
					absenceTypeStr.equalsIgnoreCase(absenceType.getCode()) ||
					absenceTypeStr.equalsIgnoreCase(absenceType.getDescription())) {
				return absenceType;
			}
		}
		throw new NoSuchElementException(String.format("Tipologia di assenza non riconosciuta: %s", absenceTypeStr));
	}

	public String toString() {
		return this.description;
	}

}
