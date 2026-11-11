package in.frcce.ecs.ui;

import in.frcce.ecs.dao.SubjectDAO;
import in.frcce.ecs.model.Subject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Master data CRUD for Subject.
 */
public class SubjectPanel extends JPanel implements MainFrame.Refreshable {

    private final SubjectDAO subjectDAO = new SubjectDAO();
    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Course Code", "Name", "Abbr", "Sem", "Type", "TH Hrs", "PR Hrs", "PR Mode", "TU Hrs", "Total", "Active"
    };

    public SubjectPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        UiTheme.styleToolbar(toolbar);

        JButton addBtn = new JButton("+ Add Subject");
        UiTheme.styleActionButton(addBtn, new Color(0x34, 0xA8, 0x53), Color.WHITE);
        addBtn.addActionListener(e -> showDialog(null));

        JButton editBtn = new JButton("✎ Edit");
        UiTheme.styleActionButton(editBtn, new Color(0xFB, 0xBC, 0x04), Color.BLACK);
        editBtn.addActionListener(e -> onEdit());

        JButton delBtn = new JButton("🗑 Delete");
        UiTheme.styleActionButton(delBtn, new Color(0xEA, 0x43, 0x35), Color.WHITE);
        delBtn.addActionListener(e -> onDelete());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
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
        List<Subject> list = subjectDAO.findAll();
        for (Subject s : list) {
            String prModeStr = s.getPracticalHrs() > 0
                    ? (s.isWholeClassPractical() ? "Whole Class" : "In Batches")
                    : "-";
            tableModel.addRow(new Object[]{
                s.getSubjectId(), s.getCourseCode(), s.getName(), s.getAbbreviation(),
                s.getSemester(), s.getType(), s.getTheoryHrs(), s.getPracticalHrs(), prModeStr,
                s.getTutorialHrs(), s.getTotalHours(), s.isActive() ? "Yes" : "No"
            });
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        subjectDAO.findById(id).ifPresent(this::showDialog);
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Delete this subject?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                subjectDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDialog(Subject existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Subject" : "Edit Subject", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 4, 3, 4);

        JTextField codeField = new JTextField(existing != null ? existing.getCourseCode() : "", 15);
        codeField.setFont(UiTheme.FONT_BASE);
        JTextField nameField = new JTextField(existing != null ? existing.getName() : "", 22);
        nameField.setFont(UiTheme.FONT_BASE);
        JTextField abbrField = new JTextField(existing != null ? existing.getAbbreviation() : "", 8);
        abbrField.setFont(UiTheme.FONT_BASE);
        JSpinner   semSpin   = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getSemester() : 3, 1, 8, 1));
        semSpin.setFont(UiTheme.FONT_BASE);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "PCC", "OE", "MDM", "HSSM", "EL", "BSC", "VAC", "CEP"
        });
        typeCombo.setFont(UiTheme.FONT_BASE);
        if (existing != null) typeCombo.setSelectedItem(existing.getType());
        JSpinner thSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getTheoryHrs() : 2, 0, 10, 1));
        thSpin.setFont(UiTheme.FONT_BASE);
        JSpinner prSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getPracticalHrs() : 2, 0, 10, 1));
        prSpin.setFont(UiTheme.FONT_BASE);
        JComboBox<String> pracModeCombo = new JComboBox<>(new String[]{"In Batches", "Whole Class"});
        pracModeCombo.setFont(UiTheme.FONT_BASE);
        if (existing != null && existing.isWholeClassPractical()) {
            pracModeCombo.setSelectedItem("Whole Class");
        }
        pracModeCombo.setEnabled(((Integer) prSpin.getValue()) > 0);
        prSpin.addChangeListener(e -> pracModeCombo.setEnabled(((Integer) prSpin.getValue()) > 0));
        JSpinner tuSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getTutorialHrs() : 0, 0, 10, 1));
        tuSpin.setFont(UiTheme.FONT_BASE);

        JLabel l0 = new JLabel("Course Code:"); l0.setFont(UiTheme.bold(11f));
        JLabel l1 = new JLabel("Subject Name:"); l1.setFont(UiTheme.bold(11f));
        JLabel l2 = new JLabel("Abbreviation:"); l2.setFont(UiTheme.bold(11f));
        JLabel l3 = new JLabel("Semester:"); l3.setFont(UiTheme.bold(11f));
        JLabel l4 = new JLabel("Type:"); l4.setFont(UiTheme.bold(11f));
        JLabel l5 = new JLabel("Theory Hrs/Wk:"); l5.setFont(UiTheme.bold(11f));
        JLabel l6 = new JLabel("Practical Hrs/Wk:"); l6.setFont(UiTheme.bold(11f));
        JLabel l7 = new JLabel("Practical Mode:"); l7.setFont(UiTheme.bold(11f));
        JLabel l8 = new JLabel("Tutorial Hrs/Wk:"); l8.setFont(UiTheme.bold(11f));

        g.gridx=0; g.gridy=0; p.add(l0, g);
        g.gridx=1; g.gridy=0; p.add(codeField, g);
        g.gridx=0; g.gridy=1; p.add(l1, g);
        g.gridx=1; g.gridy=1; p.add(nameField, g);
        g.gridx=0; g.gridy=2; p.add(l2, g);
        g.gridx=1; g.gridy=2; p.add(abbrField, g);
        g.gridx=0; g.gridy=3; p.add(l3, g);
        g.gridx=1; g.gridy=3; p.add(semSpin, g);
        g.gridx=0; g.gridy=4; p.add(l4, g);
        g.gridx=1; g.gridy=4; p.add(typeCombo, g);
        g.gridx=0; g.gridy=5; p.add(l5, g);
        g.gridx=1; g.gridy=5; p.add(thSpin, g);
        g.gridx=0; g.gridy=6; p.add(l6, g);
        g.gridx=1; g.gridy=6; p.add(prSpin, g);
        g.gridx=0; g.gridy=7; p.add(l7, g);
        g.gridx=1; g.gridy=7; p.add(pracModeCombo, g);
        g.gridx=0; g.gridy=8; p.add(l8, g);
        g.gridx=1; g.gridy=8; p.add(tuSpin, g);

        JButton save = new JButton("Save");
        UiTheme.styleActionButton(save, UiTheme.COLOR_HEADER, Color.WHITE);
        save.addActionListener(e -> {
            String code = codeField.getText().trim();
            String name = nameField.getText().trim();
            String abbr = abbrField.getText().trim().toUpperCase();
            if (code.isEmpty() || name.isEmpty() || abbr.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "All fields are required.");
                return;
            }
            Subject s = existing != null ? existing : new Subject();
            s.setCourseCode(code);
            s.setName(name);
            s.setAbbreviation(abbr);
            s.setSemester((Integer) semSpin.getValue());
            s.setType((String) typeCombo.getSelectedItem());
            s.setTheoryHrs((Integer) thSpin.getValue());
            s.setPracticalHrs((Integer) prSpin.getValue());
            s.setPracMode("Whole Class".equals(pracModeCombo.getSelectedItem()) ? "WHOLE_CLASS" : "BATCH");
            s.setTutorialHrs((Integer) tuSpin.getValue());
            s.setActive(true);

            if (existing == null) subjectDAO.insert(s);
            else subjectDAO.update(s);
            dlg.dispose();
            refresh();
        });

        g.gridx=0; g.gridy=9; g.gridwidth=2;
        g.insets=new Insets(12, 4, 4, 4);
        p.add(save, g);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}

