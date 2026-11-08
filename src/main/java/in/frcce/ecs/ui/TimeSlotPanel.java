package in.frcce.ecs.ui;

import in.frcce.ecs.dao.TimeSlotDAO;
import in.frcce.ecs.model.TimeSlot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Master data CRUD for Time Slots.
 */
public class TimeSlotPanel extends JPanel implements MainFrame.Refreshable {

    private final TimeSlotDAO timeSlotDAO = new TimeSlotDAO();
    private JTable            table;
    private DefaultTableModel tableModel;
    private JComboBox<String> dayFilterCombo;

    private static final String[] COLS = {
        "ID", "Day", "Slot #", "Start Time", "End Time", "Is Break"
    };

    public TimeSlotPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Slot");
        addBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showDialog(null));

        JButton editBtn = new JButton("✎ Edit");
        editBtn.setBackground(new Color(0xFB, 0xBC, 0x04));
        editBtn.addActionListener(e -> onEdit());

        JButton delBtn = new JButton("🗑 Delete");
        delBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        delBtn.setForeground(Color.WHITE);
        delBtn.addActionListener(e -> onDelete());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(delBtn);
        toolbar.add(Box.createHorizontalStrut(15));

        toolbar.add(new JLabel("Filter by Day:"));
        dayFilterCombo = new JComboBox<>(new String[]{
            "ALL", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        });
        dayFilterCombo.addActionListener(e -> refresh());
        toolbar.add(dayFilterCombo);

        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        table.setRowHeight(24);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        String selectedDay = (String) dayFilterCombo.getSelectedItem();
        List<TimeSlot> list = (selectedDay == null || "ALL".equals(selectedDay))
                ? timeSlotDAO.findAllIncBreaks()
                : timeSlotDAO.findByDay(selectedDay);

        for (TimeSlot ts : list) {
            tableModel.addRow(new Object[]{
                ts.getSlotId(), ts.getDay(), ts.getSlotNo(),
                ts.getStartTime(), ts.getEndTime(),
                ts.isBreak() ? "Yes (Break)" : "No"
            });
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        String day = (String) tableModel.getValueAt(row, 1);
        int slotNo = (int) tableModel.getValueAt(row, 2);
        String start = (String) tableModel.getValueAt(row, 3);
        String end = (String) tableModel.getValueAt(row, 4);
        boolean isBreak = ((String) tableModel.getValueAt(row, 5)).contains("Yes");

        TimeSlot ts = new TimeSlot(id, day, start, end, slotNo, isBreak);
        showDialog(ts);
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Delete this time slot?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                timeSlotDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete slot: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDialog(TimeSlot existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Slot" : "Edit Slot", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JComboBox<String> dayCombo = new JComboBox<>(new String[]{
            "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"
        });
        if (existing != null) dayCombo.setSelectedItem(existing.getDay());
        JSpinner slotNoSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getSlotNo() : 1, 1, 15, 1));
        JTextField startField = new JTextField(existing != null ? existing.getStartTime() : "08:45:00", 10);
        JTextField endField = new JTextField(existing != null ? existing.getEndTime() : "09:45:00", 10);
        JCheckBox breakCheck = new JCheckBox("Is Break (Lunch/Short break)", existing != null && existing.isBreak());

        g.gridx=0; g.gridy=0; p.add(new JLabel("Day:"), g);
        g.gridx=1; g.gridy=0; p.add(dayCombo, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("Slot Number:"), g);
        g.gridx=1; g.gridy=1; p.add(slotNoSpin, g);
        g.gridx=0; g.gridy=2; p.add(new JLabel("Start Time (HH:MM:SS):"), g);
        g.gridx=1; g.gridy=2; p.add(startField, g);
        g.gridx=0; g.gridy=3; p.add(new JLabel("End Time (HH:MM:SS):"), g);
        g.gridx=1; g.gridy=3; p.add(endField, g);
        g.gridx=0; g.gridy=4; p.add(new JLabel("Break Slot:"), g);
        g.gridx=1; g.gridy=4; p.add(breakCheck, g);

        JButton save = new JButton("Save");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> {
            String start = startField.getText().trim();
            String end = endField.getText().trim();
            if (start.isEmpty() || end.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Start and end times are required.");
                return;
            }
            if (start.length() == 5) start = start + ":00";
            if (end.length() == 5) end = end + ":00";

            TimeSlot ts = existing != null ? existing : new TimeSlot();
            ts.setDay((String) dayCombo.getSelectedItem());
            ts.setSlotNo((Integer) slotNoSpin.getValue());
            ts.setStartTime(start);
            ts.setEndTime(end);
            ts.setBreak(breakCheck.isSelected());

            if (existing == null) timeSlotDAO.insert(ts);
            else timeSlotDAO.update(ts);
            dlg.dispose();
            refresh();
        });

        g.gridx=0; g.gridy=5; g.gridwidth=2;
        g.insets=new Insets(12, 4, 4, 4);
        p.add(save, g);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}

