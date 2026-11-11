package in.frcce.ecs.ui;

import in.frcce.ecs.dao.ClassDivisionDAO;
import in.frcce.ecs.dao.FacultyDAO;
import in.frcce.ecs.dao.FacultySubjectDAO;
import in.frcce.ecs.dao.RoomDAO;
import in.frcce.ecs.dao.SubjectDAO;
import in.frcce.ecs.model.ClassDivision;
import in.frcce.ecs.model.Faculty;
import in.frcce.ecs.model.FacultySubject;
import in.frcce.ecs.model.Room;
import in.frcce.ecs.model.Subject;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Teaching allocation management: maps course, faculty member, whether it's
 * practical or theory, and up to 5 preferred choices of classroom/lab.
 */
public class AllocationPanel extends JPanel implements MainFrame.Refreshable {

    private final FacultySubjectDAO fsDAO    = new FacultySubjectDAO();
    private final FacultyDAO        facDAO   = new FacultyDAO();
    private final SubjectDAO        subDAO   = new SubjectDAO();
    private final ClassDivisionDAO  classDAO = new ClassDivisionDAO();
    private final RoomDAO           roomDAO  = new RoomDAO();

    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Faculty", "Course / Subject", "Class", "Type", "Preferred Room Choices (1 to 5)"
    };

    public AllocationPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Course & Faculty Allocation");
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
        Map<Integer, String> roomNames = new HashMap<>();
        for (Room r : roomDAO.findAll()) {
            roomNames.put(r.getRoomId(), r.getName() + " (" + r.getType() + ")");
        }

        List<FacultySubject> list = fsDAO.findAll();
        for (FacultySubject fs : list) {
            List<String> prefs = new ArrayList<>();
            if (fs.getPrefRoom1() != null && roomNames.containsKey(fs.getPrefRoom1())) prefs.add("1: " + roomNames.get(fs.getPrefRoom1()));
            if (fs.getPrefRoom2() != null && roomNames.containsKey(fs.getPrefRoom2())) prefs.add("2: " + roomNames.get(fs.getPrefRoom2()));
            if (fs.getPrefRoom3() != null && roomNames.containsKey(fs.getPrefRoom3())) prefs.add("3: " + roomNames.get(fs.getPrefRoom3()));
            if (fs.getPrefRoom4() != null && roomNames.containsKey(fs.getPrefRoom4())) prefs.add("4: " + roomNames.get(fs.getPrefRoom4()));
            if (fs.getPrefRoom5() != null && roomNames.containsKey(fs.getPrefRoom5())) prefs.add("5: " + roomNames.get(fs.getPrefRoom5()));

            String prefStr = prefs.isEmpty() ? "Any matching room/lab" : String.join(", ", prefs);

            String modeLabel = fs.isWholeClassPractical() ? "Whole Class" : "Batches";
            String typeLabel = "PR".equals(fs.getSessionType()) ? ("Practical (" + modeLabel + ")")
                    : ("TH".equals(fs.getSessionType()) ? "Theory"
                    : ("ALL".equals(fs.getSessionType()) ? "ALL (PR: " + modeLabel + ")" : fs.getSessionType()));

            tableModel.addRow(new Object[]{
                fs.getId(),
                fs.getFacultyName() != null ? fs.getFacultyName() : ("Faculty #" + fs.getFacultyId()),
                fs.getSubjectName() != null ? fs.getSubjectName() : ("Subject #" + fs.getSubjectId()),
                fs.getClassName() != null ? fs.getClassName() : ("Class #" + fs.getClassId()),
                typeLabel,
                prefStr
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

    @SuppressWarnings("unchecked")
    private void showAddDialog() {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Add Course, Faculty & 5 Room/Lab Preferences", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(5, 5, 5, 5);

        JComboBox<FacultyItem> facCombo = new JComboBox<>();
        for (Faculty f : facDAO.findAll()) facCombo.addItem(new FacultyItem(f));

        JComboBox<SubjectItem> subCombo = new JComboBox<>();
        for (Subject s : subDAO.findAll()) subCombo.addItem(new SubjectItem(s));

        JComboBox<ClassItem> classCombo = new JComboBox<>();
        for (ClassDivision cd : classDAO.findAll()) classCombo.addItem(new ClassItem(cd));

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Theory (TH)", "Practical (PR)", "Tutorial (TU)", "ALL"});
        JComboBox<String> pracModeCombo = new JComboBox<>(new String[]{"In Batches (4 Batches)", "Whole Class"});

        // 5 Preferred room combo boxes
        List<Room> allRooms = roomDAO.findAll();
        JComboBox<RoomItem>[] prefCombos = new JComboBox[5];
        for (int i = 0; i < 5; i++) {
            prefCombos[i] = new JComboBox<>();
            prefCombos[i].addItem(new RoomItem(null)); // "None / Any"
            for (Room r : allRooms) {
                prefCombos[i].addItem(new RoomItem(r));
            }
        }

        Runnable updatePracModeEnabled = () -> {
            String sel = (String) typeCombo.getSelectedItem();
            boolean hasPrac = sel != null && (sel.contains("PR") || "ALL".equals(sel));
            pracModeCombo.setEnabled(hasPrac);
        };
        typeCombo.addActionListener(e -> updatePracModeEnabled.run());

        // Auto-select session type & practical mode when subject changes
        Runnable updateTypeBasedOnSubject = () -> {
            SubjectItem item = (SubjectItem) subCombo.getSelectedItem();
            if (item != null) {
                if (item.s.getPracticalHrs() > 0 && item.s.getTheoryHrs() == 0) {
                    typeCombo.setSelectedItem("Practical (PR)");
                } else if (item.s.getTheoryHrs() > 0 && item.s.getPracticalHrs() == 0) {
                    typeCombo.setSelectedItem("Theory (TH)");
                }
                pracModeCombo.setSelectedItem(
                        item.s.isWholeClassPractical() ? "Whole Class" : "In Batches (4 Batches)");
            }
            updatePracModeEnabled.run();
        };
        subCombo.addActionListener(e -> updateTypeBasedOnSubject.run());
        updateTypeBasedOnSubject.run();

        // Layout rows
        int row = 0;
        g.gridx = 0; g.gridy = row; p.add(new JLabel("Course / Subject:"), g);
        g.gridx = 1; g.gridy = row++; p.add(subCombo, g);

        g.gridx = 0; g.gridy = row; p.add(new JLabel("Faculty Member:"), g);
        g.gridx = 1; g.gridy = row++; p.add(facCombo, g);

        g.gridx = 0; g.gridy = row; p.add(new JLabel("Class Division:"), g);
        g.gridx = 1; g.gridy = row++; p.add(classCombo, g);

        g.gridx = 0; g.gridy = row; p.add(new JLabel("Type (Practical / Theory):"), g);
        g.gridx = 1; g.gridy = row++; p.add(typeCombo, g);

        g.gridx = 0; g.gridy = row; p.add(new JLabel("Practical Delivery:"), g);
        g.gridx = 1; g.gridy = row++; p.add(pracModeCombo, g);

        // Separator for room choices
        g.gridx = 0; g.gridy = row++; g.gridwidth = 2;
        JLabel roomHeader = new JLabel("<html><b>Preferred 5 Choices of Classroom / Lab:</b> (Labs: 301, 309, 310, 311, 312 | Classrooms: 302, 303, 306, 307)</html>");
        roomHeader.setBorder(new EmptyBorder(8, 0, 4, 0));
        p.add(roomHeader, g);
        g.gridwidth = 1;

        for (int i = 0; i < 5; i++) {
            g.gridx = 0; g.gridy = row; p.add(new JLabel("Choice " + (i + 1) + ":"), g);
            g.gridx = 1; g.gridy = row++; p.add(prefCombos[i], g);
        }

        JButton save = new JButton("Save Allocation");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.setFont(new Font("Segoe UI", Font.BOLD, 13));
        save.addActionListener(e -> {
            FacultyItem fi = (FacultyItem) facCombo.getSelectedItem();
            SubjectItem si = (SubjectItem) subCombo.getSelectedItem();
            ClassItem ci = (ClassItem) classCombo.getSelectedItem();
            if (fi == null || si == null || ci == null) {
                JOptionPane.showMessageDialog(dlg, "Please select Faculty, Subject, and Class.");
                return;
            }

            String selectedType = (String) typeCombo.getSelectedItem();
            String sessionCode = "ALL";
            if (selectedType != null) {
                if (selectedType.contains("PR")) sessionCode = "PR";
                else if (selectedType.contains("TH")) sessionCode = "TH";
                else if (selectedType.contains("TU")) sessionCode = "TU";
            }

            FacultySubject fs = new FacultySubject();
            fs.setFacultyId(fi.f.getFacultyId());
            fs.setSubjectId(si.s.getSubjectId());
            fs.setClassId(ci.cd.getClassId());
            fs.setSessionType(sessionCode);
            fs.setPracMode("Whole Class".equals(pracModeCombo.getSelectedItem()) ? "WHOLE_CLASS" : "BATCH");

            // Set 5 preferred room choices
            RoomItem r1 = (RoomItem) prefCombos[0].getSelectedItem();
            RoomItem r2 = (RoomItem) prefCombos[1].getSelectedItem();
            RoomItem r3 = (RoomItem) prefCombos[2].getSelectedItem();
            RoomItem r4 = (RoomItem) prefCombos[3].getSelectedItem();
            RoomItem r5 = (RoomItem) prefCombos[4].getSelectedItem();

            fs.setPrefRoom1(r1 != null && r1.r != null ? r1.r.getRoomId() : null);
            fs.setPrefRoom2(r2 != null && r2.r != null ? r2.r.getRoomId() : null);
            fs.setPrefRoom3(r3 != null && r3.r != null ? r3.r.getRoomId() : null);
            fs.setPrefRoom4(r4 != null && r4.r != null ? r4.r.getRoomId() : null);
            fs.setPrefRoom5(r5 != null && r5.r != null ? r5.r.getRoomId() : null);

            try {
                fsDAO.insert(fs);
                dlg.dispose();
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Failed to add allocation: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        g.gridx = 0; g.gridy = row; g.gridwidth = 2;
        g.insets = new Insets(14, 5, 5, 5);
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

    private static class RoomItem {
        final Room r;
        RoomItem(Room r) { this.r = r; }
        @Override public String toString() {
            if (r == null) return "-- None / Any --";
            return "Room " + r.getName() + " (" + r.getType() + ")";
        }
    }
}
