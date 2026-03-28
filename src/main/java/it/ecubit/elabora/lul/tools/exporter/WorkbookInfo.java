package it.ecubit.elabora.lul.tools.exporter;

public class WorkbookInfo {

	public static final String[] SHEET_COLUMN_NAMES = {
			"Cognome", "Nome", "Giorno", "Presenza", "Ferie", "Malattia", "Permessi Retribuiti",
			"Permessi non Retribuiti",
			"Trasferta", "Festa Patronale", "recuperi", "permessi_da_recuperare", "permessi_studio",
			"permessi_sindacali",
			"donazione_sangue", "maternita", "maternita_facoltativa", "straordinari", "sciopero",
			"congedo_matrimoniale",
			"L104", "riposo_compensativo", "cig", "rol", "lutto", "ore lavorate", "ore non lavorate", "Totale",
			"Codice Fiscale"
	};
	public static final int[] SHEET_COLUMN_WIDTHS = {
			256 * 30, 256 * 30, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15,
			256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15,
			256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15,
			256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 15, 256 * 30
	};
	public static final String HEADER_BACKGOUND_COLOR = "#FCFBCF"; // rgb(252, 251, 207)
	public static final String TEMPLATE_SHEET_NAME = "Template";

}
