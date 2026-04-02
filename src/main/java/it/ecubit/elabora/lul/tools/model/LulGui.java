package it.ecubit.elabora.lul.tools.model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileSystemView;

import it.ecubit.elabora.lul.tools.zucchetti.PdfToExcelProcessor;

import java.awt.*;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.OutputStream;

public class LulGui extends JFrame {

    private final PdfToExcelProcessor processor;

    private JTextField pdfField;
    private JTextField outputField;
    private JTextArea logArea;
    private JProgressBar progressBar;
    private final Preferences prefs = Preferences.userNodeForPackage(LulGui.class);
    private static final String LAST_INPUT_DIR = "lastInputDir";
    private JButton openFileBtn;
    private Path lastOutputFile;

    public LulGui(PdfToExcelProcessor processor) {
        this.processor = processor;

        setTitle("LUL to Elabor@ converter");
        setSize(600, 450);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        initUi(); // la logArea viene creata QUI

        // SOLO ORA possiamo reindirizzare gli stream
        System.setOut(new PrintStream(new TextAreaOutputStream(logArea), true));
        System.setErr(new PrintStream(new TextAreaOutputStream(logArea), true));
    }

    private void initUi() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel);

        JPanel top = new JPanel(new GridLayout(4, 1, 5, 5));

        // ============================
        // PDF SELECTION
        // ============================
        JPanel pdfPanel = new JPanel(new BorderLayout(5, 5));
        pdfField = new JTextField();
        JButton pdfBtn = new JButton("Scegli PDF...");
        pdfBtn.addActionListener(e -> choosePdf());
        pdfPanel.add(new JLabel("PDF da elaborare:"), BorderLayout.WEST);
        pdfPanel.add(pdfField, BorderLayout.CENTER);
        pdfPanel.add(pdfBtn, BorderLayout.EAST);

        // ============================
        // OUTPUT FOLDER
        // ============================
        JPanel outPanel = new JPanel(new BorderLayout(5, 5));
        outputField = new JTextField();
        JButton outBtn = new JButton("Scegli cartella...");
        outBtn.addActionListener(e -> chooseOutputFolder());
        outPanel.add(new JLabel("Cartella output:"), BorderLayout.WEST);
        outPanel.add(outputField, BorderLayout.CENTER);
        outPanel.add(outBtn, BorderLayout.EAST);

        // ============================
        // START BUTTON
        // ============================
        JButton startBtn = new JButton("Elabora");
        startBtn.addActionListener(e -> startProcessing());

        top.add(pdfPanel);
        top.add(outPanel);
        top.add(startBtn);

        panel.add(top, BorderLayout.NORTH);

        openFileBtn = new JButton("Apri file");
        openFileBtn.setEnabled(false);
        openFileBtn.addActionListener(e -> openLastOutputFile());
        top.add(openFileBtn);

        // ============================
        // LOG AREA
        // ============================
        logArea = new JTextArea();
        logArea.setEditable(false);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(580, 250));
        panel.add(scroll, BorderLayout.CENTER);

        // ============================
        // PROGRESS BAR
        // ============================
        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        panel.add(progressBar, BorderLayout.SOUTH);
    }

    private void choosePdf() {
        JFileChooser chooser = new JFileChooser();

        // Recupera l'ultima cartella usata, se esiste
        String lastDir = prefs.get(LAST_INPUT_DIR, null);
        if (lastDir != null) {
            chooser.setCurrentDirectory(new File(lastDir));
        } else {
            // fallback: Documenti (qualsiasi lingua)
            FileSystemView fsv = FileSystemView.getFileSystemView();
            chooser.setCurrentDirectory(fsv.getDefaultDirectory());
        }

        chooser.setDialogTitle("Seleziona il PDF da elaborare");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            pdfField.setText(selected.getAbsolutePath());

            // Imposta automaticamente la cartella di output
            outputField.setText(selected.getParent());

            // Salva la cartella nelle preferenze
            prefs.put(LAST_INPUT_DIR, selected.getParent());
        }
    }

    private void chooseOutputFolder() {
        JFileChooser chooser = new JFileChooser();

        String lastDir = prefs.get(LAST_INPUT_DIR, null);
        if (lastDir != null) {
            chooser.setCurrentDirectory(new File(lastDir));
        } else {
            FileSystemView fsv = FileSystemView.getFileSystemView();
            chooser.setCurrentDirectory(fsv.getDefaultDirectory());
        }

        chooser.setDialogTitle("Seleziona la cartella di output");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            outputField.setText(selected.getAbsolutePath());

            // Salva la cartella scelta
            prefs.put(LAST_INPUT_DIR, selected.getAbsolutePath());
        }
    }

    private void startProcessing() {
        String pdfPath = pdfField.getText().trim();
        String outPath = outputField.getText().trim();

        if (pdfPath.isEmpty() || outPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleziona PDF e cartella di output.");
            return;
        }

        logArea.setText("");
        progressBar.setValue(0);

        new Thread(() -> {
            try {
                log("Inizio elaborazione...");
                progressBar.setIndeterminate(true);

                Path pdf = Path.of(pdfPath);
                Path out = Path.of(outPath);

                boolean ok = processor.process(pdf, out);

                String filename = processor.getLastGeneratedFilename();
                lastOutputFile = out.resolve(filename);

                if (ok && Files.exists(lastOutputFile)) {
                    log("File generato: " + lastOutputFile);
                    openFileBtn.setEnabled(true);
                } else {
                    log("ATTENZIONE: il file non è stato generato.");
                    openFileBtn.setEnabled(false);
                }

                log("Elaborazione completata.");

            } catch (Exception ex) {
                log("ERRORE: " + ex.getMessage());
            } finally {
                progressBar.setIndeterminate(false);
                progressBar.setValue(100);
            }
        }).start();
    }

    private void openLastOutputFile() {
        if (lastOutputFile != null && java.nio.file.Files.exists(lastOutputFile)) {
            try {
                Desktop.getDesktop().open(lastOutputFile.toFile());
            } catch (Exception ex) {
                log("ERRORE nell'apertura del file: " + ex.getMessage());
            }
        } else {
            log("Il file non esiste più.");
            openFileBtn.setEnabled(false);
        }
    }

    private void log(String msg) {
        SwingUtilities.invokeLater(() -> logArea.append(msg + "\n"));
    }

    private class TextAreaOutputStream extends OutputStream {
        private final JTextArea textArea;
        private final StringBuilder buffer = new StringBuilder();

        private static final String ESC = "\u001B";
        private static final Pattern ANSI_PATTERN = Pattern.compile(ESC + "\\[[;\\d]*m");
        private static final Pattern SPRING_LOG_PATTERN = Pattern.compile(
                "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2})\\s+INFO\\s+\\d+\\s+---\\s+\\[.*?]\\s+\\[.*?]\\s+.*?\\s{2}:\\s(.*)$");

        public TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            if (b == '\n') {
                flushBuffer();
            } else {
                buffer.append((char) b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String text = new String(b, off, len);
            for (char c : text.toCharArray()) {
                if (c == '\n') {
                    flushBuffer();
                } else {
                    buffer.append(c);
                }
            }
        }

        private void flushBuffer() {
            String line = buffer.toString();
            buffer.setLength(0);

            // Rimuove eventuali \r finali (Windows CRLF)
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }

            line = ANSI_PATTERN.matcher(line).replaceAll("");

            Matcher m = SPRING_LOG_PATTERN.matcher(line);
            if (m.matches()) {
                line = m.group(1) + " - " + m.group(2);
            }

            String finalLine = line;
            SwingUtilities.invokeLater(() -> textArea.append(finalLine + "\n"));
        }

    }
}