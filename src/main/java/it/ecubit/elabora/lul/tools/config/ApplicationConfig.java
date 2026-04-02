package it.ecubit.elabora.lul.tools.config;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import it.ecubit.elabora.lul.tools.exporter.ExcelLulExporter;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import it.ecubit.elabora.lul.tools.model.PdfDocumentInfo;
import it.ecubit.elabora.lul.tools.zucchetti.PdfReportParser;
import it.ecubit.elabora.lul.tools.zucchetti.MonthlyReportParser;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@ComponentScan("it.ecubit.elabora.lul.tools")
@Slf4j
public class ApplicationConfig implements CommandLineRunner {

	@Autowired
	PdfReportParser pdfReportParser;

	@Autowired
	ExcelLulExporter excelLulExporter;

	@Autowired
	MonthlyReportParser monthlyReportParser;

	public static void main(String[] args) {
		SpringApplication.run(ApplicationConfig.class, args);
	}

	@Override
	public void run(String... args) {
		int numArgs = args.length;
		if (numArgs < 1) {
			log.error("Missing arguments: at least PDF report filename should be specified as argument");
			log.error(
					"Usage: java -jar <ELABORA TOOLS JAR> <PDF REPORT FILENAME> (-o <OUTPUT DIR>). The -o argument is optional and defaults to the directory in which PDF resides.");
			return;
		}

		try {
			String pdfPath = args[0];
			PdfDocumentInfo docInfo = pdfReportParser.inspect(pdfPath);
			log.info("PDF document info:\n" + docInfo.toString());

			List<String> pages = pdfReportParser.extractTextFromAllPages(pdfPath);
			log.info(String.format("Extracted text content from %d pages", pages.size()));

			int pageIndex = 1;
			Map<String, List<MonthlyReport>> reportsForMonthMap = new HashMap<>();

			MonthlyReport currentReport = null;
			boolean lastWasPageA = false;
			String lastFiscalCodeForPageB = null;

			for (String page : pages) {

				// --- IGNORA LE PAGINE DI RIEPILOGO GENERALE ---
				if (page.contains("RIEPILOGO GENERALE")) {
					log.info(String.format("Skipping summary page %d (RIEPILOGO GENERALE)", pageIndex));
					pageIndex++;
					continue;
				}

				try {
					MonthlyReport partial = monthlyReportParser.parsePageContent(page);
					Employee pe = partial.getEmployee();

					boolean isPageA = pe != null && pe.getId() != null;
					boolean isPageB = pe != null && pe.getId() == null;

					// ==============================================================================================
					// PAGE TYPE A → NUOVO CEDOLINO → pagina presenze
					// Inizializziamo i campi id, name, surname, address, cap, fiscalCode e company
					// dell'employee;
					// ==============================================================================================
					if (isPageA) {

						// chiudi il precedente
						if (currentReport != null && currentReport.getMonth() != null) {
							String key = currentReport.getMonth() + " " + currentReport.getYear();
							reportsForMonthMap
									.computeIfAbsent(key, k -> new ArrayList<>())
									.add(currentReport);
						}

						currentReport = partial;
						lastWasPageA = true;

						log.info(String.format(
								"Loaded monthly report for employee %s %s from page %d (Page Type A)",
								currentReport.getEmployee().getSurname(),
								currentReport.getEmployee().getName(),
								pageIndex));

						pageIndex++;
						continue;
					}

					// ==============================================================================================
					// PAGE TYPE B → pagina busta paga
					// ==============================================================================================
					if (isPageB) {

						// --- Nuova regola: ignora pagine B duplicate dello stesso dipendente ---
						boolean isSameEmployeeAsPreviousB = !lastWasPageA &&
								lastFiscalCodeForPageB != null &&
								pe.getFiscalCode() != null &&
								pe.getFiscalCode().equalsIgnoreCase(lastFiscalCodeForPageB);

						if (isSameEmployeeAsPreviousB) {
							log.info(String.format(
									"Skipping duplicate Page Type B for employee %s %s (page %d)",
									pe.getSurname(), pe.getName(), pageIndex));
							pageIndex++;
							continue;
						}

						// aggiorna il codice fiscale dell’ultima pagina B valida
						lastFiscalCodeForPageB = pe.getFiscalCode();

						// ==============================================================================================
						// Caso 1: la pagina busta paga segue la pagina presenze dello stesso dipendente
						// In questo caso arricchisce l'employee, già inizializzato in PAGE A, con i campi mancanti
						// (headquarters, employeeCode, level, birthDate, hireDate, terminationDate)
						// ==============================================================================================
						if (lastWasPageA && currentReport != null) {

							Employee target = currentReport.getEmployee();

							if (pe.getHeadquarters() != null)
								target.setHeadquarters(pe.getHeadquarters());

							if (pe.getEmployeeCode() != null)
								target.setEmployeeCode(pe.getEmployeeCode());

							if (pe.getLevel() != null)
								target.setLevel(pe.getLevel());

							if (pe.getBirthDate() != null)
								target.setBirthDate(pe.getBirthDate());

							if (pe.getHireDate() != null)
								target.setHireDate(pe.getHireDate());

							if (pe.getTerminationDate() != null)
								target.setTerminationDate(pe.getTerminationDate());

							lastWasPageA = false;
							pageIndex++;
							continue;
						}

						// ==============================================================================================
						// Caso 2: la pagina busta paga segue un'altra busta paga di un diverso dipendente
						// In questo caso crea un nuovo emplyee id e inizializza solo i campi name, surname, fiscalCode,
						// company, headquarters, employeeCode, level, birthDate, hireDate, terminationDate dell'employee
						// ==============================================================================================
						if (!lastWasPageA) {

							// chiudi il precedente
							if (currentReport != null && currentReport.getMonth() != null) {
								String key = currentReport.getMonth() + " " + currentReport.getYear();
								reportsForMonthMap
										.computeIfAbsent(key, k -> new ArrayList<>())
										.add(currentReport);
							}

							currentReport = partial;
							lastWasPageA = false;

							log.info(String.format(
									"Loaded monthly report for employee %s %s from page %d (Page Type B without A)",
									currentReport.getEmployee().getSurname(),
									currentReport.getEmployee().getName(),
									pageIndex));

							pageIndex++;
							continue;
						}
					}

				} catch (Exception exc) {
					log.error("Unable to parse monthly report from page " + pageIndex + ": " + exc.getMessage());
					pageIndex++;
				}
			}

			// chiudi l’ultimo cedolino se presente
			if (currentReport != null && currentReport.getMonth() != null) {
				String key = currentReport.getMonth() + " " + currentReport.getYear();
				reportsForMonthMap
						.computeIfAbsent(key, k -> new ArrayList<>())
						.add(currentReport);
			}

			Path exportDirPath = Paths.get(docInfo.getFilename()).getParent();
			if (numArgs >= 3 && args[1].equalsIgnoreCase("-o")) {
				exportDirPath = Paths.get(args[2]);
			}
			exportDirPath.toFile().mkdirs();

			String pdfFilename = Paths.get(docInfo.getFilename()).getFileName().toString();
			String excelFilename = "Lul-" + FilenameUtils.removeExtension(pdfFilename) + ".xlsx";

			log.info(String.format("Exporting data to %s Excel LUL template in dir %s",
					excelFilename, exportDirPath.toString()));

			excelLulExporter.export(reportsForMonthMap, exportDirPath, excelFilename);
			log.info("Done! Export completed successfully");

		} catch (IOException ioExc) {
			log.error("IOException occurred", ioExc);
		}
	}
}
