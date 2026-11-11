package in.frcce.ecs.ui;

import in.frcce.ecs.dao.FacultyDAO;
import in.frcce.ecs.dao.FacultyUnavailabilityDAO;
import in.frcce.ecs.dao.TimeSlotDAO;
import in.frcce.ecs.model.Faculty;
import in.frcce.ecs.model.FacultyUnavailability;
import in.frcce.ecs.model.TimeSlot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Management of faculty blocked/unavailable slots.
 */
public class UnavailabilityPanel extends JPanel implements MainFrame.Refreshable {

    private final FacultyUnavailabilityDAO unavailDAO = new FacultyUnavailabilityDAO();
    private final FacultyDAO              facDAO     = new FacultyDAO();
    private final TimeSlotDAO             slotDAO    = new TimeSlotDAO();

    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Faculty", "Day", "Slot #", "Time", "Reason"
    };

    public UnavailabilityPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        UiTheme.styleToolbar(toolbar);

        JButton addBtn = new JButton("+ Block Slot");
        UiTheme.styleActionButton(addBtn, new Color(0x34, 0xA8, 0x53), Color.WHITE);
        addBtn.addActionListener(e -> showAddDialog());

        JButton delBtn = new JButton("🗑 Remove Block");
        UiTheme.styleActionButton(delBtn, new Color(0xEA, 0x43, 0x35), Color.WHITE);
        delBtn.addActionListener(e -> onDelete());

        toolbar.add(addBtn);
        toolbar.add(delBtn);
        add(toolbar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        UiTheme.applyTableStyle(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        add(new JScrollPane(table), BorderLayout.CENTER);
        refresh();
    }

    @Override
    public void refresh() {
        tableModel.setRowCount(0);
        Map<Integer, Faculty> facultyMap = new HashMap<>();
        for (Faculty f : facDAO.findAll()) facultyMap.put(f.getFacultyId(), f);

        Map<Integer, TimeSlot> slotMap = new HashMap<>();
        for (TimeSlot ts : slotDAO.findAllIncBreaks()) slotMap.put(ts.getSlotId(), ts);

        List<FacultyUnavailability> list = unavailDAO.findAll();
        for (FacultyUnavailability fu : list) {
            Faculty f = facultyMap.get(fu.getFacultyId());
            TimeSlot ts = slotMap.get(fu.getSlotId());

            String facName = f != null ? f.getName() + " (" + f.getAbbreviation() + ")" : "Faculty #" + fu.getFacultyId();
            String day = ts != null ? ts.getDay() : "-";
            int slotNo = ts != null ? ts.getSlotNo() : 0;
            String time = ts != null ? ts.getDisplayTime() : "-";

            tableModel.addRow(new Object[]{
                fu.getId(), facName, day, slotNo, time,
                fu.getReason() != null ? fu.getReason() : ""
            });
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Remove this slot block?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                unavailDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot remove: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddDialog() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Block Slot for Faculty", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 4, 3, 4);

        JComboBox<FacultyItem> facCombo = new JComboBox<>();
        facCombo.setFont(UiTheme.FONT_BASE);
        for (Faculty f : facDAO.findAll()) facCombo.addItem(new FacultyItem(f));

        JComboBox<SlotItem> slotCombo = new JComboBox<>();
        slotCombo.setFont(UiTheme.FONT_BASE);
        for (TimeSlot ts : slotDAO.findAll()) slotCombo.addItem(new SlotItem(ts));

        JTextField reasonField = new JTextField("Meeting / Personal", 20);
        reasonField.setFont(UiTheme.FONT_BASE);

        JLabel l0 = new JLabel("Faculty:"); l0.setFont(UiTheme.bold(11f));
        JLabel l1 = new JLabel("Time Slot:"); l1.setFont(UiTheme.bold(11f));
        JLabel l2 = new JLabel("Reason:"); l2.setFont(UiTheme.bold(11f));

        g.gridx=0; g.gridy=0; p.add(l0, g);
        g.gridx=1; g.gridy=0; p.add(facCombo, g);
        g.gridx=0; g.gridy=1; p.add(l1, g);
        g.gridx=1; g.gridy=1; p.add(slotCombo, g);
        g.gridx=0; g.gridy=2; p.add(l2, g);
        g.gridx=1; g.gridy=2; p.add(reasonField, g);

        JButton save = new JButton("Save Block");
        UiTheme.styleActionButton(save, UiTheme.COLOR_HEADER, Color.WHITE);
        save.addActionListener(e -> {
            FacultyItem fi = (FacultyItem) facCombo.getSelectedItem();
            SlotItem si = (SlotItem) slotCombo.getSelectedItem();
            if (fi == null || si == null) {
                JOptionPane.showMessageDialog(dlg, "Please select Faculty and Slot.");
                return;
            }
            FacultyUnavailability fu = new FacultyUnavailability();
            fu.setFacultyId(fi.f.getFacultyId());
            fu.setSlotId(si.ts.getSlotId());
            fu.setReason(reasonField.getText().trim());

            try {
                unavailDAO.insert(fu);
                dlg.dispose();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Failed to block slot: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        g.gridx=0; g.gridy=3; g.gridwidth=2;
        g.insets=new Insets(12, 4, 4, 4);
        p.add(save, g);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private static class FacultyItem {
        final Faculty f;
        FacultyItem(Faculty f) { this.f = f; }
        @Override public String toString() { return f.getName() + " (" + f.getAbbreviation() + ")"; }
    }

    private static class SlotItem {
        final TimeSlot ts;
        SlotItem(TimeSlot ts) { this.ts = ts; }
        @Override public String toString() { return ts.getDay() + " S" + ts.getSlotNo() + " (" + ts.getDisplayTime() + ")"; }
    }
}

