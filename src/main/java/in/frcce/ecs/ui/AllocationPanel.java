package in.frcce.ecs.ui;

import in.frcce.ecs.dao.ClassDivisionDAO;
import in.frcce.ecs.dao.FacultyDAO;
import in.frcce.ecs.dao.FacultySubjectDAO;
import in.frcce.ecs.dao.SubjectDAO;
import in.frcce.ecs.model.ClassDivision;
import in.frcce.ecs.model.Faculty;
import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.model.Subject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Teaching allocation management: maps faculty to subjects for each class division.
 */
public class AllocationPanel extends JPanel implements MainFrame.Refreshable {

    private final FacultySubjectDAO fsDAO    = new FacultySubjectDAO();
    private final FacultyDAO        facDAO   = new FacultyDAO();
    private final SubjectDAO        subDAO   = new SubjectDAO();
    private final ClassDivisionDAO  classDAO = new ClassDivisionDAO();

    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Faculty", "Subject", "Class Division", "Type"
    };

    public AllocationPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Allocation");
        addBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showAddDialog());

        JButton delBtn = new JButton("🗑 Delete");
        delBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        delBtn.setForeground(Color.WHITE);
        delBtn.addActionListener(e -> onDelete());

        toolbar.add(addBtn);
        toolbar.add(delBtn);
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
        List<FacultySubject> list = fsDAO.findAll();
        for (FacultySubject fs : list) {
            tableModel.addRow(new Object[]{
                fs.getId(),
                fs.getFacultyName() != null ? fs.getFacultyName() : ("Faculty #" + fs.getFacultyId()),
                fs.getSubjectName() != null ? fs.getSubjectName() : ("Subject #" + fs.getSubjectId()),
                fs.getClassName() != null ? fs.getClassName() : ("Class #" + fs.getClassId()),
                fs.getSessionType()
            });
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Delete this teaching allocation?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                fsDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddDialog() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Add Faculty-Subject Allocation", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JComboBox<FacultyItem> facCombo = new JComboBox<>();
        for (Faculty f : facDAO.findAll()) facCombo.addItem(new FacultyItem(f));

        JComboBox<SubjectItem> subCombo = new JComboBox<>();
        for (Subject s : subDAO.findAll()) subCombo.addItem(new SubjectItem(s));

        JComboBox<ClassItem> classCombo = new JComboBox<>();
        for (ClassDivision cd : classDAO.findAll()) classCombo.addItem(new ClassItem(cd));

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"ALL", "TH", "PR", "TU"});

        g.gridx=0; g.gridy=0; p.add(new JLabel("Faculty:"), g);
        g.gridx=1; g.gridy=0; p.add(facCombo, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("Subject:"), g);
        g.gridx=1; g.gridy=1; p.add(subCombo, g);
        g.gridx=0; g.gridy=2; p.add(new JLabel("Class Division:"), g);
        g.gridx=1; g.gridy=2; p.add(classCombo, g);
        g.gridx=0; g.gridy=3; p.add(new JLabel("Session Type:"), g);
        g.gridx=1; g.gridy=3; p.add(typeCombo, g);

        JButton save = new JButton("Save Allocation");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> {
            FacultyItem fi = (FacultyItem) facCombo.getSelectedItem();
            SubjectItem si = (SubjectItem) subCombo.getSelectedItem();
            ClassItem ci = (ClassItem) classCombo.getSelectedItem();
            if (fi == null || si == null || ci == null) {
                JOptionPane.showMessageDialog(dlg, "Please select Faculty, Subject, and Class.");
                return;
            }
            FacultySubject fs = new FacultySubject();
            fs.setFacultyId(fi.f.getFacultyId());
            fs.setSubjectId(si.s.getSubjectId());
            fs.setClassId(ci.cd.getClassId());
            fs.setSessionType((String) typeCombo.getSelectedItem());

            try {
                fsDAO.insert(fs);
                dlg.dispose();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Failed to add allocation: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        g.gridx=0; g.gridy=4; g.gridwidth=2;
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

    private static class SubjectItem {
        final Subject s;
        SubjectItem(Subject s) { this.s = s; }
        @Override public String toString() { return s.getName() + " (" + s.getCourseCode() + ") Sem " + s.getSemester(); }
    }

    private static class ClassItem {
        final ClassDivision cd;
        ClassItem(ClassDivision cd) { this.cd = cd; }
        @Override public String toString() { return cd.getLabel(); }
    }
}

