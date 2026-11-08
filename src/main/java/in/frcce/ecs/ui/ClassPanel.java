package in.frcce.ecs.ui;

import in.frcce.ecs.dao.BatchDAO;
import in.frcce.ecs.dao.ClassDivisionDAO;
import in.frcce.ecs.model.Batch;
import in.frcce.ecs.model.ClassDivision;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Master data CRUD for Classes and their Batches.
 */
public class ClassPanel extends JPanel implements MainFrame.Refreshable {

    private final ClassDivisionDAO classDAO = new ClassDivisionDAO();
    private final BatchDAO         batchDAO = new BatchDAO();

    private JTable            classTable;
    private DefaultTableModel classTableModel;

    private JTable            batchTable;
    private DefaultTableModel batchTableModel;
    private JLabel            batchHeaderLabel;

    private int selectedClassId = -1;

    private static final String[] CLASS_COLS = {
        "ID", "Year", "Semester", "Division", "Strength", "Academic Year"
    };

    private static final String[] BATCH_COLS = {
        "ID", "Batch Name", "Strength"
    };

    public ClassPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                buildClassSection(), buildBatchSection());
        splitPane.setResizeWeight(0.6);
        add(splitPane, BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildClassSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Classes / Divisions"));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Class");
        addBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showClassDialog(null));

        JButton editBtn = new JButton("✎ Edit");
        editBtn.setBackground(new Color(0xFB, 0xBC, 0x04));
        editBtn.addActionListener(e -> onEditClass());

        JButton delBtn = new JButton("🗑 Delete");
        delBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        delBtn.setForeground(Color.WHITE);
        delBtn.addActionListener(e -> onDeleteClass());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(delBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        classTableModel = new DefaultTableModel(CLASS_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        classTable = new JTable(classTableModel);
        classTable.setRowHeight(24);
        classTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        classTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        classTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        classTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = classTable.getSelectedRow();
                if (row >= 0) {
                    selectedClassId = (int) classTableModel.getValueAt(row, 0);
                    String label = classTableModel.getValueAt(row, 1) + " Sem "
                            + classTableModel.getValueAt(row, 2) + " Div "
                            + classTableModel.getValueAt(row, 3);
                    batchHeaderLabel.setText("Batches for: " + label);
                    loadBatches(selectedClassId);
                } else {
                    selectedClassId = -1;
                    batchHeaderLabel.setText("Select a class above to view/manage batches");
                    batchTableModel.setRowCount(0);
                }
            }
        });

        panel.add(new JScrollPane(classTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBatchSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Batches"));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        batchHeaderLabel = new JLabel("Select a class above to view/manage batches");
        batchHeaderLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JButton addBatchBtn = new JButton("+ Add Batch");
        addBatchBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        addBatchBtn.setForeground(Color.WHITE);
        addBatchBtn.addActionListener(e -> onAddBatch());

        JButton delBatchBtn = new JButton("🗑 Delete Batch");
        delBatchBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        delBatchBtn.setForeground(Color.WHITE);
        delBatchBtn.addActionListener(e -> onDeleteBatch());

        toolbar.add(batchHeaderLabel);
        toolbar.add(Box.createHorizontalStrut(20));
        toolbar.add(addBatchBtn);
        toolbar.add(delBatchBtn);
        panel.add(toolbar, BorderLayout.NORTH);

        batchTableModel = new DefaultTableModel(BATCH_COLS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        batchTable = new JTable(batchTableModel);
        batchTable.setRowHeight(22);
        batchTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        batchTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        batchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(batchTable), BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void refresh() {
        classTableModel.setRowCount(0);
        List<ClassDivision> list = classDAO.findAll();
        for (ClassDivision cd : list) {
            String yearStr = switch (cd.getYear()) {
                case 1 -> "FE";
                case 2 -> "SE";
                case 3 -> "TE";
                case 4 -> "BE";
                default -> "Year " + cd.getYear();
            };
            classTableModel.addRow(new Object[]{
                cd.getClassId(), yearStr, cd.getSemester(), cd.getDivision(),
                cd.getStrength(), cd.getAcademicYear()
            });
        }
        if (selectedClassId > 0) {
            loadBatches(selectedClassId);
        }
    }

    private void loadBatches(int classId) {
        batchTableModel.setRowCount(0);
        List<Batch> batches = batchDAO.findByClass(classId);
        for (Batch b : batches) {
            batchTableModel.addRow(new Object[]{
                b.getBatchId(), b.getBatchName(), b.getStrength()
            });
        }
    }

    private void onEditClass() {
        int row = classTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) classTableModel.getValueAt(row, 0);
        classDAO.findById(id).ifPresent(this::showClassDialog);
    }

    private void onDeleteClass() {
        int row = classTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) classTableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this,
                "Delete this class division and all its batches?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                classDAO.delete(id);
                selectedClassId = -1;
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showClassDialog(ClassDivision existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Class" : "Edit Class", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JComboBox<String> yearCombo = new JComboBox<>(new String[]{"1 (FE)", "2 (SE)", "3 (TE)", "4 (BE)"});
        if (existing != null) {
            yearCombo.setSelectedIndex(Math.max(0, Math.min(3, existing.getYear() - 1)));
        } else {
            yearCombo.setSelectedIndex(1); // Default SE
        }

        JSpinner semSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getSemester() : 3, 1, 8, 1));
        JTextField divField = new JTextField(existing != null ? existing.getDivision() : "A", 10);
        JSpinner strengthSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getStrength() : 60, 1, 200, 1));
        JTextField ayField = new JTextField(existing != null ? existing.getAcademicYear() : "2026-27", 10);

        g.gridx=0; g.gridy=0; p.add(new JLabel("Engineering Year:"), g);
        g.gridx=1; g.gridy=0; p.add(yearCombo, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("Semester:"), g);
        g.gridx=1; g.gridy=1; p.add(semSpin, g);
        g.gridx=0; g.gridy=2; p.add(new JLabel("Division:"), g);
        g.gridx=1; g.gridy=2; p.add(divField, g);
        g.gridx=0; g.gridy=3; p.add(new JLabel("Student Strength:"), g);
        g.gridx=1; g.gridy=3; p.add(strengthSpin, g);
        g.gridx=0; g.gridy=4; p.add(new JLabel("Academic Year:"), g);
        g.gridx=1; g.gridy=4; p.add(ayField, g);

        JButton save = new JButton("Save");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> {
            String div = divField.getText().trim().toUpperCase();
            String ay = ayField.getText().trim();
            if (div.isEmpty() || ay.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Division and Academic Year are required.");
                return;
            }
            int year = yearCombo.getSelectedIndex() + 1;
            int sem = (Integer) semSpin.getValue();
            int strength = (Integer) strengthSpin.getValue();

            ClassDivision cd = existing != null ? existing : new ClassDivision();
            cd.setYear(year);
            cd.setSemester(sem);
            cd.setDivision(div);
            cd.setStrength(strength);
            cd.setAcademicYear(ay);
            cd.setDeptId(1); // ECS department

            if (existing == null) {
                int newId = classDAO.insert(cd);
                // Automatically create default batches A, B, C, D
                if (newId > 0) {
                    int bStrength = Math.max(1, strength / 4);
                    batchDAO.insert(new Batch(0, newId, "A", bStrength));
                    batchDAO.insert(new Batch(0, newId, "B", bStrength));
                    batchDAO.insert(new Batch(0, newId, "C", bStrength));
                    batchDAO.insert(new Batch(0, newId, "D", strength - (3 * bStrength)));
                }
            } else {
                classDAO.update(cd);
            }
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

    private void onAddBatch() {
        if (selectedClassId <= 0) {
            JOptionPane.showMessageDialog(this, "Select a class division first.");
            return;
        }
        JTextField nameField = new JTextField(10);
        JSpinner strengthSpin = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1));

        JPanel panel = new JPanel(new GridLayout(2, 2, 4, 4));
        panel.add(new JLabel("Batch Name (e.g. A, B, B1):"));
        panel.add(nameField);
        panel.add(new JLabel("Batch Strength:"));
        panel.add(strengthSpin);

        int res = JOptionPane.showConfirmDialog(this, panel, "Add Batch",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim().toUpperCase();
            if (name.isEmpty()) return;
            Batch b = new Batch(0, selectedClassId, name, (Integer) strengthSpin.getValue());
            batchDAO.insert(b);
            loadBatches(selectedClassId);
        }
    }

    private void onDeleteBatch() {
        int row = batchTable.getSelectedRow();
        if (row < 0) return;
        int id = (int) batchTableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Delete this batch?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            batchDAO.delete(id);
            loadBatches(selectedClassId);
        }
    }
}

