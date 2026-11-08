package in.frcce.ecs.ui;

import in.frcce.ecs.dao.FacultyDAO;
import in.frcce.ecs.model.Faculty;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Master data CRUD for Faculty.
 */
public class FacultyPanel extends JPanel implements MainFrame.Refreshable {

    private final FacultyDAO facultyDAO = new FacultyDAO();
    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Name", "Abbreviation", "Email", "Designation", "Max Hrs/Wk", "Active"
    };

    public FacultyPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        // Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Faculty");
        addBtn.setBackground(new Color(0x34, 0xA8, 0x53));
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showAddDialog(null));

        JButton editBtn = new JButton("✎ Edit");
        editBtn.setBackground(new Color(0xFB, 0xBC, 0x04));
        editBtn.addActionListener(e -> onEdit());

        JButton toggleBtn = new JButton("Toggle Active");
        toggleBtn.addActionListener(e -> onToggleActive());

        JButton delBtn = new JButton("🗑 Delete");
        delBtn.setBackground(new Color(0xEA, 0x43, 0x35));
        delBtn.setForeground(Color.WHITE);
        delBtn.addActionListener(e -> onDelete());

        toolbar.add(addBtn);
        toolbar.add(editBtn);
        toolbar.add(toggleBtn);
        toolbar.add(delBtn);

        add(toolbar, BorderLayout.NORTH);

        // Table
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
        List<Faculty> list = facultyDAO.findAll();
        for (Faculty f : list) {
            tableModel.addRow(new Object[]{
                f.getFacultyId(), f.getName(), f.getAbbreviation(),
                f.getEmail(), f.getDesignation(), f.getMaxHoursPerWeek(),
                f.isActive() ? "Yes" : "No"
            });
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a faculty to edit.");
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        facultyDAO.findById(id).ifPresent(this::showAddDialog);
    }

    private void onToggleActive() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        facultyDAO.toggleActive(id);
        refresh();
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        String name = (String) tableModel.getValueAt(row, 1);
        int ch = JOptionPane.showConfirmDialog(this,
                "Delete faculty: " + name + "?", "Confirm Delete",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                facultyDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete faculty (they have sessions assigned or foreign keys): " + ex.getMessage(),
                        "Delete Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showAddDialog(Faculty existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Faculty" : "Edit Faculty",
                Dialog.ModalityType.APPLICATION_MODAL);

        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JTextField nameField = new JTextField(existing != null ? existing.getName() : "", 20);
        JTextField abbrField = new JTextField(existing != null ? existing.getAbbreviation() : "", 6);
        JTextField emailField = new JTextField(existing != null ? existing.getEmail() : "", 20);
        JComboBox<String> desigCombo = new JComboBox<>(new String[]{
            "Professor", "Associate Professor", "Assistant Professor", "Lab Instructor", "Visiting Faculty"
        });
        if (existing != null) desigCombo.setSelectedItem(existing.getDesignation());
        JSpinner hrsSpin = new JSpinner(new SpinnerNumberModel(
                existing != null ? existing.getMaxHoursPerWeek() : 18, 1, 40, 1));

        g.gridx = 0; g.gridy = 0; p.add(new JLabel("Full Name:"), g);
        g.gridx = 1; g.gridy = 0; p.add(nameField, g);

        g.gridx = 0; g.gridy = 1; p.add(new JLabel("Abbreviation:"), g);
        g.gridx = 1; g.gridy = 1; p.add(abbrField, g);

        g.gridx = 0; g.gridy = 2; p.add(new JLabel("Email:"), g);
        g.gridx = 1; g.gridy = 2; p.add(emailField, g);

        g.gridx = 0; g.gridy = 3; p.add(new JLabel("Designation:"), g);
        g.gridx = 1; g.gridy = 3; p.add(desigCombo, g);

        g.gridx = 0; g.gridy = 4; p.add(new JLabel("Max Hrs/Week:"), g);
        g.gridx = 1; g.gridy = 4; p.add(hrsSpin, g);

        JButton save = new JButton("Save");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            String abbr = abbrField.getText().trim().toUpperCase();
            String email = emailField.getText().trim().toLowerCase();
            if (name.isEmpty() || abbr.isEmpty() || email.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Name, Abbreviation, and Email are required.");
                return;
            }
            Faculty f = existing != null ? existing : new Faculty();
            f.setName(name);
            f.setAbbreviation(abbr);
            f.setEmail(email);
            f.setDesignation((String) desigCombo.getSelectedItem());
            f.setMaxHoursPerWeek((Integer) hrsSpin.getValue());
            f.setDeptId(1); // ECS department
            f.setActive(true);

            if (existing == null) {
                facultyDAO.insert(f);
            } else {
                facultyDAO.update(f);
            }
            dlg.dispose();
            refresh();
        });

        g.gridx = 0; g.gridy = 5; g.gridwidth = 2;
        g.insets = new Insets(12, 4, 4, 4);
        p.add(save, g);

        dlg.setContentPane(p);
        dlg.pack();
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }
}

