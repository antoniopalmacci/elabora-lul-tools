package it.ecubit.elabora.lul.tools.zucchetti;

import java.nio.file.Path;
import java.util.*;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;

import it.ecubit.elabora.lul.tools.exporter.ExcelLulExporter;
import it.ecubit.elabora.lul.tools.model.Employee;
import it.ecubit.elabora.lul.tools.model.MonthlyReport;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PdfToExcelProcessor {

    private final PdfReportParser pdfReportParser;
    private final MonthlyReportParser monthlyReportParser;
    private final ExcelLulExporter excelLulExporter;
    private String lastGeneratedFilename;

    public String getLastGeneratedFilename() {
        return lastGeneratedFilename;
    }

    public PdfToExcelProcessor(
            PdfReportParser pdfReportParser,
            MonthlyReportParser monthlyReportParser,
            ExcelLulExporter excelLulExporter) {

        this.pdfReportParser = pdfReportParser;
        this.monthlyReportParser = monthlyReportParser;
        this.excelLulExporter = excelLulExporter;
    }

    public interface ProgressListener {
        void onProgress(int percent);
    }

    private ProgressListener listener;

    public void setProgressListener(ProgressListener listener) {
        this.listener = listener;
    }

    private void updateProgress(int percent) {
        if (listener != null) {
            listener.onProgress(percent);
        }
    }

    public boolean process(Path pdfPath, Path outputDir) {
        excelLulExporter.setProgressListener(this::updateProgress);

        try {
            updateProgress(0);
            log.info("Analisi PDF: {}", pdfPath);

            List<String> pages = pdfReportParser.extractTextFromAllPages(pdfPath.toString());
            int totalPages = pages.size();
            log.info("Estratte {} pagine", totalPages);
            double perPage = 20.0 / totalPages;
            double[] progressArr = { 0 };

            Map<String, List<MonthlyReport>> reportsForMonthMap = new HashMap<>();

            MonthlyReport currentReport = null;
            boolean lastWasPageA = false;
            String lastFiscalCodeForPageB = null;

            int pageIndex = 1;

            for (String page : pages) {

                if (page.contains("RIEPILOGO GENERALE")) {
                    log.info("Ignoro pagina {} (RIEPILOGO GENERALE)", pageIndex);
                    advancePage(perPage, progressArr, pageIndex);
                    pageIndex++;
                    continue;
                }

                try {
                    MonthlyReport partial = monthlyReportParser.parsePageContent(page);
                    Employee pe = partial.getEmployee();

                    boolean isPageA = pe != null && pe.getId() != null;
                    boolean isPageB = pe != null && pe.getId() == null;

                    Objects.requireNonNull(pe, "pe è null!");
                    if (isPageA) {

                        if (currentReport != null && currentReport.getMonth() != null) {
                            String key = currentReport.getMonth() + " " + currentReport.getYear();
                            reportsForMonthMap.computeIfAbsent(key, k -> new ArrayList<>()).add(currentReport);
                        }

                        currentReport = partial;
                        lastWasPageA = true;

                        log.info("Caricato cedolino A per {} {} (pagina {})",
                                pe.getSurname(), pe.getName(), pageIndex);

                        advancePage(perPage, progressArr, pageIndex);
                        pageIndex++;
                        continue;
                    }

                    if (isPageB) {

                        boolean isSameEmployeeAsPreviousB = !lastWasPageA &&
                                lastFiscalCodeForPageB != null &&
                                pe.getFiscalCode() != null &&
                                pe.getFiscalCode().equalsIgnoreCase(lastFiscalCodeForPageB);

                        if (isSameEmployeeAsPreviousB) {
                            log.info("Ignoro pagina B duplicata per {} {} (pagina {})",
                                    pe.getSurname(), pe.getName(), pageIndex);
                            advancePage(perPage, progressArr, pageIndex);
                            pageIndex++;
                            continue;
                        }

                        lastFiscalCodeForPageB = pe.getFiscalCode();

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
                            advancePage(perPage, progressArr, pageIndex);
                            pageIndex++;
                            continue;
                        }

                        if (!lastWasPageA) {

                            if (currentReport != null && currentReport.getMonth() != null) {
                                String key = currentReport.getMonth() + " " + currentReport.getYear();
                                reportsForMonthMap.computeIfAbsent(key, k -> new ArrayList<>()).add(currentReport);
                            }

                            currentReport = partial;
                            lastWasPageA = false;

                            log.info("Caricato cedolino B senza A per {} {} (pagina {})",
                                    pe.getSurname(), pe.getName(), pageIndex);

                            advancePage(perPage, progressArr, pageIndex);
                            pageIndex++;
                            continue;
                        }
                    }

                } catch (Exception exc) {
                    log.error("Errore parsing pagina {}: {}", pageIndex, exc.getMessage());
                    advancePage(perPage, progressArr, pageIndex);
                    pageIndex++;
                }
            }

            if (currentReport != null && currentReport.getMonth() != null) {
                String key = currentReport.getMonth() + " " + currentReport.getYear();
                reportsForMonthMap.computeIfAbsent(key, k -> new ArrayList<>()).add(currentReport);
            }

            outputDir.toFile().mkdirs();

            this.lastGeneratedFilename = OutputFilename(pdfPath);
            log.info("Esporto Excel: {}", this.lastGeneratedFilename);

            if (!excelLulExporter.export(reportsForMonthMap, outputDir, this.lastGeneratedFilename)) {
                return false;
            }

            log.info("Esportazione completata");
            return true;

        } catch (Exception e) {
            log.error("Errore durante l'elaborazione: {}", e.getMessage());
            return false;
        }
    }

    public String OutputFilename(Path pdfPath) {
        String pdfFilename = pdfPath.getFileName().toString();
        String excelFilename = "Lul-" + FilenameUtils.removeExtension(pdfFilename) + ".xlsx";
        return excelFilename;
    }

    private void advancePage(double perPage, double[] progress, int pageIndex) {
        progress[0] += perPage;
        updateProgress((int) progress[0]);
    }

}
