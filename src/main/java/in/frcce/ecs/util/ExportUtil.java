package in.frcce.ecs.util;

import javax.swing.*;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * Utility for exporting JTable data to CSV and printing.
 */
public final class ExportUtil {

    private ExportUtil() {}

    /**
     * Export a JTable's contents to a CSV file selected by the user.
     */
    public static void exportTableToCSV(JTable table, Component parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Timetable as CSV");
        chooser.setSelectedFile(new File("timetable_export.csv"));

        int result = chooser.showSaveDialog(parent);
        if (result != JFileChooser.APPROVE_OPTION) return;

        File file = chooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".csv")) {
            file = new File(file.getAbsolutePath() + ".csv");
        }

        TableModel model = table.getModel();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            // Write column headers
            for (int col = 0; col < model.getColumnCount(); col++) {
                bw.write(escapeCSV(model.getColumnName(col)));
                if (col < model.getColumnCount() - 1) bw.write(",");
            }
            bw.newLine();

            // Write rows
            for (int row = 0; row < model.getRowCount(); row++) {
                for (int col = 0; col < model.getColumnCount(); col++) {
                    Object val = model.getValueAt(row, col);
                    bw.write(escapeCSV(val != null ? val.toString() : ""));
                    if (col < model.getColumnCount() - 1) bw.write(",");
                }
                bw.newLine();
            }

            JOptionPane.showMessageDialog(parent,
                    "Successfully exported to:\n" + file.getAbsolutePath(),
                    "Export Successful", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent,
                    "Failed to export CSV: " + e.getMessage(),
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Print a JTable using Swing print API (shows printer dialog).
     */
    public static void printTable(JTable table, String title, Component parent) {
        try {
            MessageFormat header = new MessageFormat(title);
            MessageFormat footer = new MessageFormat("- Page {0} -");
            boolean complete = table.print(JTable.PrintMode.FIT_WIDTH, header, footer,
                    true, null, true, null);
            if (!complete) {
                JOptionPane.showMessageDialog(parent, "Printing was cancelled.",
                        "Print Cancelled", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException e) {
            JOptionPane.showMessageDialog(parent,
                    "Printer error: " + e.getMessage(),
                    "Print Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

