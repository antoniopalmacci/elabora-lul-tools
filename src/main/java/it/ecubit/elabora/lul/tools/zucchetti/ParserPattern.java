package it.ecubit.elabora.lul.tools.zucchetti;

import java.util.regex.Pattern;

public class ParserPattern {

	public static final String COMPANY = "AZIENDA";
	public static final Pattern EMPLOYEE_ID_REGEX_PATTERN = Pattern.compile("\\d{6,10}/\\d{6,10}/\\d{6,10}/?");
	public static final Pattern WEEK_DAY_NUM_PATTERN = Pattern.compile("(LU|MA|ME|GI|VE|SA|DO)\\s\\d(\\d)?(\\s)?");

}
