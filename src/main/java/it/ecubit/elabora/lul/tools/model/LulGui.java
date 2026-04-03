package it.ecubit.elabora.lul.tools.model;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import it.ecubit.elabora.lul.tools.zucchetti.PdfToExcelProcessor;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.OutputStream;
import javax.swing.event.DocumentEvent;
import javax.swing.text.Style;

public class LulGui extends JFrame {

    private abstract class SimpleDocumentListener implements DocumentListener {
        public abstract void update(DocumentEvent e);

        @Override
        public void insertUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update(e); // non verrà mai chiamato per JTextField, ma serve per completezza
        }
    }

    private final PdfToExcelProcessor processor;

    private JTextField pdfField;
    private JTextField outputField;
    private JTextPane logArea = new JTextPane();

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
        System.setOut(new PrintStream(new TextPaneOutputStream(), true));
        System.setErr(new PrintStream(new TextPaneOutputStream(), true));
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
        outputField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update(DocumentEvent e) {
                updateOutputFieldColor();
            }
        });

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
        logArea = new JTextPane();
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

        // Validazione cartella di output
        if (!validateOutputDirectory(outPath)) {
            return; // blocca l’elaborazione
        }

        if (pdfPath.isEmpty() || outPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Seleziona PDF e cartella di output.");
            return;
        }

        String excelFilename = processor.OutputFilename(Path.of(pdfPath));
        Path outputDir = Paths.get(outPath);
        Path outputPath = outputDir.resolve(excelFilename);

        if (checkIfFileIsLocked(outputPath, excelFilename)) {
            return; // Interrompe l'elaborazione PRIMA di iniziare
        }

        logArea.setText("");
        progressBar.setValue(0);
        progressBar.setIndeterminate(false);

        SwingWorker<Boolean, String> worker = new SwingWorker<>() {

            private Path lastOutput;

            @Override
            protected Boolean doInBackground() {
                publish("Inizio elaborazione...");
                setProgress(0);

                try {
                    Path pdf = Path.of(pdfPath);
                    Path out = Path.of(outPath);

                    // collega il listener
                    processor.setProgressListener(percent -> setProgress(percent));

                    boolean ok = processor.process(pdf, out);

                    String filename = processor.getLastGeneratedFilename();
                    lastOutput = out.resolve(filename);

                    return ok && Files.exists(lastOutput);

                } catch (Exception ex) {
                    publish("ERRORE: " + ex.getMessage());
                    return false;
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String msg : chunks) {
                    log(msg, Color.BLACK);
                }
            }

            @Override
            protected void done() {
                try {
                    boolean ok = get();

                    progressBar.setValue(100);

                    if (ok) {
                        log("File generato: " + lastOutput, Color.GREEN.darker());
                        openFileBtn.setEnabled(true);
                        lastOutputFile = lastOutput;
                    } else {
                        log("ATTENZIONE: il file non è stato generato.", Color.RED.darker());
                        openFileBtn.setEnabled(false);
                    }

                    log("Elaborazione completata.", Color.BLACK);

                } catch (Exception ex) {
                    log("ERRORE finale: " + ex.getMessage(), Color.RED.darker());
                }
            }
        };

        // collega la progress bar al worker
        worker.addPropertyChangeListener(evt -> {
            if ("progress".equals(evt.getPropertyName())) {
                int value = (int) evt.getNewValue();
                progressBar.setValue(value);
            }
        });

        worker.execute();
    }

    private boolean validateOutputDirectory(String outPath) {

        Path outDir = Paths.get(outPath);

        if (outDir == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Percorso di output non valido.",
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        if (!Files.exists(outDir)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La cartella di destinazione non esiste:\n" + outDir,
                    "Errore",
                    JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Controllo definitivo: cartella protetta da Windows
        if (isProtectedWindowsFolder(outDir)) {
            JOptionPane.showMessageDialog(
                    this,
                    "La cartella selezionata non consente la scrittura:\n" + outDir +
                            "\n\nWindows potrebbe aver applicato restrizioni di sicurezza a questa posizione.\n" +
                            "Le cartelle del profilo utente spesso impediscono la scrittura da parte di applicazioni non autorizzate, ad esempio:\n"
                            +
                            " - Downloads\n" +
                            " - Documents\n" +
                            " - Desktop\n" +
                            " - Pictures\n" +
                            " - Videos\n" +
                            " - Cartelle sincronizzate (OneDrive, Google Drive, Dropbox)\n" +
                            "\nPer evitare errori, scegli una cartella diversa, ad esempio:\n" +
                            " - C:\\LUL\\\n" +
                            " - D:\\Rendicontazioni\\\n" +
                            " - Una cartella non sotto 'Utenti'",
                    "Permesso negato",
                    JOptionPane.WARNING_MESSAGE);

            return false;
        }

        return true;
    }

    private boolean isProtectedWindowsFolder(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) {
            return false;
        }

        try {
            // 1. Controllo permessi base
            if (!Files.isWritable(dir)) {
                return true; // cartella non scrivibile
            }

            File folder = dir.toFile();

            // 2. Controllo permessi OS
            if (!folder.canWrite()) {
                return true;
            }

            // 3. Test creazione file SENZA virtualizzazione
            File testFile = new File(folder, ".lul_write_test.tmp");

            boolean created = testFile.createNewFile();
            if (created) {
                testFile.delete();
                return false; // scrittura OK → cartella sicura
            } else {
                return true; // impossibile creare → cartella protetta
            }

        } catch (Exception e) {
            // Qualsiasi eccezione → Windows sta bloccando la scrittura
            return true;
        }
    }

    private boolean checkIfFileIsLocked(Path outputPath, String filename) {
        try (FileOutputStream fos = new FileOutputStream(outputPath.toFile(), true)) {
            // Se arrivo qui, il file NON è bloccato
            return false;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(
                    null,
                    "Il file \"" + filename + "\" risulta aperto in Excel.\n" +
                            "Chiudilo prima di procedere con l'elaborazione.",
                    "File bloccato",
                    JOptionPane.WARNING_MESSAGE);
            return true;
        }
    }

    private void updateOutputFieldColor() {
        String outPath = outputField.getText().trim();

        Path outDir = null;
        try {
            outDir = Paths.get(outPath);
        } catch (Exception ignored) {
        }

        boolean dangerous = outDir != null && isProtectedWindowsFolder(outDir);

        if (dangerous) {
            outputField.setBackground(new Color(255, 200, 200)); // rosso chiaro
            outputField.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            outputField.setToolTipText("Cartella protetta da Windows. Scegli una cartella diversa.");
        } else {
            outputField.setBackground(Color.WHITE);
            outputField.setBorder(UIManager.getBorder("TextField.border"));
            outputField.setToolTipText(null);
        }
    }

    private void openLastOutputFile() {
        if (lastOutputFile != null && java.nio.file.Files.exists(lastOutputFile)) {
            try {
                Desktop.getDesktop().open(lastOutputFile.toFile());
            } catch (Exception ex) {
                log("ERRORE nell'apertura del file: " + ex.getMessage(), Color.RED.darker());
            }
        } else {
            log("Il file non esiste più.", Color.RED.darker());
            openFileBtn.setEnabled(false);
        }
    }

    private void appendColoredText(JTextPane pane, String text, Color color) {
        StyledDocument doc = pane.getStyledDocument();
        Style style = pane.addStyle("ColorStyle", null);
        StyleConstants.setForeground(style, color);

        try {
            doc.insertString(doc.getLength(), text, style);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void log(String msg, Color color) {
        SwingUtilities.invokeLater(() -> appendColoredText(logArea, msg + "\n", color));
    }

    private class TextPaneOutputStream extends OutputStream {
        private final StringBuilder buffer = new StringBuilder();

        private static final String ESC = "\u001B";
        private static final Pattern ANSI_PATTERN = Pattern.compile(ESC + "\\[[;\\d]*m");
        private static final Pattern SPRING_LOG_PATTERN = Pattern.compile(
                "^(\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}[+-]\\d{2}:\\d{2})\\s+INFO\\s+\\d+\\s+---\\s+\\[.*?]\\s+\\[.*?]\\s+.*?\\s{2}:\\s(.*)$");

        public TextPaneOutputStream() {
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

            // Rimuove CRLF
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }

            // Rimuove ANSI
            line = ANSI_PATTERN.matcher(line).replaceAll("");

            // Riconoscimento Spring Log
            Matcher m = SPRING_LOG_PATTERN.matcher(line);
            if (m.matches()) {
                line = m.group(1) + " - " + m.group(2);
            }

            Color color = Color.BLACK;
            if (line.contains("ERROR") || line.contains("Exception")) {
                color = Color.RED;
            } else if (line.contains("WARN")) {
                color = new Color(200, 120, 0);
            } else if (line.contains("INFO")) {
                color = new Color(0, 100, 180);
            }

            log(line, color);
        }
    }

}