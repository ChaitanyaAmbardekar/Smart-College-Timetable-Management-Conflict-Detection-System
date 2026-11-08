package in.frcce.ecs.ui;

import in.frcce.ecs.dao.RoomDAO;
import in.frcce.ecs.model.Room;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Master data CRUD for Rooms and Laboratories.
 */
public class RoomPanel extends JPanel implements MainFrame.Refreshable {

    private final RoomDAO roomDAO = new RoomDAO();
    private JTable            table;
    private DefaultTableModel tableModel;

    private static final String[] COLS = {
        "ID", "Name", "Type", "Capacity", "Location", "Active"
    };

    public RoomPanel() {
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(new Color(0xF1, 0xF3, 0xF4));

        JButton addBtn = new JButton("+ Add Room");
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
        List<Room> list = roomDAO.findAll();
        for (Room r : list) {
            tableModel.addRow(new Object[]{
                r.getRoomId(), r.getName(), r.getType(), r.getCapacity(),
                r.getLocation() != null ? r.getLocation() : "",
                r.isActive() ? "Yes" : "No"
            });
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        roomDAO.findById(id).ifPresent(this::showDialog);
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        int id = (int) tableModel.getValueAt(row, 0);
        int ch = JOptionPane.showConfirmDialog(this, "Delete this room?", "Confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ch == JOptionPane.YES_OPTION) {
            try {
                roomDAO.delete(id);
                refresh();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Cannot delete room: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showDialog(Room existing) {
        JDialog dlg = new JDialog(SwingUtilities.getWindowAncestor(this),
                existing == null ? "Add Room" : "Edit Room", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(new EmptyBorder(16, 20, 16, 20));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(4, 4, 4, 4);

        JTextField nameField = new JTextField(existing != null ? existing.getName() : "", 15);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "CLASSROOM", "LAB", "SEMINAR_HALL", "TUTORIAL_ROOM"
        });
        if (existing != null) typeCombo.setSelectedItem(existing.getType());
        JSpinner capSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getCapacity() : 60, 5, 500, 1));
        JTextField locField = new JTextField(existing != null && existing.getLocation() != null ? existing.getLocation() : "", 20);
        JCheckBox activeCheck = new JCheckBox("Is Active", existing == null || existing.isActive());

        g.gridx=0; g.gridy=0; p.add(new JLabel("Room / Lab Name:"), g);
        g.gridx=1; g.gridy=0; p.add(nameField, g);
        g.gridx=0; g.gridy=1; p.add(new JLabel("Type:"), g);
        g.gridx=1; g.gridy=1; p.add(typeCombo, g);
        g.gridx=0; g.gridy=2; p.add(new JLabel("Capacity:"), g);
        g.gridx=1; g.gridy=2; p.add(capSpin, g);
        g.gridx=0; g.gridy=3; p.add(new JLabel("Location:"), g);
        g.gridx=1; g.gridy=3; p.add(locField, g);
        g.gridx=0; g.gridy=4; p.add(new JLabel("Status:"), g);
        g.gridx=1; g.gridy=4; p.add(activeCheck, g);

        JButton save = new JButton("Save");
        save.setBackground(new Color(0x1A, 0x73, 0xE8));
        save.setForeground(Color.WHITE);
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dlg, "Room name is required.");
                return;
            }
            Room r = existing != null ? existing : new Room();
            r.setName(name);
            r.setType((String) typeCombo.getSelectedItem());
            r.setCapacity((Integer) capSpin.getValue());
            r.setLocation(locField.getText().trim());
            r.setActive(activeCheck.isSelected());

            if (existing == null) roomDAO.insert(r);
            else roomDAO.update(r);
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

