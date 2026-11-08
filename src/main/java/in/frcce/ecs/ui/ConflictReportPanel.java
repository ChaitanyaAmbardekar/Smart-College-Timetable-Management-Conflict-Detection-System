package in.frcce.ecs.ui;

import in.frcce.ecs.model.ConflictReport;
import in.frcce.ecs.util.ExportUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Report panel showing unplaced sessions and conflict diagnostics from the scheduler.
 */
public class ConflictReportPanel extends JPanel implements MainFrame.Refreshable {

    private static List<ConflictReport> lastConflicts = new ArrayList<>();

    private JTable            table;
    private DefaultTableModel tableModel;
    private JLabel            summaryLabel;

    private static final String[] COLS = {
        "Session Type", "Subject", "Faculty", "Class", "Diagnostic Reason", "Status"
    };

    public static synchronized void setLastConflicts(List<ConflictReport> list) {
        lastConflicts = list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public ConflictReportPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton refreshBtn = new JButton("↻ Refresh");
        refreshBtn.addActionListener(e -> refresh());

        JButton exportBtn = new JButton("📄 Export CSV");
        exportBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        exportBtn.setForeground(Color.WHITE);
        exportBtn.addActionListener(e -> ExportUtil.exportTableToCSV(table, this));

        summaryLabel = new JLabel("No generation run yet.");
        summaryLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        toolbar.add(refreshBtn);
        toolbar.add(exportBtn);
        toolbar.add(Box.createHorizontalStrut(15));
        toolbar.add(summaryLabel);

        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        // Color status column
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFoc, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, val, isSel, hasFoc, row, col);
                String text = val != null ? val.toString() : "";
                if (text.contains("Unplaced")) {
                    c.setForeground(new Color(0xD9, 0x30, 0x25)); // red
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setForeground(new Color(0x18, 0x80, 0x38)); // green
                }
                return c;
            }
        });

        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        int unplacedCount = 0;
        for (ConflictReport cr : lastConflicts) {
            boolean unplaced = !cr.isPlaced();
            if (unplaced) unplacedCount++;
            tableModel.addRow(new Object[]{
                cr.getSessionType(),
                cr.getSubjectName() != null ? cr.getSubjectName() : ("Subject #" + cr.getSubjectId()),
                cr.getFacultyName() != null && !cr.getFacultyName().isEmpty() ? cr.getFacultyName() : ("Faculty #" + cr.getFacultyId()),
                cr.getClassName() != null && !cr.getClassName().isEmpty() ? cr.getClassName() : ("Class #" + cr.getClassId()),
                cr.getReason(),
                unplaced ? "⚠ Unplaced" : "✓ Placed"
            });
        }

        if (lastConflicts.isEmpty()) {
            summaryLabel.setText("✓ No conflicts recorded. All sessions placed successfully!");
            summaryLabel.setForeground(new Color(0x18, 0x80, 0x38));
        } else if (unplacedCount > 0) {
            summaryLabel.setText("⚠ " + unplacedCount + " session(s) could not be placed due to resource constraints.");
            summaryLabel.setForeground(new Color(0xD9, 0x30, 0x25));
        } else {
            summaryLabel.setText("✓ All " + lastConflicts.size() + " generated sessions placed with 0 conflicts.");
            summaryLabel.setForeground(new Color(0x18, 0x80, 0x38));
        }
    }
}

