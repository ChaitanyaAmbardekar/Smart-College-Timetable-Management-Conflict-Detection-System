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
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        UiTheme.styleToolbar(toolbar);

        JButton addBtn = new JButton("+ Add Room");
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
        p.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(3, 4, 3, 4);

        JTextField nameField = new JTextField(existing != null ? existing.getName() : "", 15);
        nameField.setFont(UiTheme.FONT_BASE);
        JComboBox<String> typeCombo = new JComboBox<>(new String[]{
            "CLASSROOM", "LAB", "SEMINAR_HALL", "TUTORIAL_ROOM"
        });
        typeCombo.setFont(UiTheme.FONT_BASE);
        if (existing != null) typeCombo.setSelectedItem(existing.getType());
        JSpinner capSpin = new JSpinner(new SpinnerNumberModel(existing != null ? existing.getCapacity() : 60, 5, 500, 1));
        capSpin.setFont(UiTheme.FONT_BASE);
        JTextField locField = new JTextField(existing != null && existing.getLocation() != null ? existing.getLocation() : "", 20);
        locField.setFont(UiTheme.FONT_BASE);
        JCheckBox activeCheck = new JCheckBox("Is Active", existing == null || existing.isActive());
        activeCheck.setFont(UiTheme.FONT_BASE);

        JLabel l0 = new JLabel("Room / Lab Name:"); l0.setFont(UiTheme.bold(11f));
        JLabel l1 = new JLabel("Type:"); l1.setFont(UiTheme.bold(11f));
        JLabel l2 = new JLabel("Capacity:"); l2.setFont(UiTheme.bold(11f));
        JLabel l3 = new JLabel("Location:"); l3.setFont(UiTheme.bold(11f));
        JLabel l4 = new JLabel("Status:"); l4.setFont(UiTheme.bold(11f));

        g.gridx=0; g.gridy=0; p.add(l0, g);
        g.gridx=1; g.gridy=0; p.add(nameField, g);
        g.gridx=0; g.gridy=1; p.add(l1, g);
        g.gridx=1; g.gridy=1; p.add(typeCombo, g);
        g.gridx=0; g.gridy=2; p.add(l2, g);
        g.gridx=1; g.gridy=2; p.add(capSpin, g);
        g.gridx=0; g.gridy=3; p.add(l3, g);
        g.gridx=1; g.gridy=3; p.add(locField, g);
        g.gridx=0; g.gridy=4; p.add(l4, g);
        g.gridx=1; g.gridy=4; p.add(activeCheck, g);

        JButton save = new JButton("Save");
        UiTheme.styleActionButton(save, UiTheme.COLOR_HEADER, Color.WHITE);
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

