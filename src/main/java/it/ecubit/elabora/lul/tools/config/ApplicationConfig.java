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
import org.springframework.util.StringUtils;

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

	/*
	 * @Override
	 * public void run(String... args) {
	 * int numArgs = args.length;
	 * if (numArgs < 1) {
	 * log.
	 * error("Missing arguments: at least PDF report filename should be specified as argument"
	 * );
	 * log.error(
	 * "Usage: java -jar <ELABORA TOOLS JAR> <PDF REPORT FILENAME> (-o <OUTPUT DIR>). The -o argument is optional and defaults to the directory in which PDF resides."
	 * );
	 * }
	 * try {
	 * String pdfPath = args[0];
	 * PdfDocumentInfo docInfo = pdfReportParser.inspect(pdfPath);
	 * log.info("PDF document info:\n" + docInfo.toString());
	 * List<String> pages = pdfReportParser.extractTextFromAllPages(pdfPath);
	 * log.info(String.format("Extracted text content from %d pages",
	 * pages.size()));
	 * int pageIndex = 1;
	 * Map<String, List<MonthlyReport>> reportsForMonthMap = new HashMap<>();
	 * for (String page : pages) {
	 * try {
	 * MonthlyReport monthlyReport = pdfReportParser.parsePageContent(page);
	 * String employeeNameSurname = monthlyReport.getEmployee().getName() + " "
	 * + monthlyReport.getEmployee().getSurname();
	 * String englishMonthName =
	 * StringUtils.capitalize(monthlyReport.getMonth().getName().toLowerCase());
	 * log.info(String.
	 * format("Loaded %s monthly report for employee %s from page %d",
	 * englishMonthName,
	 * employeeNameSurname, pageIndex));
	 * String key = monthlyReport.getMonth() + " " + monthlyReport.getYear();
	 * if (!reportsForMonthMap.containsKey(key)) {
	 * reportsForMonthMap.put(key, new ArrayList<>());
	 * }
	 * reportsForMonthMap.get(key).add(monthlyReport);
	 * 
	 * 
	 * pageIndex += 1;
	 * } catch (Exception exc) {
	 * log.error("Unable to parse monthly report from page: " + exc.getMessage());
	 * }
	 * }
	 * 
	 * Path exportDirPath = Paths.get(docInfo.getFilename()).getParent();
	 * if (numArgs >= 3 && args[1].equalsIgnoreCase("-o")) {
	 * exportDirPath = Paths.get(args[2]);
	 * }
	 * exportDirPath.toFile().mkdirs();
	 * String pdfFilename =
	 * Paths.get(docInfo.getFilename()).getFileName().toString();
	 * String excelFilename = "Lul-" + FilenameUtils.removeExtension(pdfFilename) +
	 * ".xlsx";
	 * log.info(String.format("Exporting data to %s Excel LUL template in dir %s",
	 * excelFilename,
	 * exportDirPath.toString()));
	 * excelLulExporter.export(reportsForMonthMap, exportDirPath, excelFilename);
	 * log.info("Done! Export completed successfully");
	 * } catch (IOException ioExc) {
	 * log.error("IOException occurred", ioExc);
	 * }
	 * }
	 */
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

			for (String page : pages) {
				if (page.contains("RIEPILOGO GENERALE")) {
					log.info(String.format("Skipping summary page %d (RIEPILOGO GENERALE)", pageIndex));
					pageIndex++;
					continue;
				}

				try {
					MonthlyReport partial = monthlyReportParser.parsePageContent(page);
					// --- IGNORA LE PAGINE DI RIEPILOGO GENERALE ---
					Employee pe = partial.getEmployee();

					boolean isPageA = pe != null && pe.getId() != null;
					boolean isPageB = !isPageA && pe != null; // Page B ha employee ma senza ID

					// ============================
					// PAGE TYPE A → NUOVO CEDOLINO
					// ============================
					if (isPageA) {

						// chiudi il cedolino precedente
						if (currentReport != null) {
							String key = currentReport.getMonth() + " " + currentReport.getYear();
							reportsForMonthMap
									.computeIfAbsent(key, k -> new ArrayList<>())
									.add(currentReport);
						}

						currentReport = partial;

						String employeeNameSurname = currentReport.getEmployee().getName() + " " +
								currentReport.getEmployee().getSurname();
						String englishMonthName = StringUtils.capitalize(
								currentReport.getMonth().getName().toLowerCase());

						log.info(String.format(
								"Loaded %s monthly report for employee %s from page %d (PageTypeA)",
								englishMonthName,
								employeeNameSurname,
								pageIndex));
					}

					// ==========================================
					// PAGE TYPE B → ARRICCHISCE IL CEDOLINO CORRENTE
					// ==========================================
					if (isPageB && currentReport != null) {

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

					}

				} catch (Exception exc) {
					log.error("Unable to parse monthly report from page " + pageIndex + ": " + exc.getMessage());
				}

				pageIndex += 1;
			}

			// chiudi l’ultimo cedolino se presente
			if (currentReport != null) {
				String key = currentReport.getMonth() + " " + currentReport.getYear();
				reportsForMonthMap
						.computeIfAbsent(key, k -> new ArrayList<>())
						.add(currentReport);
			}

			// ============================
			// EXPORT
			// ============================
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

	private MonthlyReport mergeReports(List<MonthlyReport> partials) {

		MonthlyReport finalReport = new MonthlyReport();
		Employee employee = new Employee();

		for (MonthlyReport partial : partials) {

			Employee pe = partial.getEmployee();

			// PageTypeA → copia tutto tranne headquarters
			if (pe.getId() != null) {
				employee.setId(pe.getId());
				employee.setName(pe.getName());
				employee.setSurname(pe.getSurname());
				employee.setAddress(pe.getAddress());
				employee.setCap(pe.getCap());
				employee.setFiscalCode(pe.getFiscalCode());
				employee.setCompany(pe.getCompany());

				finalReport.setMonth(partial.getMonth());
				finalReport.setYear(partial.getYear());
				finalReport.setWorkContractType(partial.getWorkContractType());
				finalReport.setDailyReports(partial.getDailyReports());
			}

			// PageTypeB → copia solo headquarters
			if (pe.getHeadquarters() != null) {
				employee.setHeadquarters(pe.getHeadquarters());
			}
		}

		finalReport.setEmployee(employee);
		return finalReport;
	}
}
