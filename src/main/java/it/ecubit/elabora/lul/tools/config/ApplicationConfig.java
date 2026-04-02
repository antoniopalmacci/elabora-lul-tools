package it.ecubit.elabora.lul.tools.config;

import javax.swing.SwingUtilities;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import it.ecubit.elabora.lul.tools.model.LulGui;
import it.ecubit.elabora.lul.tools.zucchetti.PdfToExcelProcessor;

@SpringBootApplication
@ComponentScan("it.ecubit.elabora.lul.tools")
public class ApplicationConfig {

    public static void main(String[] args) {

        System.setProperty("java.awt.headless", "false");

        ConfigurableApplicationContext ctx =
                SpringApplication.run(ApplicationConfig.class, args);

        SwingUtilities.invokeLater(() -> {
            PdfToExcelProcessor processor = ctx.getBean(PdfToExcelProcessor.class);
            LulGui gui = new LulGui(processor);
            gui.setVisible(true);
        });
    }
}
